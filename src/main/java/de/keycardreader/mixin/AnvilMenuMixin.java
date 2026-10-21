package de.keycardreader.mixin;

import de.keycardreader.KeycardReaderMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void keycardreader$requireKeyCardTag(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack input = menu.getSlot(0).getItem();

        if (input.isEmpty() || !input.is(Items.PAPER)) {
            return;
        }

        CustomData custom = input.get(DataComponents.CUSTOM_DATA);
        boolean hasKeycardTag = false;
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            // Check only that the KeyCard key exists; do not compare its value.
            hasKeycardTag = tag.contains(KeycardReaderMod.CARD_TAG);
        }

        if (!hasKeycardTag) {
            menu.getSlot(2).set(ItemStack.EMPTY);
        }
    }
}
