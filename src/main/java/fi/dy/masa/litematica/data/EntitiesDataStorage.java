package fi.dy.masa.litematica.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;

import com.mojang.datafixers.util.Either;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.mixin.IMixinDataQueryHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

public class EntitiesDataStorage implements IClientTickHandler
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private int uptimeTicks = 0;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;
    private final long chunkTimeoutMs = 5000;
    // Wait 5 seconds for loaded Client Chunks to receive Entity Data

    // Data Cache
    private final ConcurrentHashMap<BlockPos, Pair<Long, Pair<BlockEntity, NbtCompound>>> blockEntityCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer,  Pair<Long, Pair<Entity,      NbtCompound>>> entityCache      = new ConcurrentHashMap<>();
    private final long cacheTimeout = 4;
    private final long longCacheTimeout = 30;
    private boolean shouldUseLongTimeout = false;
    // Needs a long cache timeout for saving schematics
    private long serverTickTime = 0;
    // Requests to be executed
    private final Set<BlockPos> pendingBlockEntitiesQueue = new LinkedHashSet<>();
    private final Set<Integer> pendingEntitiesQueue = new LinkedHashSet<>();
    private final Set<ChunkPos> pendingChunks = new LinkedHashSet<>();
    private final Set<ChunkPos> completedChunks = new LinkedHashSet<>();
    private final Map<ChunkPos, Long> pendingChunkTimeout = new HashMap<>();
    // To save vanilla query packet transaction
    private final Map<Integer, Either<BlockPos, Integer>> transactionToBlockPosOrEntityId = new HashMap<>();
    private ClientWorld clientWorld;

    // Backup Chunk Saving task
    private boolean sentBackupPackets = false;
    private boolean receivedBackupPackets = false;
    private final HashMap<ChunkPos, Set<BlockPos>> pendingBackupChunk_BlockEntities = new HashMap<>();
    private final HashMap<ChunkPos, Set<Integer>>  pendingBackupChunk_Entities      = new HashMap<>();

    @Nullable
    public World getWorld()
    {
        return fi.dy.masa.malilib.util.WorldUtils.getBestWorld(mc);
    }

    private ClientWorld getClientWorld()
    {
        if (this.clientWorld == null)
        {
            clientWorld = mc.world;
        }

        return clientWorld;
    }

    private EntitiesDataStorage()
    {
    }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        this.uptimeTicks++;
        if (System.currentTimeMillis() - this.serverTickTime > 50)
        {
            // In this block, we do something every server tick
            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
            {
                this.serverTickTime = System.currentTimeMillis();

                if (DataManager.getInstance().hasIntegratedServer() == false && this.hasServuxServer())
                {
                    this.servuxServer = false;
                    HANDLER.unregisterPlayReceiver();
                }
                return;
            }
            else if (DataManager.getInstance().hasIntegratedServer() == false &&
                    this.hasServuxServer() == false &&
                    this.hasInValidServux == false &&
                    this.getWorld() != null)
            {
                // Make sure we're Play Registered, and request Metadata
                HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
                this.requestMetadata();
            }

            // Expire cached NBT
            this.tickCache();

            // 5 queries / server tick
            for (int i = 0; i < Configs.Generic.SERVER_NBT_REQUEST_RATE.getIntegerValue(); i++)
            {
                if (!this.pendingBlockEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingBlockEntitiesQueue.iterator();
                    BlockPos pos = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxBlockEntityData(pos);
                    }
                    else
                    {
                        requestQueryBlockEntity(pos);
                    }
                }
                if (!this.pendingEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingEntitiesQueue.iterator();
                    int entityId = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxEntityData(entityId);
                    }
                    else
                    {
                        requestQueryEntityData(entityId);
                    }
                }
            }
            this.serverTickTime = System.currentTimeMillis();
        }
    }

    public Identifier getNetworkChannel()
    {
        return ServuxLitematicaHandler.CHANNEL_ID;
    }

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxLitematicaPacket.Payload> getNetworkHandler()
    {
        return HANDLER;
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());
            this.servuxServer = false;
            this.hasInValidServux = false;
            this.sentBackupPackets = false;
            this.receivedBackupPackets = false;
        }
        else
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - dimension change or log-in");
            this.serverTickTime = System.currentTimeMillis() - (this.getCacheTimeout() + 5000L);
            this.tickCache();
            this.serverTickTime = System.currentTimeMillis();
            this.clientWorld = mc.world;
        }
        // Clear data
        this.blockEntityCache.clear();
        this.entityCache.clear();
        this.pendingBlockEntitiesQueue.clear();
        this.pendingEntitiesQueue.clear();
        // Litematic Save values
        this.completedChunks.clear();
        this.pendingChunks.clear();
        this.pendingChunkTimeout.clear();
        this.pendingBackupChunk_BlockEntities.clear();
        this.pendingBackupChunk_Entities.clear();
    }

    private long getCacheTimeout()
    {
        return (long) (MathHelper.clamp(Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue(), 0.25f, 30.0f) * 1000L);
    }

    private long getCacheTimeoutLong()
    {
        return (long) (MathHelper.clamp((Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue() * this.longCacheTimeout), 120.0f, 300.0f) * 1000L);
    }

    private void tickCache()
    {
        long nowTime = System.currentTimeMillis();
        long blockTimeout = this.getCacheTimeout();
        long entityTimeout = this.getCacheTimeout() * 2;
        int count;
        boolean beEmpty = false;
        boolean entEmpty = false;

        // Use LongTimeouts when saving a Litematic Selection,
        // which is pretty much the standard value x 30 (min 120, max 300 seconds)
        if (this.shouldUseLongTimeout)
        {
            blockTimeout = this.getCacheTimeoutLong();
            entityTimeout = this.getCacheTimeoutLong();

            // Add extra time if using QueryNbt only
            if (this.hasServuxServer() == false && this.getIfReceivedBackupPackets())
            {
                blockTimeout += 3000L;
                entityTimeout += 3000L;
            }
        }

        synchronized (this.blockEntityCache)
        {
            count = 0;

            for (BlockPos pos : this.blockEntityCache.keySet())
            {
                Pair<Long, Pair<BlockEntity, NbtCompound>> pair = this.blockEntityCache.get(pos);

                if (nowTime - pair.getLeft() > blockTimeout || pair.getLeft() - nowTime > 0)
                {
                    Litematica.debugLog("entityCache: be at pos [{}] has timed out by [{}] ms", pos.toShortString(), blockTimeout);
                    this.blockEntityCache.remove(pos);
                }
                else
                {
                    count++;
                }
            }

            if (count == 0)
            {
                beEmpty = true;
            }
        }

        synchronized (this.entityCache)
        {
            count = 0;

            for (Integer entityId : this.entityCache.keySet())
            {
                Pair<Long, Pair<Entity, NbtCompound>> pair = this.entityCache.get(entityId);

                if (nowTime - pair.getLeft() > entityTimeout || pair.getLeft() - nowTime > 0)
                {
                    Litematica.debugLog("entityCache: entity Id [{}] has timed out by [{}] ms", entityId, entityTimeout);
                    this.entityCache.remove(entityId);
                }
                else
                {
                    count++;
                }
            }

            if (count == 0)
            {
                entEmpty = true;
            }
        }

        // End Long timeout phase
        if (beEmpty && entEmpty && this.shouldUseLongTimeout)
        {
            this.shouldUseLongTimeout = false;
        }
    }

    public @Nullable NbtCompound getFromBlockEntityCacheNbt(BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight().getRight();
        }

        return null;
    }

    public @Nullable BlockEntity getFromBlockEntityCache(BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight().getLeft();
        }

        return null;
    }

    public @Nullable NbtCompound getFromEntityCacheNbt(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight().getRight();
        }

        return null;
    }

    public @Nullable Entity getFromEntityCache(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight().getLeft();
        }

        return null;
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        this.hasInValidServux = false;
    }

    public boolean hasServuxServer()
    {
        return this.servuxServer;
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
            Litematica.debugLog("LitematicDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        return this.servuxVersion;
    }

    public int getPendingBlockEntitiesCount()
    {
        return this.pendingBlockEntitiesQueue.size();
    }

    public int getPendingEntitiesCount()
    {
        return this.pendingEntitiesQueue.size();
    }

    public int getBlockEntityCacheCount()
    {
        return this.blockEntityCache.size();
    }

    public int getEntityCacheCount()
    {
        return this.entityCache.size();
    }

    public boolean getIfReceivedBackupPackets()
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            return this.sentBackupPackets & this.receivedBackupPackets;
        }

        return false;
    }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        // NO-OP
    }

    public void requestMetadata()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false &&
            Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxLitematicaPacket.MetadataRequest(nbt));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            Litematica.debugLog("EntitiesDataStorage#receiveServuxMetadata(): received METADATA from Servux");

            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                if (data.getInt("version") != ServuxLitematicaPacket.PROTOCOL_VERSION)
                {
                    Litematica.logger.warn("LitematicDataChannel: Mis-matched protocol version!");
                }

                this.setServuxVersion(data.getString("servux"));
                this.setIsServuxServer();

                return true;
            }
        }

        return false;
    }

    public void onPacketFailure()
    {
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public @Nullable Pair<BlockEntity, NbtCompound> requestBlockEntity(World world, BlockPos pos)
    {
        // Don't cache/request a BE for the Schematic World
        if (world instanceof WorldSchematic)
        {
            BlockEntity be = world.getWorldChunk(pos).getBlockEntity(pos);

            if (be != null)
            {
                NbtCompound nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());

                return Pair.of(be, nbt);
            }
        }
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight();
        }
        else if (world.getBlockState(pos).getBlock() instanceof BlockEntityProvider)
        {
            if (DataManager.getInstance().hasIntegratedServer() == false &&
                Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                this.pendingBlockEntitiesQueue.add(pos);
            }

            BlockEntity be = world.getWorldChunk(pos).getBlockEntity(pos);

            if (be != null)
            {
                NbtCompound nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                Pair<BlockEntity, NbtCompound> pair = Pair.of(be, nbt);

                synchronized (this.blockEntityCache)
                {
                    this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), pair));
                }

                return pair;
            }
        }

        return null;
    }

    public @Nullable Pair<Entity, NbtCompound> requestEntity(World world, int entityId)
    {
        if (world instanceof WorldSchematic)
        {
            return null;
        }
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight();
        }
        if (DataManager.getInstance().hasIntegratedServer() == false &&
            Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.pendingEntitiesQueue.add(entityId);
        }

        if (world != null)
        {
            Entity entity = world.getEntityById(entityId);
            NbtCompound nbt = new NbtCompound();

            if (entity != null && entity.saveSelfNbt(nbt))
            {
                Pair<Entity, NbtCompound> pair = Pair.of(entity, nbt);

                synchronized (this.entityCache)
                {
                    this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), pair));
                }

                return pair;
            }
        }

        return null;
    }

    @Nullable
    public Inventory getBlockInventory(World world, BlockPos pos, boolean useNbt)
    {
        if (world instanceof WorldSchematic)
        {
            return InventoryUtils.getInventory(world, pos);
        }
        if (this.blockEntityCache.containsKey(pos))
        {
            Inventory inv = null;

            if (useNbt)
            {
                inv = InventoryUtils.getNbtInventory(this.blockEntityCache.get(pos).getRight().getRight(), -1, world.getRegistryManager());
            }
            else
            {
                BlockEntity be = this.blockEntityCache.get(pos).getRight().getLeft();
                BlockState state = world.getBlockState(pos);

                if (state.getBlock() instanceof AirBlock || state.equals(Blocks.AIR.getDefaultState()))
                {
                    return null;
                }

                if (be instanceof Inventory inv1)
                {
                    if (be instanceof ChestBlockEntity)
                    {
                        ChestType type = state.get(ChestBlock.CHEST_TYPE);

                        if (type != ChestType.SINGLE)
                        {
                            BlockPos posAdj = pos.offset(ChestBlock.getFacing(state));
                            if (!world.isChunkLoaded(posAdj)) return null;
                            BlockState stateAdj = world.getBlockState(posAdj);

                            var dataAdj = this.getFromBlockEntityCache(posAdj);

                            if (dataAdj == null)
                            {
                                this.requestBlockEntity(world, posAdj);
                            }

                            if (stateAdj.getBlock() == state.getBlock() &&
                                dataAdj instanceof ChestBlockEntity inv2 &&
                                stateAdj.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE &&
                                stateAdj.get(ChestBlock.FACING) == state.get(ChestBlock.FACING))
                            {
                                Inventory invRight = type == ChestType.RIGHT ? inv1 : inv2;
                                Inventory invLeft = type == ChestType.RIGHT ? inv2 : inv1;

                                inv = new DoubleInventory(invRight, invLeft);
                            }
                        }
                        else
                        {
                            inv = inv1;
                        }
                    }
                    else
                    {
                        inv = inv1;
                    }
                }
            }

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.requestBlockEntity(world, pos);
        }

        return null;
    }

    /*
    @Nullable
    public Inventory getEntityInventory(int entityId, boolean useNbt)
    {
        if (world instanceof WorldSchematic)
        {
            return null;
        }

        if (this.entityCache.containsKey(entityId) && this.getWorld() != null)
        {
            Inventory inv = null;

            if (useNbt)
            {
                inv = InventoryUtils.getNbtInventory(this.entityCache.get(entityId).getRight().getRight(), -1, this.getWorld().getRegistryManager());
            }
            else
            {
                Entity entity = this.entityCache.get(entityId).getRight().getLeft();

                if (entity instanceof Inventory)
                {
                    inv = (Inventory) entity;
                }
                else if (entity instanceof PlayerEntity player)
                {
                    inv = new SimpleInventory(player.getInventory().main.toArray(new ItemStack[36]));
                }
                else if (entity instanceof VillagerEntity)
                {
                    inv = ((VillagerEntity) entity).getInventory();
                }
                else if (entity instanceof AbstractHorseEntity)
                {
                    inv = ((IMixinAbstractHorseEntity) entity).malilib_getHorseInventory();
                }
                else if (entity instanceof PiglinEntity)
                {
                    inv = ((IMixinPiglinEntity) entity).malilib_getInventory();
                }
            }

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.requestEntity(entityId);
        }

        return null;
    }
     */

    private void requestQueryBlockEntity(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDataQueryHandler().queryBlockNbt(pos, nbtCompound ->
            {
                handleBlockEntityData(pos, nbtCompound, null);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).litematica_currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDataQueryHandler().queryEntityNbt(entityId, nbtCompound ->
            {
                handleEntityData(entityId, nbtCompound);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).litematica_currentTransactionId(), Either.right(entityId));
        }
    }

    private void requestServuxBlockEntityData(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxLitematicaPacket.BlockEntityRequest(pos));
        }
    }

    private void requestServuxEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxLitematicaPacket.EntityRequest(entityId));
        }
    }

    // The minY, maxY should be calculated based on the Selection Box...  But for now, we can just grab the entire chunk.
    public void requestServuxBulkEntityData(ChunkPos chunkPos, int minY, int maxY)
    {
        if (this.hasServuxServer() == false)
        {
            return;
        }

        NbtCompound req = new NbtCompound();

        this.completedChunks.remove(chunkPos);
        this.pendingChunks.add(chunkPos);
        this.pendingChunkTimeout.put(chunkPos, Util.getMeasuringTimeMs());

        minY = MathHelper.clamp(minY, -60, 319);
        maxY = MathHelper.clamp(maxY, -60, 319);

        req.putString("Task", "BulkEntityRequest");
        req.putInt("minY", minY);
        req.putInt("maxY", maxY);

        Litematica.debugLog("EntitiesDataStorage#requestServuxBulkEntityData(): for chunkPos [{}] to Servux (minY [{}], maxY [{}])", chunkPos.toString(), minY, maxY);
        HANDLER.encodeClientData(ServuxLitematicaPacket.BulkNbtRequest(chunkPos, req));
    }

    public void requestBackupBulkEntityData(ChunkPos chunkPos, int minY, int maxY)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return;
        }

        this.completedChunks.remove(chunkPos);
        minY = MathHelper.clamp(minY, -60, 319);
        maxY = MathHelper.clamp(maxY, -60, 319);

        ClientWorld world = this.getClientWorld();
        Chunk chunk = world != null ? world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) : null;

        if (chunk == null)
        {
            return;
        }

        BlockPos pos1 = new BlockPos(chunkPos.getStartX(), minY, chunkPos.getStartZ());
        BlockPos pos2 = new BlockPos(chunkPos.getEndX(),   maxY, chunkPos.getEndZ());
        Box bb = PositionUtils.createEnclosingAABB(pos1, pos2);
        Set<BlockPos> teSet = chunk.getBlockEntityPositions();
        List<Entity> entList = world.getOtherEntities(null, bb, EntityUtils.NOT_PLAYER);

        Litematica.debugLog("EntitiesDataStorage#requestBackupBulkEntityData(): for chunkPos {} (minY [{}], maxY [{}]) // Request --> TE: [{}], E: [{}]", chunkPos.toString(), minY, maxY, teSet.size(), entList.size());
        //System.out.printf("0: ChunkPos [%s], Box [%s] // teSet [%d], entList [%d]\n", chunkPos.toString(), bb.toString(), teSet.size(), entList.size());

        for (BlockPos tePos : teSet)
        {
            if ((tePos.getX() < chunkPos.getStartX() || tePos.getX() > chunkPos.getEndX()) ||
                (tePos.getZ() < chunkPos.getStartZ() || tePos.getZ() > chunkPos.getEndZ()) ||
                (tePos.getY() < minY || tePos.getY() > maxY))
            {
                continue;
            }

            this.requestBlockEntity(world, tePos);
        }

        if (teSet.size() > 0)
        {
            this.pendingBackupChunk_BlockEntities.put(chunkPos, teSet);
        }

        Set<Integer> entSet = new LinkedHashSet<>();

        for (Entity entity : entList)
        {
            this.requestEntity(world, entity.getId());
            entSet.add(entity.getId());
        }

        if (entSet.size() > 0)
        {
            this.pendingBackupChunk_Entities.put(chunkPos, entSet);
        }

        if (teSet.size() > 0 || entSet.size() > 0)
        {
            this.pendingChunks.add(chunkPos);
            this.pendingChunkTimeout.put(chunkPos, Util.getMeasuringTimeMs());
        }
        else
        {
            this.completedChunks.add(chunkPos);
        }
    }

    private boolean markBackupBlockEntityComplete(ChunkPos chunkPos, BlockPos pos)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return true;
        }

        //Litematica.debugLog("EntitiesDataStorage#markBackupBlockEntityComplete() - Marking ChunkPos {} - Block Entity at [{}] as complete.", chunkPos.toString(), pos.toShortString());

        if (this.pendingChunks.contains(chunkPos))
        {
            if (this.pendingBackupChunk_BlockEntities.containsKey(chunkPos))
            {
                Set<BlockPos> teSet = this.pendingBackupChunk_BlockEntities.get(chunkPos);

                if (teSet.contains(pos))
                {
                    teSet.remove(pos);

                    if (teSet.isEmpty())
                    {
                        Litematica.debugLog("EntitiesDataStorage#markBackupBlockEntityComplete(): ChunkPos {} - Block Entity List Complete!", chunkPos.toString());
                        this.pendingBackupChunk_BlockEntities.remove(chunkPos);
                        this.pendingChunks.remove(chunkPos);
                        this.pendingChunkTimeout.remove(chunkPos);
                        this.completedChunks.add(chunkPos);
                        return true;
                    }
                    else
                    {
                        this.pendingBackupChunk_BlockEntities.replace(chunkPos, teSet);
                    }
                }
            }
        }

        return false;
    }

    private boolean markBackupEntityComplete(ChunkPos chunkPos, int entityId)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return true;
        }

        //Litematica.debugLog("EntitiesDataStorage#markBackupEntityComplete() - Marking ChunkPos {} - EntityId [{}] as complete.", chunkPos.toString(), entityId);

        if (this.pendingChunks.contains(chunkPos))
        {
            if (this.pendingBackupChunk_Entities.containsKey(chunkPos))
            {
                Set<Integer> entSet = this.pendingBackupChunk_Entities.get(chunkPos);

                if (entSet.contains(entityId))
                {
                    entSet.remove(entityId);

                    if (entSet.isEmpty())
                    {
                        Litematica.debugLog("EntitiesDataStorage#markBackupEntityComplete(): ChunkPos {} - EntitiyList Complete!", chunkPos.toString());
                        this.pendingBackupChunk_Entities.remove(chunkPos);
                        this.pendingChunks.remove(chunkPos);
                        this.pendingChunkTimeout.remove(chunkPos);
                        this.completedChunks.add(chunkPos);
                        return true;
                    }
                    else
                    {
                        this.pendingBackupChunk_Entities.replace(chunkPos, entSet);
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, NbtCompound nbt, @Nullable Identifier type)
    {
        this.pendingBlockEntitiesQueue.remove(pos);
        if (nbt == null || this.getClientWorld() == null) return null;

        BlockEntity blockEntity = this.getClientWorld().getBlockEntity(pos);

        if (blockEntity != null && (type == null || type.equals(BlockEntityType.getId(blockEntity.getType()))))
        {
            if (nbt.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
            {
                Identifier id = BlockEntityType.getId(blockEntity.getType());

                if (id != null)
                {
                    nbt.putString(NbtKeys.ID, id.toString());
                }
            }

            synchronized (this.blockEntityCache)
            {
                if (this.blockEntityCache.containsKey(pos))
                {
                    this.blockEntityCache.replace(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity, nbt)));
                }
                else
                {
                    this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity, nbt)));
                }
            }

            blockEntity.read(nbt, this.getClientWorld().getRegistryManager());
            ChunkPos chunkPos = new ChunkPos(pos);

            if (this.hasPendingChunk(chunkPos) && this.hasServuxServer() == false)
            {
                this.markBackupBlockEntityComplete(chunkPos, pos);
            }

            return blockEntity;
        }

        Optional<RegistryEntry.Reference<BlockEntityType<?>>> opt = Registries.BLOCK_ENTITY_TYPE.getEntry(type);

        if (opt.isPresent())
        {
            BlockEntityType<?> beType = opt.get().value();

            if (beType.supports(this.getClientWorld().getBlockState(pos)))
            {
                BlockEntity blockEntity2 = beType.instantiate(pos, this.getClientWorld().getBlockState(pos));

                if (blockEntity2 != null)
                {
                    if (nbt.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
                    {
                        Identifier id = BlockEntityType.getId(beType);

                        if (id != null)
                        {
                            nbt.putString(NbtKeys.ID, id.toString());
                        }
                    }
                    synchronized (this.blockEntityCache)
                    {
                        if (this.blockEntityCache.containsKey(pos))
                        {
                            this.blockEntityCache.replace(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity2, nbt)));
                        }
                        else
                        {
                            this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity2, nbt)));
                        }
                    }

                    if (Configs.Generic.ENTITY_DATA_LOAD_NBT.getBooleanValue())
                    {
                        blockEntity2.read(nbt, this.getClientWorld().getRegistryManager());
                        this.getClientWorld().addBlockEntity(blockEntity2);
                    }

                    ChunkPos chunkPos = new ChunkPos(pos);

                    if (this.hasPendingChunk(chunkPos) && this.hasServuxServer() == false)
                    {
                        this.markBackupBlockEntityComplete(chunkPos, pos);
                    }

                    return blockEntity2;
                }
            }
        }

        return null;
    }

    @Nullable
    public Entity handleEntityData(int entityId, NbtCompound nbt)
    {
        this.pendingEntitiesQueue.remove(entityId);
        if (nbt == null || this.getClientWorld() == null) return null;
        Entity entity = this.getClientWorld().getEntityById(entityId);

        if (entity != null)
        {
            if (nbt.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
            {
                Identifier id = EntityType.getId(entity.getType());

                if (id != null)
                {
                    nbt.putString(NbtKeys.ID, id.toString());
                }
            }
            synchronized (this.entityCache)
            {
                if (this.entityCache.containsKey(entityId))
                {
                    this.entityCache.replace(entityId, Pair.of(System.currentTimeMillis(), Pair.of(entity, nbt)));
                }
                else
                {
                    this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), Pair.of(entity, nbt)));
                }
            }

            if (Configs.Generic.ENTITY_DATA_LOAD_NBT.getBooleanValue())
            {
                EntityUtils.loadNbtIntoEntity(entity, nbt);
            }

            if (this.hasPendingChunk(entity.getChunkPos()) && this.hasServuxServer() == false)
            {
                this.markBackupEntityComplete(entity.getChunkPos(), entityId);
            }
        }

        return entity;
    }

    public void handleBulkEntityData(int transactionId, @Nullable NbtCompound nbt)
    {
        if (nbt == null)
        {
            return;
        }

        // TODO --> Split out the task this way (I should have done this under sakura.12, etc),
        //  So we need to check if the "Task" is not included for now... (Wait for the updates to bake in)
        if ((nbt.contains("Task") && nbt.getString("Task").equals("BulkEntityReply")) ||
            nbt.contains("Task") == false)
        {
            NbtList tileList = nbt.contains("TileEntities") ? nbt.getList("TileEntities", Constants.NBT.TAG_COMPOUND) : new NbtList();
            NbtList entityList = nbt.contains("Entities") ? nbt.getList("Entities", Constants.NBT.TAG_COMPOUND) : new NbtList();
            ChunkPos chunkPos = new ChunkPos(nbt.getInt("chunkX"), nbt.getInt("chunkZ"));

            this.shouldUseLongTimeout = true;

            for (int i = 0; i < tileList.size(); ++i)
            {
                NbtCompound te = tileList.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(te);
                Identifier type = Identifier.of(te.getString("id"));

                this.handleBlockEntityData(pos, te, type);
            }

            for (int i = 0; i < entityList.size(); ++i)
            {
                NbtCompound ent = entityList.getCompound(i);
                Vec3d pos = NbtUtils.readEntityPositionFromTag(ent).toVanilla();
                int entityId = ent.getInt("entityId");

                this.handleEntityData(entityId, ent);
            }

            this.pendingChunks.remove(chunkPos);
            this.pendingChunkTimeout.remove(chunkPos);
            this.completedChunks.add(chunkPos);

            Litematica.debugLog("EntitiesDataStorage#handleBulkEntityData(): [ChunkPos {}] received TE: [{}], and E: [{}] entiries from Servux", chunkPos.toString(), tileList.size(), entityList.size());
        }
    }

    public void handleVanillaQueryNbt(int transactionId, NbtCompound nbt)
    {
        Either<BlockPos, Integer> either = this.transactionToBlockPosOrEntityId.remove(transactionId);

        if (either != null)
        {
            this.receivedBackupPackets = true;
            either.ifLeft(pos ->     handleBlockEntityData(pos, nbt, null))
                  .ifRight(entityId -> handleEntityData(entityId, nbt));
        }
    }

    public boolean hasPendingChunk(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
        {
            return this.pendingChunks.contains(pos);
        }

        return false;
    }

    private void checkForPendingChunkTimeout(ChunkPos pos)
    {
        if ((this.hasServuxServer() && this.hasPendingChunk(pos)) ||
            (this.getIfReceivedBackupPackets() && this.hasPendingChunk(pos)))
        {
            long now = Util.getMeasuringTimeMs();

            // Take no action when ChunkPos is not loaded by the ClientWorld.
            if (WorldUtils.isClientChunkLoaded(mc.world, pos.x, pos.z) == false)
            {
                this.pendingChunkTimeout.replace(pos, now);
                return;
            }

            long duration = now - this.pendingChunkTimeout.get(pos);

            if (duration > (this.getChunkTimeoutMs()))
            {
                Litematica.debugLog("EntitiesDataStorage#checkForPendingChunkTimeout(): [ChunkPos {}] has timed out waiting for data, marking complete without Receiving Entity Data.", pos.toString());
                this.pendingChunkTimeout.remove(pos);
                this.pendingChunks.remove(pos);
                this.completedChunks.add(pos);
            }
        }
    }

    private long getChunkTimeoutMs()
    {
        if (this.hasServuxServer())
        {
            return this.chunkTimeoutMs;
        }
        else if (this.getIfReceivedBackupPackets())
        {
            return this.chunkTimeoutMs + 3000L;
        }

        return 1000L;
    }

    public boolean hasCompletedChunk(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
        {
            this.checkForPendingChunkTimeout(pos);
            return this.completedChunks.contains(pos);
        }

        return true;
    }

    public void markCompletedChunkDirty(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
        {
            this.completedChunks.remove(pos);
        }
    }

    // TODO --> Only in case we need to save config settings in the future
    public JsonObject toJson()
    {
        return new JsonObject();
    }

    public void fromJson(JsonObject obj)
    {
        // NO-OP
    }
}
