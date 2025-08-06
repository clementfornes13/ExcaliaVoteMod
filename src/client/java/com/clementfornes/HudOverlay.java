package com.clementfornes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.*;

public class HudOverlay {
    private final ConfigManager config;
    private boolean showHud = true;
    private long lastFetch = 0;
    private final long refreshInterval = 5 * 60 * 1000;
    private final int padding = 6;
    private final int lineHeight = 13;

    private final Map<String, Boolean> voteAvailability = new HashMap<>();
    private final Map<String, Long> animationTimers = new HashMap<>();
    private static final int animationDuration = 3000;
    public static final String[] styles = {
            "Minimaliste", "Dégradé", "Arc-en-ciel", "Compact", "Lisible", "Terminal"
    };

    public HudOverlay(ConfigManager config) {
        this.config = config;
    }

    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter, VoteService service) {
        MinecraftClient mc = MinecraftClient.getInstance();
        handleInput(mc);

        if (!showHud || mc.player == null || !ServerUtils.isExcaliaServer(mc))
            return;

        long now = System.currentTimeMillis();
        if (now - lastFetch > refreshInterval) {
            lastFetch = now;
            service.fetch(mc.getSession().getUsername());
        }

        List<String> lines = buildLines(service);
        List<Integer> colors = buildColors(mc, service, now);

        int width = calcWidth(mc, lines) + padding * 2;
        int height = lines.size() * lineHeight + padding * 2;

        float scale = config.getHudScale();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int wScaled = (int) (width * scale);
        int hScaled = (int) (height * scale);
        int rawX, rawY;
        switch (config.getHudAnchor()) {
            case 1 -> {
                rawX = (sw - wScaled - padding) / (int) scale;
                rawY = padding;
            }
            case 2 -> {
                rawX = ((sw - wScaled) / 2) / (int) scale;
                rawY = ((sh - hScaled) / 2) / (int) scale;
            }
            case 3 -> {
                rawX = (sw - wScaled - padding) / (int) scale;
                rawY = (sh - hScaled - padding) / (int) scale;
            }
            default -> {
                rawX = padding;
                rawY = padding;
            }
        }
        int x = rawX;
        int y = rawY;

        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1);

        String style = styles[config.getStyleIndex()];
        switch (style) {
            case "Minimaliste" -> {
                long nearestRem = Long.MAX_VALUE;
                long nowLocal = System.currentTimeMillis();
                if (service.sites != null) {
                    for (long resetTime : service.sites.values()) {
                        long remTime = resetTime - nowLocal;
                        if (remTime > 0 && remTime < nearestRem) {
                            nearestRem = remTime;
                        }
                    }
                }
                String nearestStr;
                if (nearestRem == Long.MAX_VALUE || nearestRem <= 0) {
                    nearestStr = "Disponible";
                } else {
                    nearestStr = String.format("%02d:%02d", nearestRem / 60000, (nearestRem / 1000) % 60);
                }
                String total = service.totalVotes == -1 ? "..." : String.valueOf(service.totalVotes);
                String display = total + " | " + nearestStr;
                ctx.drawText(mc.textRenderer, Text.literal(display), x, y, 0xFFFFFF, false);
            }
            case "Compact" -> {
                long nowLocal = System.currentTimeMillis();
                long nearestRem = Long.MAX_VALUE;
                String nearestId = null;
                if (service.sites != null) {
                    for (Map.Entry<String, Long> entry : service.sites.entrySet()) {
                        long remTime = entry.getValue() - nowLocal;
                        if (remTime > 0 && remTime < nearestRem) {
                            nearestRem = remTime;
                            nearestId = entry.getKey();
                        }
                    }
                }
                String nearestStr = (nearestRem == Long.MAX_VALUE || nearestRem <= 0)
                        ? "Disponible"
                        : String.format("%02d:%02d", nearestRem / 60000, (nearestRem / 1000) % 60);
                String totalStr = service.totalVotes == -1 ? "..." : String.valueOf(service.totalVotes);
                String siteName = nearestId == null ? "" : getSiteName(nearestId) + ": ";
                String display = "Votes " + totalStr + " | " + siteName + nearestStr;
                ctx.drawText(mc.textRenderer, Text.literal(display), x, y, 0x00FF88, false);
            }
            case "Lisible" -> {
                int pad = padding * 2;
                int w = calcWidth(mc, lines) + pad * 2;
                int h = lines.size() * lineHeight + pad * 2;
                ctx.fill(x, y, x + w, y + h, 0xCCFFFFFF);
                ctx.drawBorder(x, y, w + 1, h + 1, 0xFF000000);
                int ty2 = y + pad;
                for (int i = 0; i < lines.size(); i++) {
                    ctx.drawText(mc.textRenderer, Text.literal(lines.get(i)), x + pad, ty2, colors.get(i), false);
                    ty2 += lineHeight;
                }
            }
            case "Terminal" -> {
                int w2 = calcWidth(mc, lines) + padding * 2;
                int x0 = x;
                int y0 = y;
                ctx.drawText(mc.textRenderer, Text.literal("+" + "-".repeat(w2 / 6) + "+"), x0, y0, 0x00FF00, false);
                int y1 = y0 + lineHeight;
                for (String line : lines) {
                    ctx.drawText(mc.textRenderer, Text.literal("| " + line), x0, y1, 0x00FF00, false);
                    y1 += lineHeight;
                }
                ctx.drawText(mc.textRenderer, Text.literal("+" + "-".repeat(w2 / 6) + "+"), x0, y1, 0x00FF00, false);
            }
            case "Dégradé", "Arc-en-ciel" -> {
                ctx.fill(x - 2, y - 2, x + width, y + height, 0x88000000);
                ctx.drawBorder(x - 2, y - 2, width + 2, height + 2, 0xFFAAAAAA);

                int ty = y + padding;
                for (int i = 0; i < lines.size(); i++) {
                    ctx.drawText(mc.textRenderer, Text.literal(lines.get(i)), x + padding, ty, colors.get(i), true);
                    ty += lineHeight;
                }
            }
            default -> {
                int ty = y + padding;
                for (int i = 0; i < lines.size(); i++) {
                    ctx.drawText(mc.textRenderer, Text.literal(lines.get(i)), x + padding, ty, colors.get(i), true);
                    ty += lineHeight;
                }
            }
        }

        ctx.getMatrices().pop();

        config.save();
    }

    private void handleInput(MinecraftClient mc) {
        if (KeyBindings.TOGGLE_HUD.wasPressed())
            showHud = !showHud;
        if (KeyBindings.INCREASE_SCALE.wasPressed())
            config.setHudScale(Math.min(config.getHudScale() + 0.1f, 2.0f));
        if (KeyBindings.DECREASE_SCALE.wasPressed())
            config.setHudScale(Math.max(config.getHudScale() - 0.1f, 0.5f));
        if (KeyBindings.CYCLE_STYLE.wasPressed())
            config.setStyleIndex((config.getStyleIndex() + 1) % styles.length);
        if (KeyBindings.CYCLE_ANCHOR.wasPressed())
            config.setHudAnchor((config.getHudAnchor() + 1) % 4);
        if (KeyBindings.OPEN_CONFIG.wasPressed()) {
            mc.setScreen(new ExcaliaConfigScreen(config));
        }
        if (KeyBindings.RESET_CONFIG.wasPressed()) {
            config.setHudScale(1.0f);
            config.setStyleIndex(0);
            config.setHudAnchor(0);
            config.save();
            mc.setScreen(new ExcaliaConfigScreen(config));
        }
    }

    private List<String> buildLines(VoteService service) {
        List<String> lines = new ArrayList<>();
        if (service.totalVotes == -1) {
            lines.add("Chargement des votes...");
        } else if (service.sites == null) {
            lines.add("Erreur récupération votes, veuillez créer un compte sur le site de Excalia");
        } else {
            lines.add("Votes ce mois-ci : " + service.totalVotes);
            for (Map.Entry<String, Long> entry : service.sites.entrySet()) {
                long rem = entry.getValue() - System.currentTimeMillis();
                boolean available = rem <= 0;
                String timeStr = available
                        ? "Disponible"
                        : String.format("%02d:%02d", rem / 60000, (rem / 1000) % 60);
                lines.add(getSiteName(entry.getKey()) + " : " + timeStr);
            }
        }
        return lines;
    }

    private List<Integer> buildColors(MinecraftClient mc, VoteService service, long now) {
        List<Integer> colors = new ArrayList<>();
        if (service.totalVotes == -1) {
            colors.add(0xAAAAAA);
        } else if (service.sites == null) {
            colors.add(0xFF5555);
        } else {
            colors.add(0x00FF88);
            for (Map.Entry<String, Long> entry : service.sites.entrySet()) {
                long rem = entry.getValue() - now;
                boolean available = rem <= 0;
                boolean wasAvailable = voteAvailability.getOrDefault(entry.getKey(), false);
                if (available && !wasAvailable) {
                    mc.player.playSound(
                            Registries.SOUND_EVENT.get(Identifier.of("minecraft", "entity.experience_orb.pickup")),
                            1.0f, 1.0f);
                    animationTimers.put(entry.getKey(), now);
                }
                voteAvailability.put(entry.getKey(), available);

                int color;
                String style = styles[config.getStyleIndex()];
                if (style.equals("Dégradé")) {
                    double ratio = Math.max(0, Math.min(1, (double) rem / 7_200_000));
                    color = ((int) (255 * ratio) << 16) | ((int) (255 * (1 - ratio)) << 8);
                } else if (style.equals("Arc-en-ciel")) {
                    float hue = ((now % 10000L) / 10000f + entry.getKey().hashCode()) % 1.0f;
                    color = Color.HSBtoRGB(hue, 1f, 1f);
                } else {
                    color = available ? 0x00FF00 : 0xFFFFFF;
                }

                if (available && animationTimers.containsKey(entry.getKey())) {
                    long elapsed = now - animationTimers.get(entry.getKey());
                    if (elapsed < animationDuration) {
                        float alpha = (float) Math.sin(elapsed / 150.0f * Math.PI);
                        color = ((int) (255 * alpha) << 24) | (color & 0xFFFFFF);
                    } else {
                        animationTimers.remove(entry.getKey());
                    }
                }
                colors.add(color);
            }
        }
        return colors;
    }

    private int calcWidth(MinecraftClient mc, List<String> lines) {
        return lines.stream()
                .mapToInt(s -> mc.textRenderer.getWidth(s))
                .max().orElse(100);
    }

    private static String getSiteName(String id) {
        return switch (id) {
            case "2" -> "top-serveurs.net";
            case "4" -> "www.serveursminecraft.org";
            case "5" -> "serveur-prive.net";
            case "6" -> "serveur-minecraft-vote.fr";
            case "7" -> "www.serveur-minecraft.com";
            default -> "Site #" + id;
        };
    }
}
