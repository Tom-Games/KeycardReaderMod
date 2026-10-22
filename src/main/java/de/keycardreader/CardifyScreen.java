package de.keycardreader;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Menü von /cardify: Kartenname (mit Live-Vorschau), Farbe per Klick auf eine Karte
 * oder eigene Hex-Farbe, Spezialcode.
 */
public final class CardifyScreen extends Screen {
    // Reihenfolge der Farbspalten: Blue, Green, Yellow, Red, Purple, danach "Eigene Farbe" (Hex)
    private static final String[] COLOR_LETTERS = {"B", "G", "Y", "R", "P"};
    private static final String[] COLOR_NAMES = {"Blau", "Grün", "Gelb", "Rot", "Lila"};
    private static final String[] ROW_LETTERS = {"B", "W"};
    private static final String[] ROW_NAMES = {"Schwarz", "Weiß"};
    private static final int CUSTOM_COL = 5;

    private static final int CONTENT_W = 208;
    private static final int BTN = 22;
    private static final int STEP = 26;

    // vertikales Layout (relativ zu "top")
    private static final int PREVIEW_Y = 16;
    private static final int NAME_LABEL_Y = 35;
    private static final int NAME_BOX_Y = 45;
    private static final int COLOR_LABEL_Y = 70;
    private static final int ROW0_Y = 83;
    private static final int HEX_LABEL_Y = 136;
    private static final int HEX_BOX_Y = 146;
    private static final int SERIAL_LABEL_Y = 171;
    private static final int SERIAL_BOX_Y = 181;
    private static final int BUTTONS_Y = 209;
    private static final int PANEL_H = 229;

    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private int selRow = 0;
    private int selCol = 0;
    private String nameValue = "";
    private String hexValue = "";
    private String serialValue = "";

    private EditBox nameBox;
    private EditBox hexBox;
    private EditBox serialBox;
    private Button createButton;
    private int left;
    private int top;

    public CardifyScreen() {
        super(Component.literal("Keycard erstellen"));
    }

    /** Einmal beim Client-Start aufrufen: zeichnet Texte, Karten-Icons und Auswahlrahmen. */
    public static void registerHooks() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof CardifyScreen cardify) {
                ScreenEvents.afterExtract(screen).register(
                        (s, g, mouseX, mouseY, delta) -> cardify.drawOverlay(g));
            }
        });
    }

    private static boolean serialValid(String s) {
        return s.length() >= 4 && s.length() <= 51 && s.matches("[A-Za-z0-9_-]+");
    }

    private String hexDigits() {
        return hexValue.startsWith("#") ? hexValue.substring(1) : hexValue;
    }

    private boolean hexValid() {
        return hexDigits().matches("[0-9A-Fa-f]{6}");
    }

    /** Aktuell gewählter Code oder null (eigene Farbe gewählt, Hex aber noch ungültig). */
    private String currentCode() {
        String row = ROW_LETTERS[selRow];
        if (selCol == CUSTOM_COL) {
            return hexValid() ? "KC-" + row + "-#" + hexDigits().toUpperCase(Locale.ROOT) : null;
        }
        return "KC-" + row + "-" + COLOR_LETTERS[selCol];
    }

    private ItemStack iconFor(int row, int col) {
        if (col == CUSTOM_COL) {
            int rgb = hexValid() ? Integer.parseInt(hexDigits(), 16) : 0x808080;
            return CardCodes.iconStack(row == 0, rgb);
        }
        String code = "KC-" + ROW_LETTERS[row] + "-" + COLOR_LETTERS[col];
        return ICONS.computeIfAbsent(code, CardCodes::iconStack);
    }

    @Override
    protected void init() {
        Font font = Minecraft.getInstance().font;
        left = this.width / 2 - CONTENT_W / 2;
        top = Math.max(2, (this.height - PANEL_H) / 2);

        nameBox = new EditBox(font, left, top + NAME_BOX_Y, CONTENT_W, 20, Component.literal("Kartenname"));
        nameBox.setMaxLength(50);
        nameBox.setValue(nameValue);
        nameBox.setResponder(v -> nameValue = v);
        addRenderableWidget(nameBox);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col <= CUSTOM_COL; col++) {
                final int r = row;
                final int c = col;
                Button b = Button.builder(Component.empty(), btn -> { selRow = r; selCol = c; updateCreateState(); })
                        .bounds(buttonX(col), buttonY(row), BTN, BTN)
                        .build();
                String tip = col == CUSTOM_COL
                        ? ROW_NAMES[row] + " – Eigene Farbe (Hex)"
                        : ROW_NAMES[row] + " – " + COLOR_NAMES[col] + " (KC-" + ROW_LETTERS[row] + "-" + COLOR_LETTERS[col] + ")";
                b.setTooltip(Tooltip.create(Component.literal(tip)));
                addRenderableWidget(b);
            }
        }

        hexBox = new EditBox(font, left, top + HEX_BOX_Y, 100, 20, Component.literal("Hex-Farbe"));
        hexBox.setMaxLength(7);
        hexBox.setValue(hexValue);
        hexBox.setResponder(v -> {
            // Nur # und Hex-Zeichen zulassen (ersetzt den entfernten EditBox-Filter)
            String cleaned = v.replaceAll("[^#0-9A-Fa-f]", "");
            if (!cleaned.equals(v)) {
                hexBox.setValue(cleaned); // ruft diesen Responder erneut mit bereinigtem Wert auf
                return;
            }
            hexValue = v;
            if (hexValid()) selCol = CUSTOM_COL; // gültiger Hex-Wert wählt automatisch "Eigene Farbe"
            updateCreateState();
        });
        addRenderableWidget(hexBox);

        serialBox = new EditBox(font, left, top + SERIAL_BOX_Y, CONTENT_W, 20, Component.literal("Spezialcode"));
        serialBox.setMaxLength(51);
        serialBox.setValue(serialValue);
        serialBox.setResponder(v -> {
            serialValue = v;
            updateCreateState();
        });
        addRenderableWidget(serialBox);

        createButton = Button.builder(Component.literal("Erstellen"), b -> submit())
                .bounds(left, top + BUTTONS_Y, 100, 20).build();
        addRenderableWidget(createButton);
        addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> onClose())
                .bounds(left + CONTENT_W - 100, top + BUTTONS_Y, 100, 20).build());
        updateCreateState();
    }

    private int buttonX(int col) { return left + 52 + col * STEP; }
    private int buttonY(int row) { return top + ROW0_Y + row * STEP; }

    private void updateCreateState() {
        if (createButton != null) createButton.active = serialValid(serialValue) && currentCode() != null;
    }

    private void submit() {
        String code = currentCode();
        if (!serialValid(serialValue) || code == null) return;
        ClientPlayNetworking.send(new CardifyRequestPayload(code, nameValue, serialValue));
        onClose();
    }

    private void drawOverlay(GuiGraphicsExtractor g) {
        Font font = Minecraft.getInstance().font;
        String title = "Keycard erstellen";
        g.text(font, title, this.width / 2 - font.width(title) / 2, top + 4, 0xFFFFFFFF, true);

        drawPreview(g, font);

        g.text(font, "Kartenname", left, top + NAME_LABEL_Y, 0xFFAAAAAA, false);
        g.text(font, "Farbe (zum Auswählen anklicken)", left, top + COLOR_LABEL_Y, 0xFFAAAAAA, false);

        for (int row = 0; row < 2; row++) {
            g.text(font, ROW_NAMES[row], left, buttonY(row) + 7, 0xFFFFFFFF, false);
            for (int col = 0; col <= CUSTOM_COL; col++) {
                int bx = buttonX(col);
                int by = buttonY(row);
                g.item(iconFor(row, col), bx + 3, by + 3);
                if (row == selRow && col == selCol) {
                    int c = 0xFFFFFF55;
                    g.fill(bx - 2, by - 2, bx + BTN + 2, by - 1, c);
                    g.fill(bx - 2, by + BTN + 1, bx + BTN + 2, by + BTN + 2, c);
                    g.fill(bx - 2, by - 2, bx - 1, by + BTN + 2, c);
                    g.fill(bx + BTN + 1, by - 2, bx + BTN + 2, by + BTN + 2, c);
                }
            }
        }

        g.text(font, "Eigene Farbe (Hex, z. B. FF8800)", left, top + HEX_LABEL_Y, 0xFFAAAAAA, false);
        if (!hexValue.isEmpty() && !hexValid()) {
            String err = "6 Hex-Ziffern";
            g.text(font, err, left + CONTENT_W - font.width(err), top + HEX_LABEL_Y, 0xFFFF5555, false);
        }

        g.text(font, "Spezialcode", left, top + SERIAL_LABEL_Y, 0xFFAAAAAA, false);
        if (!serialValue.isEmpty() && !serialValid(serialValue)) {
            String err = "4–51 Zeichen: A–Z, 0–9, _ und -";
            g.text(font, err, left + CONTENT_W - font.width(err), top + SERIAL_LABEL_Y, 0xFFFF5555, false);
        }
    }

    /** Live-Vorschau: gewählte Karte + eingegebener Name in der Kartenfarbe. */
    private void drawPreview(GuiGraphicsExtractor g, Font font) {
        String code = currentCode();
        String shown = nameValue.trim().isEmpty() ? (code != null ? code : "KC-" + ROW_LETTERS[selRow] + "-#??????") : nameValue.trim();
        Style style = code != null ? CardCodes.nameStyle(code) : Style.EMPTY;

        int maxTextW = CONTENT_W - 24;
        while (shown.length() > 1 && font.width(shown) > maxTextW) {
            shown = shown.substring(0, shown.length() - 1);
        }
        Component text = Component.literal(shown).withStyle(style);
        int totalW = 16 + 4 + font.width(text);
        int x = this.width / 2 - totalW / 2;
        g.item(iconFor(selRow, selCol), x, top + PREVIEW_Y - 1);
        g.text(font, text, x + 20, top + PREVIEW_Y + 4, 0xFFFFFFFF, true);
    }
}
