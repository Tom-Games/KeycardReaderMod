package de.keycardreader;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-Speicher: welche Dropper sind Reader und welche Karten liegen darunter (vom Server geschickt). */
public final class ReaderStatusClient {
    private static volatile Map<Long, List<String>> readers = Map.of();

    private ReaderStatusClient() {}

    public static void update(ReaderStatusPayload payload) {
        Map<Long, List<String>> map = new HashMap<>();
        for (ReaderStatusPayload.Entry e : payload.readers()) {
            map.put(e.pos().asLong(), e.codes());
        }
        readers = map;
    }

    public static void clear() {
        readers = Map.of();
    }

    /** null = kein Reader; leere Liste = Reader ohne Karte; sonst die Kartencodes. */
    public static List<String> get(BlockPos pos) {
        return readers.get(pos.asLong());
    }
}
