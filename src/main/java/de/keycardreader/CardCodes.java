package de.keycardreader;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kartencodes: Preset (KC-B-B ... KC-W-Y) oder frei gewählte Farben per Hex:
 * KC-#KARTE-#STREIFEN, z. B. KC-#222222-#FF8800.
 */
public final class CardCodes {
    private static final Pattern HEX = Pattern.compile("KC-#([0-9A-F]{6})-#([0-9A-F]{6})");

    private CardCodes() {}

    public static boolean isHex(String code) {
        return code != null && HEX.matcher(code).matches();
    }

    public static boolean isValid(String code) {
        return code != null && (KeycardReaderMod.CARD_MODELS.containsKey(code) || isHex(code));
    }

    public static String hexCode(String body, String stripe) {
        return "KC-#" + body.toUpperCase() + "-#" + stripe.toUpperCase();
    }

    private static int group(String code, int g) {
        Matcher m = HEX.matcher(code);
        return m.matches() ? Integer.parseInt(m.group(g), 16) : 0x808080;
    }

    /** Setzt Modell (bei Hex-Karten zusätzlich beide Farben) auf den Stack. */
    public static void applyVisual(ItemStack stack, String code) {
        if (isHex(code)) {
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("keycardreader", "kc-hex"));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of(), List.of(), List.of(), List.of(group(code, 1), group(code, 2))));
        } else {
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                    "keycardreader", KeycardReaderMod.CARD_MODELS.get(code)));
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    public static void removeVisual(ItemStack stack) {
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("minecraft", "paper"));
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    /** Icon für Menüs mit beliebigen Farben. */
    public static ItemStack hexIcon(int body, int stripe) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("keycardreader", "kc-hex"));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(), List.of(), List.of(), List.of(body, stripe)));
        return stack;
    }

    /** Namensfarbe: Preset-Farbe bzw. bei Hex die Streifenfarbe. */
    public static Style nameStyle(String code) {
        if (isHex(code)) return Style.EMPTY.withColor(TextColor.fromRgb(group(code, 2)));
        return Style.EMPTY.withColor(switch (code.charAt(code.length() - 1)) {
            case 'B' -> ChatFormatting.DARK_BLUE;
            case 'G' -> ChatFormatting.DARK_GREEN;
            case 'P' -> ChatFormatting.DARK_PURPLE;
            case 'R' -> ChatFormatting.DARK_RED;
            case 'Y' -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        });
    }
}
