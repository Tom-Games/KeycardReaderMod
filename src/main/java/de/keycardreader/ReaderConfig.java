package de.keycardreader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-seitige Config: config/keycardreader.json
 * {
 *   "readerNames": ["KeyCard einlegen", "Insert KeyCard"],
 *   "sound": true
 * }
 * Jeder Dropper, der einen dieser Namen trägt (Groß-/Kleinschreibung egal), ist ein Reader.
 */
public final class ReaderConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("keycardreader");
    private static final List<String> DEFAULT_NAMES = List.of("KeyCard einlegen", "Insert KeyCard");

    private static volatile List<String> names = lowerAll(DEFAULT_NAMES);
    private static volatile boolean sound = true;

    private ReaderConfig() {}

    public static boolean isReaderName(String customName) {
        if (customName == null) return false;
        return names.contains(customName.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean soundEnabled() {
        return sound;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("keycardreader.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(file)) {
                JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (root.has("readerNames") && root.get("readerNames").isJsonArray()) {
                    List<String> loaded = new ArrayList<>();
                    for (JsonElement e : root.getAsJsonArray("readerNames")) {
                        String s = e.getAsString().trim();
                        if (!s.isEmpty()) loaded.add(s);
                    }
                    if (!loaded.isEmpty()) names = lowerAll(loaded);
                }
                if (root.has("sound")) sound = root.get("sound").getAsBoolean();
            } else {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                DEFAULT_NAMES.forEach(arr::add);
                root.add("readerNames", arr);
                root.addProperty("sound", true);
                Files.createDirectories(file.getParent());
                Files.writeString(file, gson.toJson(root));
            }
        } catch (Exception e) {
            LOGGER.warn("keycardreader.json konnte nicht gelesen werden, Standardwerte werden benutzt", e);
        }
    }

    private static List<String> lowerAll(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) out.add(s.trim().toLowerCase(Locale.ROOT));
        return List.copyOf(out);
    }
}
