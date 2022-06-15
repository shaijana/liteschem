package fi.dy.masa.litematica.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.util.ToBooleanFunction;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataManager implements IDirectoryCache {
    private static final DataManager INSTANCE = new DataManager();

    private static final Pattern PATTERN_ITEM_NBT = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)(?<nbt>\\{.*\\})$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();
    private static final ArrayList<ToBooleanFunction<Text>> CHAT_LISTENERS = new ArrayList<>();

    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;
    private static boolean createPlacementOnLoad = true;
    private static boolean canSave;
    private static boolean isCarpetServer;
    private static long clientTickStart;

    //SH    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();
    //SH    private final SchematicProjectsManager schematicProjectsManager = new SchematicProjectsManager();
    private LayerRange renderRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
    private ToolMode operationMode = ToolMode.SCHEMATIC_PLACEMENT;
    private AreaSelectionSimple areaSimple = new AreaSelectionSimple(true);
    @Nullable
    private MaterialListBase materialList;

    private DataManager() {
    }

    private static DataManager getInstance() {
        return INSTANCE;
    }

    public static IDirectoryCache getDirectoryCache() {
        return INSTANCE;
    }

    public static void onClientTickStart() {
        clientTickStart = System.nanoTime();
    }

    public static long getClientTickStartTime() {
        return clientTickStart;
    }

    public static ItemStack getToolItem() {
        return toolItem;
    }

    public static void setIsCarpetServer(final boolean isCarpetServer) {
        DataManager.isCarpetServer = isCarpetServer;
    }

    public static boolean isCarpetServer() {
        return isCarpetServer;
    }

    public static void addChatListener(final ToBooleanFunction<Text> listener) {
        synchronized (CHAT_LISTENERS) {
            CHAT_LISTENERS.add(listener);
        }
    }

    public static void removeChatListener(final ToBooleanFunction<Text> listener) {
        synchronized (CHAT_LISTENERS) {
            CHAT_LISTENERS.remove(listener);
        }
    }

    public static void clearChatListeners() {
        synchronized (CHAT_LISTENERS) {
            CHAT_LISTENERS.clear();
        }
    }

    public static boolean onChatMessage(final Text text) {
        synchronized (CHAT_LISTENERS) {
            boolean cancel = false;

            for (final ToBooleanFunction<Text> listener : CHAT_LISTENERS) {
                cancel |= listener.applyAsBoolean(text);
            }

            return cancel;
        }
    }

    public static boolean getCreatePlacementOnLoad() {
        return createPlacementOnLoad;
    }

    public static void setCreatePlacementOnLoad(final boolean create) {
        createPlacementOnLoad = create;
    }

    public static ConfigGuiTab getConfigGuiTab() {
        return configGuiTab;
    }

    public static void setConfigGuiTab(final ConfigGuiTab tab) {
        configGuiTab = tab;
    }

/*SH    public static SelectionManager getSelectionManager()
    {
        return getInstance().selectionManager;
    }*/

    public static SchematicPlacementManager getSchematicPlacementManager() {
        return getInstance().schematicPlacementManager;
    }

/*SH    public static SchematicProjectsManager getSchematicProjectsManager()
    {
        return getInstance().schematicProjectsManager;
    }*/

    @Nullable
    public static MaterialListBase getMaterialList() {
        return getInstance().materialList;
    }

    public static void setMaterialList(@Nullable final MaterialListBase materialList) {
        final MaterialListBase old = getInstance().materialList;

        if (old != null) {
            final MaterialListHudRenderer renderer = old.getHudRenderer();

            if (renderer.getShouldRenderCustom()) {
                renderer.toggleShouldRender();
                InfoHud.getInstance().removeInfoHudRenderer(renderer, false);
            }
        }

        getInstance().materialList = materialList;
    }

    public static ToolMode getToolMode() {
        return getInstance().operationMode;
    }

    public static void setToolMode(final ToolMode mode) {
        getInstance().operationMode = mode;
    }

    public static LayerRange getRenderLayerRange() {
        return getInstance().renderRange;
    }

    public static AreaSelectionSimple getSimpleArea() {
        return getInstance().areaSimple;
    }

    @Override
    @Nullable
    public File getCurrentDirectoryForContext(final String context) {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(final String context, final File dir) {
        LAST_DIRECTORIES.put(context, dir);
    }

    public static void load() {
        getInstance().loadPerDimensionData();

        final File file = getCurrentStorageFile(true);
        final JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject()) {
            LAST_DIRECTORIES.clear();

            final JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "last_directories")) {
                final JsonObject obj = root.get("last_directories").getAsJsonObject();

                for (final Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    final String name = entry.getKey();
                    final JsonElement el = entry.getValue();

                    if (el.isJsonPrimitive()) {
                        final File dir = new File(el.getAsString());

                        if (dir.exists() && dir.isDirectory()) {
                            LAST_DIRECTORIES.put(name, dir);
                        }
                    }
                }
            }

            if (JsonUtils.hasString(root, "config_gui_tab")) {
                try {
                    configGuiTab = ConfigGuiTab.valueOf(root.get("config_gui_tab").getAsString());
                } catch (final Exception e) {
                }

                if (configGuiTab == null) {
                    configGuiTab = ConfigGuiTab.GENERIC;
                }
            }

            createPlacementOnLoad = JsonUtils.getBooleanOrDefault(root, "create_placement_on_load", true);
        }

        canSave = true;
    }

/*SH    public static void save()
    {
        save(false);
    }

    public static void save(boolean forceSave)
    {
        if (canSave == false && forceSave == false)
        {
            return;
        }

        getInstance().savePerDimensionData();

        JsonObject root = new JsonObject();
        JsonObject objDirs = new JsonObject();

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().getAbsolutePath()));
        }

        root.add("last_directories", objDirs);

        root.add("create_placement_on_load", new JsonPrimitive(createPlacementOnLoad));
        root.add("config_gui_tab", new JsonPrimitive(configGuiTab.name()));

        File file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFile(root, file);

        canSave = false;
    }*/

    public static void clear() {
        TaskScheduler.getInstanceClient().clearTasks();
        SchematicVerifier.clearActiveVerifiers();

        getSchematicPlacementManager().clear();
//SH        getSchematicProjectsManager().clear();
//SH        getSelectionManager().clear();
        setMaterialList(null);
        clearChatListeners();

        InfoHud.getInstance().reset(); // remove the line providers and clear the data
        setIsCarpetServer(false);
    }

/*SH    private void savePerDimensionData()
    {
        this.schematicProjectsManager.saveCurrentProject();
        JsonObject root = this.toJson();

        File file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFile(root, file);
    }*/

    private void loadPerDimensionData() {
//SH        this.selectionManager.clear();
        this.schematicPlacementManager.clear();
//SH        this.schematicProjectsManager.clear();
        this.materialList = null;

        final File file = getCurrentStorageFile(false);
        final JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject()) {
            final JsonObject root = element.getAsJsonObject();
            this.fromJson(root);
        }
    }

    private void fromJson(final JsonObject obj) {
/*SH        if (JsonUtils.hasObject(obj, "selections"))
        {
            this.selectionManager.loadFromJson(obj.get("selections").getAsJsonObject());
        }*/

        if (JsonUtils.hasObject(obj, "placements")) {
            this.schematicPlacementManager.loadFromJson(obj.get("placements").getAsJsonObject());
        }

/*SH        if (JsonUtils.hasObject(obj, "schematic_projects_manager"))
        {
            this.schematicProjectsManager.loadFromJson(obj.get("schematic_projects_manager").getAsJsonObject());
        }*/

        if (JsonUtils.hasObject(obj, "render_range")) {
            this.renderRange = LayerRange.createFromJson(JsonUtils.getNestedObject(obj, "render_range", false), SchematicWorldRefresher.INSTANCE);
        }

        if (JsonUtils.hasString(obj, "operation_mode")) {
            try {
                this.operationMode = ToolMode.valueOf(obj.get("operation_mode").getAsString());
            } catch (final Exception e) {
            }

/*SH            if (this.operationMode == null)
            {
                this.operationMode = ToolMode.AREA_SELECTION;
            }*/
        }

/*SH        if (JsonUtils.hasObject(obj, "area_simple"))
        {
            this.areaSimple = AreaSelectionSimple.fromJson(obj.get("area_simple").getAsJsonObject());
        }*/

        if (JsonUtils.hasObject(obj, "tool_mode_data")) {
            this.toolModeDataFromJson(obj.get("tool_mode_data").getAsJsonObject());
        }
    }

    private JsonObject toJson() {
        final JsonObject obj = new JsonObject();

//SH        obj.add("selections", this.selectionManager.toJson());
        obj.add("placements", this.schematicPlacementManager.toJson());
//SH        obj.add("schematic_projects_manager", this.schematicProjectsManager.toJson());
        obj.add("operation_mode", new JsonPrimitive(this.operationMode.name()));
        obj.add("render_range", this.renderRange.toJson());
        obj.add("area_simple", this.areaSimple.toJson());
        obj.add("tool_mode_data", this.toolModeDataToJson());

        return obj;
    }

    private JsonObject toolModeDataToJson() {
        final JsonObject obj = new JsonObject();
        obj.add("delete", ToolModeData.DELETE.toJson());
        return obj;
    }

    private void toolModeDataFromJson(final JsonObject obj) {
        if (JsonUtils.hasObject(obj, "delete")) {
            ToolModeData.DELETE.fromJson(obj.get("delete").getAsJsonObject());
        }
    }

    public static File getDefaultBaseSchematicDirectory() {
        return FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics"));
    }

    public static File getCurrentConfigDirectory() {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    public static File getSchematicsBaseDirectory() {
        final File dir;

        if (Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED.getBooleanValue()) {
            dir = new File(Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY.getStringValue());
        } else {
            dir = getDefaultBaseSchematicDirectory();
        }

        if (dir.exists() == false && dir.mkdirs() == false) {
            Litematica.logger.warn("Failed to create the schematic directory '{}'", dir.getAbsolutePath());
        }

        return dir;
    }

/*SH    public static File getAreaSelectionsBaseDirectory()
    {
        File dir;
        String name = StringUtils.getWorldOrServerName();

        if (Configs.Generic.AREAS_PER_WORLD.getBooleanValue() && name != null)
        {
            // The 'area_selections' sub-directory is to prevent showing the world name or server IP in the browser,
            // as the root directory name is shown in the navigation widget
            dir = FileUtils.getCanonicalFileIfPossible(new File(new File(new File(getCurrentConfigDirectory(), "area_selections_per_world"), name), "area_selections"));
        }
        else
        {
            dir = FileUtils.getCanonicalFileIfPossible(new File(getCurrentConfigDirectory(), "area_selections"));
        }

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the area selections base directory '{}'", dir.getAbsolutePath());
        }

        return dir;
    }*/

    private static File getCurrentStorageFile(final boolean globalData) {
        final File dir = getCurrentConfigDirectory();

        if (dir.exists() == false && dir.mkdirs() == false) {
            Litematica.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, StringUtils.getStorageFileName(globalData, Reference.MOD_ID + "_", ".json", "default"));
    }

    public static void setToolItem(final String itemNameIn) {
        if (itemNameIn.isEmpty() || itemNameIn.equals("empty")) {
            toolItem = ItemStack.EMPTY;
            return;
        }

        try {
            final Matcher matcherNbt = PATTERN_ITEM_NBT.matcher(itemNameIn);
            final Matcher matcherBase = PATTERN_ITEM_BASE.matcher(itemNameIn);

            String itemName = null;
            NbtCompound nbt = null;

            if (matcherNbt.matches()) {
                itemName = matcherNbt.group("name");
                nbt = (new StringNbtReader(new StringReader(matcherNbt.group("nbt")))).parseCompound();
            } else if (matcherBase.matches()) {
                itemName = matcherBase.group("name");
            }

            if (itemName != null) {
                final Item item = Registry.ITEM.get(new Identifier(itemName));

                if (item != null && item != Items.AIR) {
                    toolItem = new ItemStack(item);
                    toolItem.setNbt(nbt);
                    return;
                }
            }
        } catch (final Exception ignore) {
        }

        // Fall back to a stick
        toolItem = new ItemStack(Items.STICK);
        Configs.Generic.TOOL_ITEM.setValueFromString(Registry.ITEM.getId(Items.STICK).toString());
    }
}
