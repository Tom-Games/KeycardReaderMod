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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alles rund um Kartencodes.
 * Preset-Codes:  KC-B-B ... KC-W-Y  (Schwarz/Weiß x Blau, Grün, Gelb, Rot, Lila)
 * Hex-Codes:     KC-B-#RRGGBB oder KC-W-#RRGGBB (frei wählbare Farbe)
 */
public final class CardCodes {
    private static final Pattern HEX = Pattern.compile("KC-([BW])-#([0-9A-F]{6})");

    public static final Map<String, String> PRESET_MODELS = Map.ofEntries(
        Map.entry("KC-B-B", "kc-b-b"), Map.entry("KC-B-G", "kc-b-g"),
        Map.entry("KC-B-P", "kc-b-p"), Map.entry("KC-B-R", "kc-b-r"),
        Map.entry("KC-B-Y", "kc-b-y"), Map.entry("KC-W-B", "kc-w-b"),
        Map.entry("KC-W-G", "kc-w-g"), Map.entry("KC-W-P", "kc-w-p"),
        Map.entry("KC-W-R", "kc-w-r"), Map.entry("KC-W-Y", "kc-w-y")
    );

    private CardCodes() {}

    public static boolean isHex(String code) {
        return code != null && HEX.matcher(code).matches();
    }

    public static boolean isValid(String code) {
        return code != null && (PRESET_MODELS.containsKey(code) || isHex(code));
    }

    /** Modellname (ohne Namespace) für den Code oder null. */
    public static String modelOf(String code) {
        if (code == null) return null;
        String preset = PRESET_MODELS.get(code);
        if (preset != null) return preset;
        Matcher m = HEX.matcher(code);
        if (m.matches()) return "B".equals(m.group(1)) ? "kc-hex-b" : "kc-hex-w";
        return null;
    }

    /** RGB-Wert einer Hex-Karte (ohne Alpha). Nur aufrufen, wenn isHex(code). */
    public static int rgbOf(String code) {
        Matcher m = HEX.matcher(code);
        if (!m.matches()) return 0x808080;
        return Integer.parseInt(m.group(2), 16);
    }

    /** Setzt Modell (und bei Hex-Karten die Farbe) auf den Stack. */
    public static void applyVisual(ItemStack stack, String code) {
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("keycardreader", modelOf(code)));
        if (isHex(code)) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(), List.of(), List.of(rgbOf(code))));
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    /** Entfernt alles, was applyVisual gesetzt hat (für /discardify). */
    public static void removeVisual(ItemStack stack) {
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("minecraft", "paper"));
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    /** Icon-Stack für Menüs: Hex-Karte mit beliebiger Farbe. */
    public static ItemStack iconStack(boolean black, int rgb) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("keycardreader", black ? "kc-hex-b" : "kc-hex-w"));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(rgb)));
        return stack;
    }

    /** Icon-Stack für einen gültigen Code. */
    public static ItemStack iconStack(String code) {
        ItemStack stack = new ItemStack(Items.PAPER);
        applyVisual(stack, code);
        return stack;
    }

    /** Namensfarbe: Preset-Farbe bzw. die gewählte Hex-Farbe. */
    public static Style nameStyle(String code) {
        if (isHex(code)) return Style.EMPTY.withColor(TextColor.fromRgb(rgbOf(code)));
        if (code == null || code.isEmpty()) return Style.EMPTY.withColor(ChatFormatting.WHITE);
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
