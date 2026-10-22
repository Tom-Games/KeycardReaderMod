package de.keycardreader;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server -> Client: welche Keycards liegen im Trichter unter dem geöffneten Reader?
 * active = false bedeutet: der geöffnete Dropper ist kein Reader (Anzeige ausblenden).
 */
public record ReaderPayload(boolean active, List<ItemStack> cards) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReaderPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("keycardreader", "reader_cards"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReaderPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ReaderPayload::active,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, ReaderPayload::cards,
            ReaderPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
