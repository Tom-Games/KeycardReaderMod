package de.keycardreader;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> Server: Auswahl aus dem Erstellmenü (Server prüft alles erneut). */
public record CardifyRequestPayload(String code, String name, String serial) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardifyRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("keycardreader", "cardify_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardifyRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), CardifyRequestPayload::code,
            ByteBufCodecs.stringUtf8(128), CardifyRequestPayload::name,
            ByteBufCodecs.stringUtf8(128), CardifyRequestPayload::serial,
            CardifyRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
