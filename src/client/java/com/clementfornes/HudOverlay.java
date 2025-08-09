package com.clementfornes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

/**
 * HUD overlay optimisé:
 * - Réutilise les listes par frame pour limiter les allocations
 * - Styles en enum
 * - Centralisation du temps et constantes nommées
 * - Debounce de sauvegarde de la config
 * - Boussole unique au-dessus du prompt de chat, tooltip + cooldown visible
 */
public class HudOverlay {
    private final ConfigManager config;

    // UI layout
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 13;

    // Timing constants
    private static final long ANIMATION_DURATION_MS = 3_000L;
    private static final int SCREENSHOT_HIDE_DURATION_TICKS = 20;
    private static final long CENTER_MESSAGE_DURATION_MS = 3_000L;
    private static final long CONFIG_SAVE_DEBOUNCE_MS = 500L;
    private static final long RAINBOW_CYCLE_MS = 10_000L;

    // Reusable buffers (évite allocations par frame)
    private final List<String> lines = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();

    // State
    private boolean showHud = true;
    private final Map<String, Boolean> voteAvailability = new HashMap<>();
    private final Map<String, Long> animationTimers = new HashMap<>();
    private boolean configDirty = false;
    private long lastConfigChangeTime = 0L;

    private boolean hideOnScreenshot = false;
    private int screenshotTimer = 0;

    private boolean showCenterMessage = false;
    private long centerMessageStart = 0L;
    private String centerMessage = "";

    private boolean shortcutsEnabled = true;
    // doit être static car utilisé depuis la classe interne statique
    static boolean wasMouseDown = false;

    // Text constants
    private static final String MSG_LOADING = "Chargement des votes...";
    private static final String MSG_ERROR = "Erreur récupération votes";
    private static final String MSG_AVAILABLE = "Disponible";
    private static final String MSG_VOTE_AVAILABLE_TITLE = "Vote dispo !";

    public HudOverlay(ConfigManager config) {
        this.config = config;
    }

    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter, VoteService service) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Cache temps pour la frame
        long now = System.currentTimeMillis();

        // Hide all on screenshot
        if (hideOnScreenshot) {
            if (--screenshotTimer <= 0) {
                hideOnScreenshot = false;
            } else {
                attemptSaveConfig(now);
                return;
            }
        }

        handleInput(mc, now);

        if (!showHud || mc.player == null || !ServerUtils.isExcaliaServer(mc)) {
            attemptSaveConfig(now);
            return;
        }

        // Prepare data (réutilise les listes)
        lines.clear();
        colors.clear();
        buildLines(service, now);
        buildColors(mc, service, now);

        float scale = config.getHudScale();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        HudStyle style = HudStyle.fromIndex(config.getStyleIndex());

        // Render area
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1);

        HudRenderer.render(ctx, mc, config, service, style, lines, colors, sw, sh, PADDING, LINE_HEIGHT, now);

        ctx.getMatrices().pop();

        HudRenderer.drawCenterMessage(ctx, mc, centerMessage, centerMessageStart, showCenterMessage,
                CENTER_MESSAGE_DURATION_MS);

        attemptSaveConfig(now);

        // Draw single compass (cooldown + tooltip)
        HudRenderer.drawCompass(ctx, mc, config, now);
    }

    private void handleInput(MinecraftClient mc, long now) {
        if (KeyBindings.DISABLE_SHORTCUTS.wasPressed()) {
            shortcutsEnabled = !shortcutsEnabled;
            return;
        }
        if (!shortcutsEnabled)
            return;

        if (KeyBindings.TOGGLE_HUD.wasPressed()) {
            showHud = !showHud;
        }
        if (KeyBindings.INCREASE_SCALE.wasPressed()) {
            config.setHudScale(Math.min(config.getHudScale() + 0.1f, 2.0f));
            markConfigDirty(now);
        }
        if (KeyBindings.DECREASE_SCALE.wasPressed()) {
            config.setHudScale(Math.max(config.getHudScale() - 0.1f, 0.5f));
            markConfigDirty(now);
        }
        if (KeyBindings.CYCLE_STYLE.wasPressed()) {
            config.setStyleIndex((config.getStyleIndex() + 1) % HudStyle.values().length);
            markConfigDirty(now);
        }
        if (KeyBindings.CYCLE_ANCHOR.wasPressed()) {
            config.setHudAnchor((config.getHudAnchor() + 1) % 4);
            markConfigDirty(now);
        }
        if (KeyBindings.OPEN_CONFIG.wasPressed()) {
            mc.setScreen(new ExcaliaConfigScreen(config));
        }
        // Hide during F2 screenshots
        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_F2)) {
            hideOnScreenshot = true;
            screenshotTimer = SCREENSHOT_HIDE_DURATION_TICKS;
        }
    }

    private void markConfigDirty(long now) {
        configDirty = true;
        lastConfigChangeTime = now;
    }

    private void attemptSaveConfig(long now) {
        if (configDirty && now - lastConfigChangeTime >= CONFIG_SAVE_DEBOUNCE_MS) {
            config.save();
            configDirty = false;
        }
    }

    private void buildLines(VoteService service, long now) {
        if (service.totalVotes == -1) {
            lines.add(MSG_LOADING);
        } else if (service.sites == null) {
            lines.add(MSG_ERROR);
        } else {
            lines.add("Votes ce mois-ci : " + service.totalVotes);
            for (Map.Entry<String, Long> e : service.sites.entrySet()) {
                long rem = e.getValue() - now;
                boolean avail = rem <= 0;
                String timeStr = avail ? MSG_AVAILABLE : String.format("%02d:%02d", rem / 60000, (rem / 1000) % 60);
                lines.add(getSiteName(e.getKey()) + " : " + timeStr);
            }
        }
    }

    private void buildColors(MinecraftClient mc, VoteService service, long now) {
        if (service.totalVotes == -1) {
            colors.add(0xAAAAAA);
        } else if (service.sites == null) {
            colors.add(0xFF5555);
        } else {
            colors.add(config.getAvailableColor());
            for (Map.Entry<String, Long> e : service.sites.entrySet()) {
                long rem = e.getValue() - now;
                boolean avail = rem <= 0;
                boolean wasAvail = voteAvailability.getOrDefault(e.getKey(), false);

                if (avail && !wasAvail) {
                    // son + animation
                    mc.player.playSound(
                            Registries.SOUND_EVENT.get(Identifier.of("minecraft", "entity.experience_orb.pickup")),
                            1.0f, 1.0f);
                    animationTimers.put(e.getKey(), now);

                    // Titre + sous-titre
                    mc.inGameHud.setTitleTicks(10, 70, 20);
                    mc.inGameHud.setTitle(Text.literal(MSG_VOTE_AVAILABLE_TITLE));
                    String site = getSiteName(e.getKey());
                    mc.inGameHud.setSubtitle(Text.literal(site));

                    // Message central optionnel
                    showCenterMessage = true;
                    centerMessageStart = now;
                    centerMessage = MSG_VOTE_AVAILABLE_TITLE + " sur " + site;
                }
                voteAvailability.put(e.getKey(), avail);

                int color;
                HudStyle style = HudStyle.fromIndex(config.getStyleIndex());
                if (style == HudStyle.DEGRADE) {
                    double ratio = Math.max(0, Math.min(1, (double) rem / (2 * 3_600_000)));
                    color = ((int) (255 * ratio) << 16) | ((int) (255 * (1 - ratio)) << 8);
                } else if (style == HudStyle.ARC_EN_CIEL) {
                    float hue = ((now % RAINBOW_CYCLE_MS) / (float) RAINBOW_CYCLE_MS + e.getKey().hashCode()) % 1.0f;
                    color = Color.HSBtoRGB(hue, 1f, 1f);
                } else {
                    color = avail ? config.getAvailableColor() : config.getUnavailableColor();
                }

                if (avail && animationTimers.containsKey(e.getKey())) {
                    long elapsed = now - animationTimers.get(e.getKey());
                    if (elapsed < ANIMATION_DURATION_MS) {
                        float alpha = (float) Math.sin(elapsed / 150.0f * Math.PI);
                        color = ((int) (255 * alpha) << 24) | (color & 0xFFFFFF);
                    } else {
                        animationTimers.remove(e.getKey());
                    }
                }
                colors.add(color);
            }
        }
    }

    private static String getSiteName(String id) {
        return switch (id) {
            case "2" -> "Top Serveurs";
            case "4" -> "Serveurs Minecraft";
            case "5" -> "Serveur-Privé";
            case "6" -> "Serveur-Minecraft-Vote";
            case "7" -> "Serveur-Minecraft.com";
            default -> "Site #" + id;
        };
    }

    // Styles
    public enum HudStyle {
        MINIMALISTE,
        DEGRADE,
        ARC_EN_CIEL,
        COMPACT,
        LISIBLE;

        public static HudStyle fromIndex(int idx) {
            HudStyle[] vals = values();
            return vals[idx % vals.length];
        }
    }

    // -------- Rendering helper --------
    private static class HudRenderer {

        static void render(DrawContext ctx, MinecraftClient mc, ConfigManager config,
                VoteService service, HudStyle style, List<String> lines,
                List<Integer> colors, int sw, int sh,
                int padding, int lineHeight, long now) {
            switch (style) {
                case MINIMALISTE -> renderMinimal(ctx, mc, service, sw, sh, padding, lineHeight, config, now);
                case COMPACT -> renderCompact(ctx, mc, service, sw, sh, padding, lineHeight, config, now);
                case LISIBLE -> renderLisible(ctx, mc, lines, colors, sw, sh, padding, lineHeight);
                case DEGRADE, ARC_EN_CIEL -> renderDefault(ctx, mc, lines, colors, padding, lineHeight);
            }
        }

        private static void renderMinimal(DrawContext ctx, MinecraftClient mc,
                VoteService service, int sw, int sh,
                int padding, int lineHeight, ConfigManager config, long now) {
            long nearestRem = Long.MAX_VALUE;
            if (service.sites != null) {
                for (long resetTime : service.sites.values()) {
                    long rem = resetTime - now;
                    if (rem > 0 && rem < nearestRem)
                        nearestRem = rem;
                }
            }
            String nearestStr = (nearestRem == Long.MAX_VALUE || nearestRem <= 0)
                    ? MSG_AVAILABLE
                    : String.format("%02d:%02d", nearestRem / 60000, (nearestRem / 1000) % 60);
            String total = service.totalVotes == -1 ? "..." : String.valueOf(service.totalVotes);
            String display = total + " | " + nearestStr;

            int textW = mc.textRenderer.getWidth(display);
            int[] pos = computeAnchor(sw, sh, 1.0f, textW, lineHeight, padding, config.getHudAnchor());
            ctx.drawText(mc.textRenderer, Text.literal(display), pos[0], pos[1], 0xFFFFFF, false);
        }

        private static void renderCompact(DrawContext ctx, MinecraftClient mc,
                VoteService service, int sw, int sh,
                int padding, int lineHeight,
                ConfigManager config, long now) {
            long nearestRem = Long.MAX_VALUE;
            String nearestId = null;
            if (service.sites != null) {
                for (Map.Entry<String, Long> e : service.sites.entrySet()) {
                    long rem = e.getValue() - now;
                    if (rem > 0 && rem < nearestRem) {
                        nearestRem = rem;
                        nearestId = e.getKey();
                    }
                }
            }
            String nearestStr = (nearestRem == Long.MAX_VALUE || nearestRem <= 0)
                    ? MSG_AVAILABLE
                    : String.format("%02d:%02d", nearestRem / 60000, (nearestRem / 1000) % 60);
            String total = service.totalVotes == -1 ? "..." : String.valueOf(service.totalVotes);
            String site = (nearestId == null ? "" : getSiteName(nearestId) + ": ");
            String display = "Votes " + total + " | " + site + nearestStr;

            int textW = mc.textRenderer.getWidth(display);
            int[] pos = computeAnchor(sw, sh, 1.0f, textW, lineHeight, padding, config.getHudAnchor());
            ctx.drawText(mc.textRenderer, Text.literal(display), pos[0], pos[1], config.getAvailableColor(), false);
        }

        private static void renderLisible(DrawContext ctx, MinecraftClient mc,
                List<String> lines, List<Integer> colors,
                int sw, int sh, int padding, int lineHeight) {
            int pad2 = padding * 2;
            int w = calcWidth(mc, lines) + pad2;
            int h = lines.size() * lineHeight + pad2;
            int[] pos = computeAnchor(sw, sh, 1.0f, w, h, padding, 0);
            int x = pos[0], y = pos[1];
            ctx.fill(x, y, x + w, y + h, 0xAAFFFFFF);
            ctx.drawBorder(x, y, w, h, 0xFF000000);
            int ty = y + padding;
            for (int i = 0; i < lines.size(); i++) {
                ctx.drawText(mc.textRenderer, Text.literal(lines.get(i)), x + padding, ty, colors.get(i), false);
                ty += lineHeight;
            }
        }

        private static void renderDefault(DrawContext ctx, MinecraftClient mc,
                List<String> lines, List<Integer> colors,
                int padding, int lineHeight) {
            int w = calcWidth(mc, lines) + padding * 2;
            int h = lines.size() * lineHeight + padding * 2;
            int x = padding, y = padding;
            ctx.fill(x - 2, y - 2, x + w, y + h, 0x88000000);
            ctx.drawBorder(x - 2, y - 2, w + 2, h + 2, 0xFFAAAAAA);
            int ty = y + padding;
            for (int i = 0; i < lines.size(); i++) {
                ctx.drawText(mc.textRenderer, Text.literal(lines.get(i)), x + padding, ty, colors.get(i), true);
                ty += lineHeight;
            }
        }

        static void drawCenterMessage(DrawContext ctx, MinecraftClient mc,
                String centerMessage, long startTs,
                boolean visible, long durationMs) {
            if (!visible)
                return;
            long elapsed = System.currentTimeMillis() - startTs;
            if (elapsed < durationMs) {
                int sw = mc.getWindow().getScaledWidth();
                String msg = centerMessage;
                int x0 = (sw - mc.textRenderer.getWidth(msg)) / 2;
                int y0 = mc.getWindow().getScaledHeight() / 2;
                ctx.drawText(mc.textRenderer, Text.literal(msg), x0, y0, 0xFFFF55, true);
            }
        }

        static void drawCompass(DrawContext ctx, MinecraftClient mc, ConfigManager config, long now) {
            int sw = mc.getWindow().getScaledWidth();
            int size = 16, pad = 4;

            // Position: juste au-dessus du prompt de chat (ou bas de l'écran si chat fermé)
            int inputHeight = mc.textRenderer.fontHeight + 4;
            int windowHeight = mc.getWindow().getScaledHeight();
            int baseY = (mc.currentScreen instanceof ChatScreen)
                    ? windowHeight - inputHeight - size - pad
                    : windowHeight - size - pad;

            int x = pad;
            int y = baseY;

            long elapsed = now - config.getTimerStart();
            long cooldownMs = config.getTimerDuration() * 60_000L;
            boolean ready = elapsed >= cooldownMs;

            // Fond + icône
            int bgColor = ready ? 0xFF00AA00 : 0xAAFF0000;
            ctx.fill(x, y, x + size, y + size, bgColor);
            ctx.drawItem(new ItemStack(Items.COMPASS), x, y);

            // Texte du cooldown au-dessus de l'icône
            if (!ready) {
                long remMs = Math.max(cooldownMs - elapsed, 0);
                String remStr = String.format("%02d:%02d", remMs / 60000, (remMs / 1000) % 60);
                int textW = mc.textRenderer.getWidth(remStr);
                ctx.getMatrices().push();
                // remonter en Z pour forcer l'ordre de rendu
                ctx.getMatrices().translate(0, 0, 300);
                ctx.drawTextWithShadow(
                        mc.textRenderer,
                        remStr,
                        x + (size - textW) / 2,
                        y + (size - mc.textRenderer.fontHeight) / 2,
                        0xFFFFFFFF);
                ctx.getMatrices().pop();
            }

            // Hover + tooltip
            int mx = (int) (mc.mouse.getX() * sw / mc.getWindow().getWidth());
            int my = (int) (mc.mouse.getY() * windowHeight / mc.getWindow().getHeight());
            boolean hovered = mx >= x && mx <= x + size && my >= y && my <= y + size;
            if (hovered) {
                String tip = ready ? "Envoyer message de vote" : "En cooldown";
                ctx.drawTextWithShadow(mc.textRenderer, tip, x, y - mc.textRenderer.fontHeight - 2, 0xFFFFFF);
            }

            // Clic: seulement si chat ouvert + prêt + sur la zone
            boolean md = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                    GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
            if (md && !wasMouseDown && ready && hovered && mc.currentScreen instanceof ChatScreen) {
                String msg = config.getAutoMessage();
                if (msg != null && !msg.isBlank()) {
                    mc.player.networkHandler.sendChatMessage(msg);
                }
                config.setTimerActive(true);
                config.setTimerStart(now);
            }
            wasMouseDown = md;
        }

        private static int calcWidth(MinecraftClient mc, List<String> lines) {
            return lines.stream().mapToInt(mc.textRenderer::getWidth).max().orElse(100);
        }

        private static int[] computeAnchor(int sw, int sh, float scale, int w, int h, int padding, int anchor) {
            int wS = (int) (w * scale);
            int hS = (int) (h * scale);
            return switch (anchor) {
                case 1 -> new int[] { (int) ((sw - wS - padding) / scale), padding };
                case 2 -> new int[] { padding, (int) ((sh - hS - padding) / scale) };
                case 3 -> new int[] { (int) ((sw - wS - padding) / scale), (int) ((sh - hS - padding) / scale) };
                default -> new int[] { padding, padding };
            };
        }
    }
}