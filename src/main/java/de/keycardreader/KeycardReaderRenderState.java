package de.keycardreader;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.List;

final class KeycardReaderRenderState extends BlockEntityRenderState {
    boolean isReader;
    /** Lampe an = mindestens eine gültige Karte im Trichter darunter. */
    boolean lit;
    /** Bis zu 2 Kartencodes, die außen am Reader angezeigt werden. */
    List<String> cards = List.of();
    Direction facing = Direction.NORTH;
}
