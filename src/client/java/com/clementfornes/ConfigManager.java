package com.clementfornes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    // JSON keys
    private static final String HUD_SCALE_KEY = "hudScale";
    private static final String STYLE_INDEX_KEY = "styleIndex";
    private static final String HUD_ANCHOR_KEY = "hudAnchor";
    private static final String AVAILABLE_COLOR_KEY = "availableColor";
    private static final String UNAVAILABLE_COLOR_KEY = "unavailableColor";
    private static final String AUTO_MESSAGE_KEY = "autoMessage";
    private static final String TIMER_ACTIVE_KEY = "timerActive";
    private static final String TIMER_START_KEY = "timerStart";
    private static final String TIMER_DURATION_KEY = "timerDuration";

    private final Path path;

    // HUD
    private float hudScale = 0.6f; // 0.5 – 2.0
    private int styleIndex = 0; // enum index
    private int hudAnchor = 0; // 0 - 3 (top-left, top-right, bottom-left, bottom-right)
    private int availableColor = 0x00FF88;
    private int unavailableColor = 0xFFFFFF;

    // Chat
    private String autoMessage = "";

    // Timer
    private boolean timerActive = false;
    private long timerStart = 0L; // epoch ms
    private int timerDuration = 20; // minutes

    private final Logger logger;

    public ConfigManager(Path configPath) {
        this.path = configPath;
        this.logger = LogUtils.getLogger();
    }

    public void load() {
        try {
            ensureParentDir();
        } catch (IOException e) {
            logger.error("[ExcaliaVoteMod] Failed to create config directory: {}", e.getMessage());
            return;
        }

        if (!Files.exists(path)) {
            logger.warn("[ExcaliaVoteMod] Config not found, using default values");
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            hudScale = getFloat(obj, HUD_SCALE_KEY, hudScale);
            styleIndex = getInt(obj, STYLE_INDEX_KEY, styleIndex);
            hudAnchor = getInt(obj, HUD_ANCHOR_KEY, hudAnchor);
            availableColor = getInt(obj, AVAILABLE_COLOR_KEY, availableColor);
            unavailableColor = getInt(obj, UNAVAILABLE_COLOR_KEY, unavailableColor);
            autoMessage = getString(obj, AUTO_MESSAGE_KEY, autoMessage);

            timerActive = getBoolean(obj, TIMER_ACTIVE_KEY, timerActive);
            timerStart = getLong(obj, TIMER_START_KEY, timerStart);
            timerDuration = getInt(obj, TIMER_DURATION_KEY, timerDuration);
        } catch (IOException | JsonParseException e) {
            logger.warn("[ExcaliaVoteMod] Error loading config, using default values", e);
        }
    }

    public void save() {
        try {
            ensureParentDir();
        } catch (IOException e) {
            logger.error("[ExcaliaVoteMod] Failed to create config directory: {}", e.getMessage());
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty(HUD_SCALE_KEY, hudScale);
        obj.addProperty(STYLE_INDEX_KEY, styleIndex);
        obj.addProperty(HUD_ANCHOR_KEY, hudAnchor);
        obj.addProperty(AVAILABLE_COLOR_KEY, availableColor);
        obj.addProperty(UNAVAILABLE_COLOR_KEY, unavailableColor);
        obj.addProperty(AUTO_MESSAGE_KEY, autoMessage);

        obj.addProperty(TIMER_ACTIVE_KEY, timerActive);
        obj.addProperty(TIMER_START_KEY, timerStart);
        obj.addProperty(TIMER_DURATION_KEY, timerDuration);

        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(obj.toString());
        } catch (IOException e) {
            logger.error("[ExcaliaVoteMod] Failed to save config: {}", e.getMessage());
        }
    }

    private void ensureParentDir() throws IOException {
        Files.createDirectories(path.getParent());
    }

    // ---- JSON helpers ------------------------------------------------------

    private float getFloat(JsonObject obj, String key, float def) {
        try {
            return obj.has(key) ? obj.get(key).getAsFloat() : def;
        } catch (Exception ignore) {
            return def;
        }
    }

    private int getInt(JsonObject obj, String key, int def) {
        try {
            return obj.has(key) ? obj.get(key).getAsInt() : def;
        } catch (Exception ignore) {
            return def;
        }
    }

    private long getLong(JsonObject obj, String key, long def) {
        try {
            return obj.has(key) ? obj.get(key).getAsLong() : def;
        } catch (Exception ignore) {
            return def;
        }
    }

    private boolean getBoolean(JsonObject obj, String key, boolean def) {
        try {
            return obj.has(key) ? obj.get(key).getAsBoolean() : def;
        } catch (Exception ignore) {
            return def;
        }
    }

    private String getString(JsonObject obj, String key, String def) {
        try {
            return obj.has(key) ? obj.get(key).getAsString() : def;
        } catch (Exception ignore) {
            return def;
        }
    }

    // ---- Getters / Setters -------------------------------------------------

    public float getHudScale() {
        return hudScale;
    }

    public void setHudScale(float hudScale) {
        this.hudScale = hudScale;
    }

    public int getStyleIndex() {
        return styleIndex;
    }

    public void setStyleIndex(int styleIndex) {
        this.styleIndex = styleIndex;
    }

    public int getHudAnchor() {
        return hudAnchor;
    }

    public void setHudAnchor(int hudAnchor) {
        this.hudAnchor = hudAnchor;
    }

    public int getAvailableColor() {
        return availableColor;
    }

    public void setAvailableColor(int availableColor) {
        this.availableColor = availableColor;
    }

    public int getUnavailableColor() {
        return unavailableColor;
    }

    public void setUnavailableColor(int unavailableColor) {
        this.unavailableColor = unavailableColor;
    }

    public String getAutoMessage() {
        return autoMessage;
    }

    public void setAutoMessage(String autoMessage) {
        this.autoMessage = autoMessage;
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    public void setTimerActive(boolean timerActive) {
        this.timerActive = timerActive;
    }

    public long getTimerStart() {
        return timerStart;
    }

    public void setTimerStart(long timerStart) {
        this.timerStart = timerStart;
    }

    public int getTimerDuration() {
        return timerDuration;
    }

    public void setTimerDuration(int timerDuration) {
        this.timerDuration = timerDuration;
    }
}