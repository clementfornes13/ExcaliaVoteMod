package com.clementfornes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ExcaliaConfigScreen extends Screen {
    private final ConfigManager config;

    public ExcaliaConfigScreen(ConfigManager config) {
        super(Text.literal("Configuration Excalia Vote Mod"));
        this.config = config;
    }

    @Override
    protected void init() {
        int midWidth = this.width / 2;
        int y = 50;

        this.addDrawableChild(new SliderWidget(
                midWidth - 100, y, 200, 20,
                Text.literal("Échelle HUD"),
                config.getHudScale()) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal(String.format("Échelle HUD: %.2f", value)));
            }

            @Override
            protected void applyValue() {
                config.setHudScale((float) value);
            }
        });
        y += 30;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Style: " + HudOverlay.styles[config.getStyleIndex()]),
                button -> {
                    int next = (config.getStyleIndex() + 1) % HudOverlay.styles.length;
                    config.setStyleIndex(next);
                    button.setMessage(Text.literal("Style: " + HudOverlay.styles[next]));
                })
                .dimensions(midWidth - 100, y, 200, 20)
                .build());
        y += 30;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Position: " + config.getHudAnchor()),
                button -> {
                    int next = (config.getHudAnchor() + 1) % 4;
                    config.setHudAnchor(next);
                    button.setMessage(Text.literal("Position: " + next));
                })
                .dimensions(midWidth - 100, y, 200, 20)
                .build());
        y += 40;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Fermer"),
                button -> MinecraftClient.getInstance().setScreen(null))
                .dimensions(midWidth - 100, y, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}