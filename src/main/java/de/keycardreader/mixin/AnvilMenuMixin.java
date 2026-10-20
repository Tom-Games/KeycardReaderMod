package de.keycardreader.mixin;

import de.keycardreader.KeycardReaderMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    private int cost;

    @Shadow
    private net.minecraft.world.Container inputSlots;

    @Shadow
    private net.minecraft.world.Container resultSlots;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void keycardreader$requireKeyCardTag(CallbackInfo ci) {
        ItemStack input = inputSlots.getItem(0);

        if (input.isEmpty() || !input.is(Items.PAPER)) {
            return;
        }

        CustomData custom = input.get(DataComponents.CUSTOM_DATA);
        boolean tagged = false;

        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            tagged = tag.getBoolean(KeycardReaderMod.CARD_TAG).orElse(false);
        }

        if (!tagged) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            cost = 0;
        }
    }
}
