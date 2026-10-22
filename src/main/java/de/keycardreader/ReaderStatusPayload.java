package de.keycardreader;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: alle Reader in der Nähe und welche Keycards (Codes) im Trichter darunter liegen.
 * Der Server entscheidet, was ein Reader ist; der Client zeichnet nur.
 */
public record ReaderStatusPayload(List<Entry> readers) implements CustomPacketPayload {

    /** Ein Reader: Position + bis zu 2 Kartencodes aus dem Trichter darunter. */
    public record Entry(BlockPos pos, List<String> codes) {}

    public static final CustomPacketPayload.Type<ReaderStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("keycardreader", "reader_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReaderStatusPayload> CODEC = new StreamCodec<>() {
        @Override
        public ReaderStatusPayload decode(RegistryFriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Entry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
                int c = buf.readVarInt();
                List<String> codes = new ArrayList<>(c);
                for (int j = 0; j < c; j++) codes.add(buf.readUtf(32));
                list.add(new Entry(pos, codes));
            }
            return new ReaderStatusPayload(list);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ReaderStatusPayload value) {
            buf.writeVarInt(value.readers().size());
            for (Entry e : value.readers()) {
                buf.writeInt(e.pos().getX());
                buf.writeInt(e.pos().getY());
                buf.writeInt(e.pos().getZ());
                buf.writeVarInt(e.codes().size());
                for (String code : e.codes()) buf.writeUtf(code, 32);
            }
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
