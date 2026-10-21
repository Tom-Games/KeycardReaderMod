package de.keycardreader.mixin;

import de.keycardreader.KeycardReaderMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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
        CustomData custom = input.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            if (tag.contains(KeycardReaderMod.CARD_TAG)) {
                code = tag.getString(KeycardReaderMod.CARD_TAG).orElse("");
            }
        }

        // Plain paper cannot be renamed into a keycard.
        if (code == null || !KeycardReaderMod.CARD_MODELS.containsKey(code)) {
            menu.getSlot(2).set(ItemStack.EMPTY);
            return;
        }
        if (result.isEmpty()) return;

        // Re-assert the model from the protected tag, regardless of the anvil name.
        result.set(DataComponents.ITEM_MODEL,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "keycardreader", KeycardReaderMod.CARD_MODELS.get(code)));

        // Keep the player's chosen anvil name, but force its color to match the card's color.
        char colorCode = code.charAt(code.length() - 1);
        ChatFormatting color = CARD_COLORS.getOrDefault(colorCode, ChatFormatting.WHITE);
        Component currentName = result.get(DataComponents.CUSTOM_NAME);
        if (currentName == null) currentName = Component.literal(code);
        result.set(DataComponents.CUSTOM_NAME,
                currentName.copy().withStyle(Style.EMPTY.withColor(color)));
    }
}
