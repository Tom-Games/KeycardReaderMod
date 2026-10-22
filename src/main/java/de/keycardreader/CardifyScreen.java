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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Menü von /cardify: Kartenname, Farbe (= Code) per Klick auf die Karte, Spezialcode. */
public final class CardifyScreen extends Screen {
    // Reihenfolge der Farbspalten: Blue, Green, Yellow, Red, Purple
    private static final String[] COLOR_LETTERS = {"B", "G", "Y", "R", "P"};
    private static final String[] COLOR_NAMES = {"Blau", "Grün", "Gelb", "Rot", "Lila"};
    private static final String[] ROW_LETTERS = {"B", "W"};
    private static final String[] ROW_NAMES = {"Schwarz", "Weiß"};

    private static final int CONTENT_W = 180;
    private static final int PANEL_H = 186;
    private static final int BTN = 22;
    private static final int STEP = 26;

    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private String selectedCode = "KC-B-B";
    private String nameValue = "";
    private String serialValue = "";

    private EditBox nameBox;
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

    private static ItemStack iconFor(String code) {
        return ICONS.computeIfAbsent(code, c -> {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                    "keycardreader", KeycardReaderMod.CARD_MODELS.get(c)));
            return stack;
        });
    }

    private static boolean serialValid(String s) {
        return s.length() >= 4 && s.length() <= 51 && s.matches("[A-Za-z0-9_-]+");
    }

    @Override
    protected void init() {
        Font font = Minecraft.getInstance().font;
        left = this.width / 2 - CONTENT_W / 2;
        top = (this.height - PANEL_H) / 2;

        nameBox = new EditBox(font, left, top + 28, CONTENT_W, 20, Component.literal("Kartenname"));
        nameBox.setMaxLength(50);
        nameBox.setValue(nameValue);
        nameBox.setResponder(v -> nameValue = v);
        addRenderableWidget(nameBox);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                final String code = "KC-" + ROW_LETTERS[row] + "-" + COLOR_LETTERS[col];
                Button b = Button.builder(Component.empty(), btn -> selectedCode = code)
                        .bounds(buttonX(col), buttonY(row), BTN, BTN)
                        .build();
                b.setTooltip(Tooltip.create(Component.literal(
                        ROW_NAMES[row] + " – " + COLOR_NAMES[col] + " (" + code + ")")));
                addRenderableWidget(b);
            }
        }

        serialBox = new EditBox(font, left, top + 134, CONTENT_W, 20, Component.literal("Spezialcode"));
        serialBox.setMaxLength(51);
        serialBox.setValue(serialValue);
        serialBox.setResponder(v -> {
            serialValue = v;
            updateCreateState();
        });
        addRenderableWidget(serialBox);

        createButton = Button.builder(Component.literal("Erstellen"), b -> submit())
                .bounds(left, top + 164, 88, 20).build();
        addRenderableWidget(createButton);
        addRenderableWidget(Button.builder(Component.literal("Abbrechen"),
                        b -> onClose())
                .bounds(left + 92, top + 164, 88, 20).build());
        updateCreateState();
    }

    private int buttonX(int col) { return left + 52 + col * STEP; }
    private int buttonY(int row) { return top + 66 + row * STEP; }

    private void updateCreateState() {
        if (createButton != null) createButton.active = serialValid(serialValue);
    }

    private void submit() {
        if (!serialValid(serialValue)) return;
        ClientPlayNetworking.send(new CardifyRequestPayload(selectedCode, nameValue, serialValue));
        onClose();
    }

    private void drawOverlay(GuiGraphicsExtractor g) {
        Font font = Minecraft.getInstance().font;
        String title = "Keycard erstellen";
        g.text(font, title, this.width / 2 - font.width(title) / 2, top + 4, 0xFFFFFFFF, true);
        g.text(font, "Kartenname", left, top + 17, 0xFFAAAAAA, false);
        g.text(font, "Farbe (zum Auswählen anklicken)", left, top + 54, 0xFFAAAAAA, false);

        for (int row = 0; row < 2; row++) {
            g.text(font, ROW_NAMES[row], left, buttonY(row) + 7, 0xFFFFFFFF, false);
            for (int col = 0; col < 5; col++) {
                String code = "KC-" + ROW_LETTERS[row] + "-" + COLOR_LETTERS[col];
                int bx = buttonX(col);
                int by = buttonY(row);
                g.item(iconFor(code), bx + 3, by + 3);
                if (code.equals(selectedCode)) {
                    int c = 0xFFFFFF55;
                    g.fill(bx - 2, by - 2, bx + BTN + 2, by - 1, c);
                    g.fill(bx - 2, by + BTN + 1, bx + BTN + 2, by + BTN + 2, c);
                    g.fill(bx - 2, by - 2, bx - 1, by + BTN + 2, c);
                    g.fill(bx + BTN + 1, by - 2, bx + BTN + 2, by + BTN + 2, c);
                }
            }
        }

        g.text(font, "Spezialcode", left, top + 122, 0xFFAAAAAA, false);
        if (!serialValue.isEmpty() && !serialValid(serialValue)) {
            g.text(font, "4–51 Zeichen: A–Z, 0–9, _ und -", left, top + 155, 0xFFFF5555, false);
        }
    }
}
