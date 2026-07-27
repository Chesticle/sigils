package com.sigils.client.draft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.sigils.core.draft.DraftLimits;
import com.sigils.core.geometry.Vec2;
import com.sigils.core.glyph.GlyphInstance;

/**
 * Saves an arrangement — glyph ids, positions, rotations, scales — to the game
 * directory. Deliberately not the traces: a template is a design, and the
 * drawing of it is the part the player is supposed to earn each time.
 */
public final class DraftTemplates {

    private DraftTemplates() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("sigils").resolve("templates").resolve("last.json");
    }

    public static void save(List<GlyphInstance> placements) {
        JsonArray array = new JsonArray();
        for (GlyphInstance placement : placements) {
            JsonObject entry = new JsonObject();
            entry.addProperty("glyph", placement.glyphId());
            entry.addProperty("x", placement.position().x());
            entry.addProperty("y", placement.position().y());
            entry.addProperty("rotation", placement.rotation());
            entry.addProperty("scale", placement.scale());
            array.add(entry);
        }
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, array.toString());
        } catch (Exception e) {
            // A template is a convenience; failing to save one is not worth a crash.
        }
    }

    public static List<GlyphInstance> loadLast() {
        List<GlyphInstance> placements = new ArrayList<>();
        try {
            Path path = file();
            if (!Files.exists(path)) {
                return placements;
            }
            JsonArray array = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
            for (JsonElement element : array) {
                if (placements.size() >= DraftLimits.HARD_MAX_GLYPHS) {
                    break; // a hand-edited file is still untrusted input
                }
                JsonObject entry = element.getAsJsonObject();
                placements.add(new GlyphInstance(
                        entry.get("glyph").getAsString(),
                        new Vec2(entry.get("x").getAsFloat(), entry.get("y").getAsFloat()),
                        entry.get("rotation").getAsFloat(),
                        entry.get("scale").getAsFloat()));
            }
        } catch (Exception e) {
            return List.of();
        }
        return placements;
    }
}