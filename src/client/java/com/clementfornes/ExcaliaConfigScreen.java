package com.clementfornes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Écran de configuration pour Excalia Vote Mod.
 * - Échelle HUD (0.5 → 2.0) via slider
 * - Style (enum HudStyle)
 * - Position (ancrage)
 * - Message personnalisé à envoyer via la boussole
 */
@Environment(EnvType.CLIENT)
public class ExcaliaConfigScreen extends Screen {
    private static final int CONTROL_WIDTH = 240;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_SPACING = 30;

    private static final float SCALE_MIN = 0.5f;
    private static final float SCALE_MAX = 2.0f;

    private final ConfigManager config;
    private SliderWidget scaleSlider;
    private ButtonWidget styleButton;
    private ButtonWidget anchorButton;
    private TextFieldWidget autoMessageField;

    public ExcaliaConfigScreen(ConfigManager config) {
        super(Text.literal("Excalia Vote Mod Configuration"));
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // --- HUD Scale slider ---
        float scale = clamp(config.getHudScale(), SCALE_MIN, SCALE_MAX);
        double norm = (scale - SCALE_MIN) / (SCALE_MAX - SCALE_MIN);

        scaleSlider = new SliderWidget(
                centerX - CONTROL_WIDTH / 2, y,
                CONTROL_WIDTH, CONTROL_HEIGHT,
                Text.literal("HUD Scale"),
                norm) {
            @Override
            protected void updateMessage() {
                float s = (float) (SCALE_MIN + value * (SCALE_MAX - SCALE_MIN));
                this.setMessage(Text.literal(String.format("HUD Scale: %.2f", s)));
            }

            @Override
            protected void applyValue() {
                float s = (float) (SCALE_MIN + value * (SCALE_MAX - SCALE_MIN));
                config.setHudScale(s);
            }
        };
        this.addDrawableChild(scaleSlider);

        // --- Style selector ---
        y += CONTROL_SPACING;
        HudOverlay.HudStyle currentStyle = HudOverlay.HudStyle.fromIndex(config.getStyleIndex());
        styleButton = ButtonWidget.builder(
                Text.literal("Style: " + currentStyle.name()),
                btn -> {
                    int next = (config.getStyleIndex() + 1) % HudOverlay.HudStyle.values().length;
                    config.setStyleIndex(next);
                    HudOverlay.HudStyle updated = HudOverlay.HudStyle.fromIndex(next);
                    btn.setMessage(Text.literal("Style: " + updated.name()));
                })
                .dimensions(centerX - CONTROL_WIDTH / 2, y, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build();
        this.addDrawableChild(styleButton);

        // --- Anchor selector ---
        y += CONTROL_SPACING;
        anchorButton = ButtonWidget.builder(
                Text.literal("Position: " + config.getHudAnchor()),
                btn -> {
                    int next = (config.getHudAnchor() + 1) % 4;
                    config.setHudAnchor(next);
                    btn.setMessage(Text.literal("Position: " + next));
                })
                .dimensions(centerX - CONTROL_WIDTH / 2, y, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build();
        this.addDrawableChild(anchorButton);

        // --- Auto message field ---
        y += CONTROL_SPACING;
        autoMessageField = new TextFieldWidget(
                this.textRenderer,
                centerX - CONTROL_WIDTH / 2, y,
                CONTROL_WIDTH, CONTROL_HEIGHT,
                Text.literal("Auto Message:"));
        autoMessageField.setMaxLength(256);
        autoMessageField.setText(config.getAutoMessage());
        this.addDrawableChild(autoMessageField);

        // --- Done button ---
        y += CONTROL_SPACING;
        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Done"),
                        btn -> {
                            config.setAutoMessage(autoMessageField.getText());
                            config.save(); // Immediate save to JSON
                            MinecraftClient.getInstance().setScreen(null);
                        })
                        .dimensions(centerX - CONTROL_WIDTH / 2, y, CONTROL_WIDTH, CONTROL_HEIGHT)
                        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        autoMessageField.render(context, mouseX, mouseY, delta);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}