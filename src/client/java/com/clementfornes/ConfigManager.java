package com.clementfornes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private final Path path;
    private float hudScale = 0.6f;
    private int styleIndex = 0;
    private int hudAnchor = 0;
    private final Logger logger;

    public ConfigManager(Path configPath) {
        this.path = configPath;
        this.logger = LogUtils.getLogger();
    }

    public void load() {
        if (!Files.exists(path)) {
            logger.warn("[ExcaliaVoteMod] Config file not found, using defaults");
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            hudScale = obj.get("hudScale").getAsFloat();
            styleIndex = obj.get("styleIndex").getAsInt();
            hudAnchor = obj.get("hudAnchor").getAsInt();
        } catch (Exception e) {
            logger.warn("[ExcaliaVoteMod] Failed to load config, using defaults", e);
        }
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("hudScale", hudScale);
        obj.addProperty("styleIndex", styleIndex);
        obj.addProperty("hudAnchor", hudAnchor);
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(obj.toString());
        } catch (Exception e) {
            logger.error("[ExcaliaVoteMod] Failed to save config", e);
        }
    }

    public float getHudScale() {
        return hudScale;
    }

    public void setHudScale(float scale) {
        this.hudScale = scale;
    }

    public int getStyleIndex() {
        return styleIndex;
    }

    public void setStyleIndex(int index) {
        this.styleIndex = index;
    }

    public int getHudAnchor() {
        return hudAnchor;
    }

    public void setHudAnchor(int anchor) {
        this.hudAnchor = anchor;
    }
}
