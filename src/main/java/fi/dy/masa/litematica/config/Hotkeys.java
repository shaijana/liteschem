package fi.dy.masa.litematica.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

public class Hotkeys
{
//SH    public static final ConfigHotkey ADD_SELECTION_BOX                  = new ConfigHotkey("addSelectionBox",                   "M,A",  "Add a new selection box (position 1) here");
//SH    public static final ConfigHotkey CLONE_SELECTION                    = new ConfigHotkey("cloneSelection",                    "",     "Quickly clone the current area selection.\nThis basically just creates an in-memory-only schematic,\nand then creates a placement of that schematic and selects it,\nand also switches the tool mode to the Paste mode.");
//SH    public static final ConfigHotkey DELETE_SELECTION_BOX               = new ConfigHotkey("deleteSelectionBox",                "",     "Delete the currently selected box");
//SH    public static final ConfigHotkey EASY_PLACE_ACTIVATION              = new ConfigHotkey("easyPlaceActivation",               "",     KeybindSettings.MODIFIER_INGAME_EMPTY, "When the easyPlaceMode is enabled, this key must\nbe held to enable placing the blocks when\nusing the vanilla Use key");
//SH    public static final ConfigHotkey EASY_PLACE_TOGGLE                  = new ConfigHotkey("easyPlaceToggle",                   "",     "Allows quickly toggling on/off the Easy Place mode");
//SH    public static final ConfigHotkey EXECUTE_OPERATION                  = new ConfigHotkey("executeOperation",                  "",     "Execute the currently selected tool operation with the\ncurrent selection or placement in the Fill, Replace,\nPaste Schematic etc. modes");
    public static final ConfigHotkey INVERT_GHOST_BLOCK_RENDER_STATE    = new ConfigHotkey("invertGhostBlockRenderState",       "",     "Inverts the schematic/ghost block rendering status\nwhile this keybind is held down");
    public static final ConfigHotkey INVERT_OVERLAY_RENDER_STATE        = new ConfigHotkey("invertOverlayRenderState",          "",     "Inverts the Overlay rendering status while this keybind is held down");
    public static final ConfigHotkey LAYER_MODE_NEXT                    = new ConfigHotkey("layerModeNext",                     "M,PAGE_UP",    "Cycle the rendering mode (all, layers) forward");
    public static final ConfigHotkey LAYER_MODE_PREVIOUS                = new ConfigHotkey("layerModePrevious",                 "M,PAGE_DOWN",  "Cycle the rendering mode (all, layers) backwards");
    public static final ConfigHotkey LAYER_NEXT                         = new ConfigHotkey("layerNext",                         "PAGE_UP",      "Move the rendered layer selection up");
    public static final ConfigHotkey LAYER_PREVIOUS                     = new ConfigHotkey("layerPrevious",                     "PAGE_DOWN",    "Move the rendered layer selection down");
    public static final ConfigHotkey LAYER_SET_HERE                     = new ConfigHotkey("layerSetHere",                      "",     "Set the Render Layer to the player's current position");
    public static final ConfigHotkey NUDGE_SELECTION_NEGATIVE           = new ConfigHotkey("nudgeSelectionNegative",            "",     "Nudge the current selection in the \"negative\" direction\nThis is basically the same as mouse wheel down\nwith the Nudge modifier pressed");
    public static final ConfigHotkey NUDGE_SELECTION_POSITIVE           = new ConfigHotkey("nudgeSelectionPositive",            "",     "Nudge the current selection in the \"positive\" direction\nThis is basically the same as mouse wheel up\nwith the Nudge modifier pressed");
    public static final ConfigHotkey MOVE_ENTIRE_SELECTION              = new ConfigHotkey("moveEntireSelection",               "",     "Move the entire current selection here");
//SH    public static final ConfigHotkey OPEN_GUI_AREA_SETTINGS             = new ConfigHotkey("openGuiAreaSettings",               "KP_MULTIPLY", "Open the Area Settings GUI for the currently selected area");
    public static final ConfigHotkey OPEN_GUI_LOADED_SCHEMATICS         = new ConfigHotkey("openGuiLoadedSchematics",           "",     "Open the Loaded Schematic GUI");
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU                 = new ConfigHotkey("openGuiMainMenu",                   "M",    KeybindSettings.RELEASE_EXCLUSIVE, "Open the Litematica main menu");
    public static final ConfigHotkey OPEN_GUI_MATERIAL_LIST             = new ConfigHotkey("openGuiMaterialList",               "M,L",  "Open the Material List GUI for the currently\nselected schematic placement");
//SH    public static final ConfigHotkey OPEN_GUI_PLACEMENT_SETTINGS        = new ConfigHotkey("openGuiPlacementSettings",          "KP_SUBTRACT", "Open the Placement Settings GUI for the currently\nselected placement or sub-region");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PLACEMENTS      = new ConfigHotkey("openGuiSchematicPlacements",        "M,P",  "Open the Schematic Placements GUI");
//SH    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PROJECTS        = new ConfigHotkey("openGuiSchematicProjects",          "",     "Open the Schematic Projects GUI");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_VERIFIER        = new ConfigHotkey("openGuiSchematicVerifier",          "M,V",  "Open the Schematic Verifier GUI for the currently\nselected schematic placement");
//SH    public static final ConfigHotkey OPEN_GUI_SELECTION_MANAGER         = new ConfigHotkey("openGuiSelectionManager",           "M,S",  "Open the Area Selection manager GUI");
    public static final ConfigHotkey OPEN_GUI_SETTINGS                  = new ConfigHotkey("openGuiSettings",                   "M,C",  "Open the Config GUI");
    public static final ConfigHotkey OPERATION_MODE_CHANGE_MODIFIER     = new ConfigHotkey("operationModeChangeModifier",       "LEFT_CONTROL", KeybindSettings.MODIFIER_INGAME, "The modifier key to quickly change the operation mode.\nHold this and scroll while holding the \"tool item\" to quickly cycle the mode.");
//SH    public static final ConfigHotkey PICK_BLOCK_FIRST                   = new ConfigHotkey("pickBlockFirst",                    "BUTTON_3",     KeybindSettings.PRESS_ALLOWEXTRA, "A key to pick block the first\nschematic block ray traced to");
//SH    public static final ConfigHotkey PICK_BLOCK_LAST                    = new ConfigHotkey("pickBlockLast",                     "",             KeybindSettings.MODIFIER_INGAME, "A key to pick block the last schematic block\nray traced to, before the first (possible) client world\nblock ray traced to. Basically this would get\nyou the block you could place against an existing block.");
//SH    public static final ConfigHotkey PICK_BLOCK_TOGGLE                  = new ConfigHotkey("pickBlockToggle",                   "M,BUTTON_3",   "A hotkey to toggle the pick block toggle option in the\nGeneric configs. This is provided as a quick way to enable\nor disable the pick block keys, if they interfere with something.");
    public static final ConfigHotkey RENDER_INFO_OVERLAY                = new ConfigHotkey("renderInfoOverlay",                 "I",            KeybindSettings.PRESS_ALLOWEXTRA, "The key that enables rendering the block info overlay.\nUse NONE for not requiring a key to be pressed.\nDisable the similarly named option in the Visuals\nconfigs to disable the overlay completely.");
    public static final ConfigHotkey RENDER_OVERLAY_THROUGH_BLOCKS      = new ConfigHotkey("renderOverlayThroughBlocks",        "RIGHT_CONTROL", KeybindSettings.PRESS_ALLOWEXTRA, "A hotkey to allow the overlays to render through blocks.\nThis is just a quicker way to temporarily enable\nthe same thing that the 'schematicOverlayRenderThroughBlocks' option in Visuals does.");
    public static final ConfigHotkey RERENDER_SCHEMATIC                 = new ConfigHotkey("rerenderSchematic",                 "F3,M", "Hotkey to refresh/redraw only the schematic, instead of\nhaving to refresh the vanilla terrain too with F3 + A");
//SH    public static final ConfigHotkey SAVE_AREA_AS_IN_MEMORY_SCHEMATIC   = new ConfigHotkey("saveAreaAsInMemorySchematic",       "",     "Save the current Area Selection as an in-memory Schematic");
//SH    public static final ConfigHotkey SAVE_AREA_AS_SCHEMATIC_TO_FILE     = new ConfigHotkey("saveAreaAsSchematicToFile",         "LEFT_CONTROL,LEFT_ALT,S",  "Save the current Area Selection as a Schematic to a file");
//SH    public static final ConfigHotkey SCHEMATIC_REBUILD_BREAK_ALL_EXCEPT = new ConfigHotkey("schematicRebuildBreakAllExcept",    "",     KeybindSettings.MODIFIER_INGAME, "Modifier key to activate the \"break everything except\"\nmode/function in the Schematic Rebuild tool mode.\nBasically when you hold this key and punch a schematic block,\nall other blocks except this block will be removed from the schematic.");
//SH    public static final ConfigHotkey SCHEMATIC_REBUILD_BREAK_ALL        = new ConfigHotkey("schematicRebuildBreakPlaceAll",     "",     KeybindSettings.MODIFIER_INGAME, "Modifier key to activate the \"break all identical blocks\"\nfunction in the Schematic Rebuild tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_REBUILD_BREAK_DIRECTION  = new ConfigHotkey("schematicRebuildBreakPlaceDirection","",    KeybindSettings.MODIFIER_INGAME, "Modifier key to activate the directional/continuous\nbreak or place function in the Schematic Rebuild tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_REBUILD_REPLACE_ALL      = new ConfigHotkey("schematicRebuildReplaceAll",        "",     KeybindSettings.MODIFIER_INGAME, "Modifier key to activate the \"replace all identical\"\nreplace mode/function in the Schematic Rebuild tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_REBUILD_REPLACE_DIRECTION= new ConfigHotkey("schematicRebuildReplaceDirection",  "",     KeybindSettings.MODIFIER_INGAME, "Modifier key to activate the directional/continuous\nreplace mode/function in the Schematic Rebuild tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_MODIFIER   = new ConfigHotkey("schematicVersionCycleModifier",     "",     KeybindSettings.MODIFIER_INGAME, "The modifier key to hold to be able to use the mouse wheel\nto cycle through the schematic versions in the Version Control tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_NEXT       = new ConfigHotkey("schematicVersionCycleNext",         "",     "A hotkey to cycle to the next schematic version in the Version Control tool mode");
//SH    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_PREVIOUS   = new ConfigHotkey("schematicVersionCyclePrevious",     "",     "A hotkey to cycle to the next schematic version in the Version Control tool mode");
//SH    public static final ConfigHotkey SELECTION_GRAB_MODIFIER            = new ConfigHotkey("selectionGrabModifier",             "",     KeybindSettings.MODIFIER_INGAME, "The modifier key to hold to \"grab\" a selection\nbox or corner for cursor moving.");
//SH    public static final ConfigHotkey SELECTION_GROW_HOTKEY              = new ConfigHotkey("selectionGrow",                     "",     "The action hotkey to auto-grow the selection box around\nany adjacent/diagonally connected blocks");
//SH    public static final ConfigHotkey SELECTION_GROW_MODIFIER            = new ConfigHotkey("selectionGrowModifier",             "",     KeybindSettings.MODIFIER_INGAME, "The modifier key to hold to grow or shrink\na selection box while scrolling");
//SH    public static final ConfigHotkey SELECTION_NUDGE_MODIFIER           = new ConfigHotkey("selectionNudgeModifier",            "LEFT_ALT", KeybindSettings.MODIFIER_INGAME, "The modifier key to hold while scrolling\nto nudge the selected area or corner");
//SH    public static final ConfigHotkey SELECTION_MODE_CYCLE               = new ConfigHotkey("selectionModeCycle",                "LEFT_CONTROL,M", "Change the mode between Corners and Cuboid\nin the Area Selection mode");
//SH    public static final ConfigHotkey SELECTION_SHRINK_HOTKEY            = new ConfigHotkey("selectionShrink",                   "",     "The action hotkey to shrink the selection box so that there\nisn't any empty space (empty layers) on any side");
    public static final ConfigHotkey SET_AREA_ORIGIN                    = new ConfigHotkey("setAreaOrigin",                     "",     "Set/move the origin point of the current\narea selection to the player's position");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_1       = new ConfigHotkey("setSelectionBoxPosition1",          "",     "Set the first position of the currently selected\nbox to the player's position");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_2       = new ConfigHotkey("setSelectionBoxPosition2",          "",     "Set the second position of the currently selected\nbox to the player's position");
    public static final ConfigHotkey TOGGLE_ALL_RENDERING               = new ConfigHotkey("toggleAllRendering",                "M,R",  "Toggle all rendering on/off", "All Rendering");
    public static final ConfigHotkey TOGGLE_AREA_SELECTION_RENDERING    = new ConfigHotkey("toggleAreaSelectionBoxesRendering", "",     "Toggle Area Selection boxes rendering on/off");
    public static final ConfigHotkey TOGGLE_SCHEMATIC_RENDERING         = new ConfigHotkey("toggleSchematicRendering",          "M,G",  "Toggle schematic rendering (blocks & overlay) on/off");
    public static final ConfigHotkey TOGGLE_INFO_OVERLAY_RENDERING      = new ConfigHotkey("toggleInfoOverlayRendering",        "",     "Toggle the info overlay rendering (for hovered block info)");
    public static final ConfigHotkey TOGGLE_OVERLAY_RENDERING           = new ConfigHotkey("toggleOverlayRendering",            "",     "Toggle the block overlay rendering on/off");
    public static final ConfigHotkey TOGGLE_OVERLAY_OUTLINE_RENDERING   = new ConfigHotkey("toggleOverlayOutlineRendering",     "",     "Toggle the block overlay outline rendering on/off");
    public static final ConfigHotkey TOGGLE_OVERLAY_SIDE_RENDERING      = new ConfigHotkey("toggleOverlaySideRendering",        "",     "Toggle the block overlay side rendering on/off");
    public static final ConfigHotkey TOGGLE_PLACEMENT_BOXES_RENDERING   = new ConfigHotkey("togglePlacementBoxesRendering",     "",     "Toggle Schematic Placement boxes rendering on/off");
//SH   public static final ConfigHotkey TOGGLE_PLACEMENT_RESTRICTION       = new ConfigHotkey("togglePlacementRestriction",        "",     "A hotkey to toggle the placement restriction mode");
    public static final ConfigHotkey TOGGLE_SCHEMATIC_BLOCK_RENDERING   = new ConfigHotkey("toggleSchematicBlockRendering",     "",     "Toggle schematic block rendering on/off");
    public static final ConfigHotkey TOGGLE_TRANSLUCENT_RENDERING       = new ConfigHotkey("toggleTranslucentRendering",        "",     "Toggle translucent vs. opaque ghost block rendering");
    public static final ConfigHotkey TOGGLE_VERIFIER_OVERLAY_RENDERING  = new ConfigHotkey("toggleVerifierOverlayRendering",    "",     "Toggle the Schematic Verifier overlay rendering");
    public static final ConfigHotkey TOOL_ENABLED_TOGGLE                = new ConfigHotkey("toolEnabledToggle",                 "M,T",  "The keybind to toggle the \"tool\" item functionality on/off");
    public static final ConfigHotkey TOOL_PLACE_CORNER_1                = new ConfigHotkey("toolPlaceCorner1",                  "BUTTON_1", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use while holding the \"tool\" item\nto place the primary/first corner");
    public static final ConfigHotkey TOOL_PLACE_CORNER_2                = new ConfigHotkey("toolPlaceCorner2",                  "BUTTON_2", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use while holding the \"tool\" item\nto place the second corner");
//SH    public static final ConfigHotkey TOOL_SELECT_ELEMENTS               = new ConfigHotkey("toolSelectElements",                "BUTTON_3", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use to select corners or boxes\nwhile holding the \"tool\" item");
//SH    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_1       = new ConfigHotkey("toolSelectModifierBlock1",          "LEFT_ALT", KeybindSettings.MODIFIER_INGAME, "The modifier key to hold while using the 'toolSelectElements'\nhotkey, to select the primary block type to use in some of the tool modes");
//SH    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_2       = new ConfigHotkey("toolSelectModifierBlock2",          "LEFT_SHIFT", KeybindSettings.MODIFIER_INGAME, "The modifier key to hold while using the 'toolSelectElements'\nhotkey, to select the secondary block type to use in some of the tool modes");
    public static final ConfigHotkey UNLOAD_CURRENT_SCHEMATIC           = new ConfigHotkey("unloadCurrentSchematic",            "",     "Unloads the schematic of the currently selected placement,and thus also removes all placements created from it\n");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
//SH            ADD_SELECTION_BOX,
//SH            CLONE_SELECTION,
//SH            DELETE_SELECTION_BOX,
//SH            EASY_PLACE_ACTIVATION,
//SH            EASY_PLACE_TOGGLE,
//SH            EXECUTE_OPERATION,
            INVERT_GHOST_BLOCK_RENDER_STATE,
            INVERT_OVERLAY_RENDER_STATE,
            LAYER_MODE_NEXT,
            LAYER_MODE_PREVIOUS,
            LAYER_NEXT,
            LAYER_PREVIOUS,
            LAYER_SET_HERE,
            NUDGE_SELECTION_NEGATIVE,
            NUDGE_SELECTION_POSITIVE,
            MOVE_ENTIRE_SELECTION,
//SH            OPEN_GUI_AREA_SETTINGS,
            OPEN_GUI_LOADED_SCHEMATICS,
            OPEN_GUI_MAIN_MENU,
            OPEN_GUI_MATERIAL_LIST,
//SH            OPEN_GUI_PLACEMENT_SETTINGS,
            OPEN_GUI_SCHEMATIC_PLACEMENTS,
//SH            OPEN_GUI_SCHEMATIC_PROJECTS,
            OPEN_GUI_SCHEMATIC_VERIFIER,
//SH            OPEN_GUI_SELECTION_MANAGER,
            OPEN_GUI_SETTINGS,
            OPERATION_MODE_CHANGE_MODIFIER,
//SH            PICK_BLOCK_FIRST,
//SH            PICK_BLOCK_LAST,
//SH            PICK_BLOCK_TOGGLE,
            RENDER_INFO_OVERLAY,
            RENDER_OVERLAY_THROUGH_BLOCKS,
            RERENDER_SCHEMATIC,
//SH            SAVE_AREA_AS_IN_MEMORY_SCHEMATIC,
//SH            SAVE_AREA_AS_SCHEMATIC_TO_FILE,
//SH            SCHEMATIC_REBUILD_BREAK_ALL,
//SH            SCHEMATIC_REBUILD_BREAK_ALL_EXCEPT,
//SH            SCHEMATIC_REBUILD_BREAK_DIRECTION,
//SH            SCHEMATIC_REBUILD_REPLACE_ALL,
//SH            SCHEMATIC_REBUILD_REPLACE_DIRECTION,
//SH            SCHEMATIC_VERSION_CYCLE_MODIFIER,
//SH            SCHEMATIC_VERSION_CYCLE_NEXT,
//SH            SCHEMATIC_VERSION_CYCLE_PREVIOUS,
//SH            SELECTION_GRAB_MODIFIER,
//SH            SELECTION_GROW_HOTKEY,
//SH            SELECTION_GROW_MODIFIER,
//SH            SELECTION_NUDGE_MODIFIER,
//SH            SELECTION_MODE_CYCLE,
//SH            SELECTION_SHRINK_HOTKEY,
            SET_AREA_ORIGIN,
            SET_SELECTION_BOX_POSITION_1,
            SET_SELECTION_BOX_POSITION_2,
            TOGGLE_ALL_RENDERING,
            TOGGLE_AREA_SELECTION_RENDERING,
            TOGGLE_INFO_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_OUTLINE_RENDERING,
            TOGGLE_OVERLAY_SIDE_RENDERING,
            TOGGLE_PLACEMENT_BOXES_RENDERING,
//SH            TOGGLE_PLACEMENT_RESTRICTION,
            TOGGLE_SCHEMATIC_BLOCK_RENDERING,
            TOGGLE_SCHEMATIC_RENDERING,
            TOGGLE_TRANSLUCENT_RENDERING,
            TOGGLE_VERIFIER_OVERLAY_RENDERING,
            TOOL_ENABLED_TOGGLE,
            TOOL_PLACE_CORNER_1,
            TOOL_PLACE_CORNER_2,
//SH            TOOL_SELECT_ELEMENTS,
//SH            TOOL_SELECT_MODIFIER_BLOCK_1,
//SH            TOOL_SELECT_MODIFIER_BLOCK_2,
            UNLOAD_CURRENT_SCHEMATIC
    );
}
