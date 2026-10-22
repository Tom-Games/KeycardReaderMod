package de.keycardreader.mixin;

import de.keycardreader.CardCodes;
import de.keycardreader.KeycardReaderMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    private static final Map<Character, ChatFormatting> CARD_COLORS = Map.of(
        'B', ChatFormatting.DARK_BLUE,
        'G', ChatFormatting.DARK_GREEN,
        'P', ChatFormatting.DARK_PURPLE,
        'R', ChatFormatting.DARK_RED,
        'Y', ChatFormatting.GOLD
    );

    @Inject(method = "createResult", at = @At("TAIL"))
    private void keycardreader$validateAndColorKeycard(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack input = menu.getSlot(0).getItem();
        ItemStack result = menu.getSlot(2).getItem();

        if (input.isEmpty() || !input.is(Items.PAPER)) return;

        String code = null;
        String serial = null;
        CustomData custom = input.get(DataComponents.CUSTOM_DATA);
        CompoundTag protectedTag = null;
        if (custom != null) {
            protectedTag = custom.copyTag();
            if (protectedTag.contains(KeycardReaderMod.CARD_TAG)) {
                code = protectedTag.getString(KeycardReaderMod.CARD_TAG).orElse("");
            }
            if (protectedTag.contains(KeycardReaderMod.CARD_SERIAL_TAG)) {
                serial = protectedTag.getString(KeycardReaderMod.CARD_SERIAL_TAG).orElse("");
            }
        }

        // Both values are mandatory: the model code chooses appearance; serial distinguishes copies.
        if (code == null || !CardCodes.isValid(code)
                || serial == null || serial.length() < 4 || serial.length() > 64
                || !serial.matches("[A-Za-z0-9_-]+")) {
            menu.getSlot(2).set(ItemStack.EMPTY);
            return;
        }
        if (result.isEmpty()) return;

        // Re-assert both model and full identity on the anvil result. CustomData differs between
        // cards with different serials, so Minecraft's normal stack comparison keeps them separate.
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(protectedTag));
        CardCodes.applyVisual(result, code);

        // Name behalten, Farbe immer passend zur Karte erzwingen.
        Component currentName = result.get(DataComponents.CUSTOM_NAME);
        if (currentName == null) currentName = Component.literal(code);
        result.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(CardCodes.nameStyle(code)));
    }
}
