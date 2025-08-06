package com.clementfornes;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding TOGGLE_HUD;
    public static KeyBinding INCREASE_SCALE;
    public static KeyBinding DECREASE_SCALE;
    public static KeyBinding CYCLE_STYLE;
    public static KeyBinding CYCLE_ANCHOR;
    public static KeyBinding OPEN_CONFIG;
    public static KeyBinding RESET_CONFIG;

    public static void register() {
        TOGGLE_HUD = create("toggle_hud", GLFW.GLFW_KEY_H);
        INCREASE_SCALE = create("increase_hud", GLFW.GLFW_KEY_KP_ADD);
        DECREASE_SCALE = create("decrease_hud", GLFW.GLFW_KEY_KP_SUBTRACT);
        CYCLE_STYLE = create("cycle_style", GLFW.GLFW_KEY_J);
        CYCLE_ANCHOR = create("cycle_anchor", GLFW.GLFW_KEY_K);
        OPEN_CONFIG = create("open_config", GLFW.GLFW_KEY_O);
        RESET_CONFIG = create("reset_config", GLFW.GLFW_KEY_R);
    }

    private static KeyBinding create(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.excaliavotemod." + name,
                        InputUtil.Type.KEYSYM,
                        key,
                        "key.categories.excaliavotemod"));
    }
}
