package com.clementfornes;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Raccourcis clavier du mod, centralisés et standardisés.
 */
public final class KeyBindings {
    private KeyBindings() {
    }

    private static final String PREFIX = "key.excaliavotemod.";
    private static final String CATEGORY = "key.categories.excaliavotemod";

    public static KeyBinding TOGGLE_HUD;
    public static KeyBinding INCREASE_SCALE;
    public static KeyBinding DECREASE_SCALE;
    public static KeyBinding CYCLE_STYLE;
    public static KeyBinding CYCLE_ANCHOR;
    public static KeyBinding OPEN_CONFIG;
    public static KeyBinding DISABLE_SHORTCUTS;

    public static void register() {
        TOGGLE_HUD = bind("toggle_hud", GLFW.GLFW_KEY_H);
        INCREASE_SCALE = bind("increase_hud", GLFW.GLFW_KEY_KP_ADD);
        DECREASE_SCALE = bind("decrease_hud", GLFW.GLFW_KEY_KP_SUBTRACT);
        CYCLE_STYLE = bind("cycle_style", GLFW.GLFW_KEY_J);
        CYCLE_ANCHOR = bind("cycle_anchor", GLFW.GLFW_KEY_K);
        OPEN_CONFIG = bind("open_config", GLFW.GLFW_KEY_O);
        DISABLE_SHORTCUTS = bind("disable_shortcuts", GLFW.GLFW_KEY_U);
    }

    private static KeyBinding bind(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                PREFIX + name,
                InputUtil.Type.KEYSYM,
                defaultKey,
                CATEGORY));
    }
}