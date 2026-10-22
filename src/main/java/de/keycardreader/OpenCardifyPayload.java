package de.keycardreader;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> Client: öffnet das Keycard-Erstellmenü (nach /cardify). */
public record OpenCardifyPayload() implements CustomPacketPayload {

    public static final OpenCardifyPayload INSTANCE = new OpenCardifyPayload();

    public static final CustomPacketPayload.Type<OpenCardifyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("keycardreader", "open_cardify"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCardifyPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
