package de.keycardreader.mixin;

import de.keycardreader.KeycardReaderMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Set;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    private static final Set<String> KEYCARD_NAMES = Set.of(
            "KC-B-B", "KC-B-G", "KC-B-P", "KC-B-R", "KC-B-Y",
            "KC-W-B", "KC-W-G", "KC-W-P", "KC-W-R", "KC-W-Y"
    );

    @Inject(method = "createResult", at = @At("TAIL"))
    private void keycardreader$handleKeycards(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack input = menu.getSlot(0).getItem();
        ItemStack output = menu.getSlot(2).getItem();

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
            menu.getSlot(2).set(ItemStack.EMPTY);
            return;
        }

        if (output.isEmpty()) {
            return;
        }

        String name = output.has(DataComponents.CUSTOM_NAME)
                ? output.get(DataComponents.CUSTOM_NAME).getString()
                : "";

        if (KEYCARD_NAMES.contains(name)) {
            output.set(
                    DataComponents.ITEM_MODEL,
                    Identifier.fromNamespaceAndPath(
                            "keycardreader",
                            name.toLowerCase(Locale.ROOT)
                    )
            );
        } else {
            // Restore vanilla paper rendering for names that are not keycard names.
            output.remove(DataComponents.ITEM_MODEL);
        }
    }
}
