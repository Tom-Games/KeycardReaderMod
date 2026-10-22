package de.keycardreader;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeichnet rechts neben dem Reader-Inventar ein kleines Feld "Benötigte KeyCard".
 * Pro Karte aus dem Trichter darunter: Slot mit Item + der Kartenname direkt daneben.
 */
public final class ReaderOverlay {
    private static final int GUI_WIDTH = 176;    // Breite des Dispenser/Dropper-Menüs
    private static final int GUI_HEIGHT = 166;
    private static final long MAX_AGE_MS = 2500;  // Server sendet alle ~0,5 s neu
    private static final float SCALE = 0.75f;     // Anzeige insgesamt verkleinert
    private static final int PAD = 4;
    private static final int SLOT = 18;
    private static final int GAP = 3;             // Abstand Slot <-> Name
    private static final int MAX_NAME_W = 100;    // lokale Breite, danach wird der Name gekürzt

    private static boolean active = false;
    private static List<ItemStack> cards = List.of();
    private static long lastUpdate = 0;

    private ReaderOverlay() {}

    public static void update(ReaderPayload payload) {
        active = payload.active();
        cards = payload.cards();
        lastUpdate = System.currentTimeMillis();
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DispenserScreen) {
                ScreenEvents.afterExtract(screen).register(ReaderOverlay::render);
            }
        });
    }

    /** Name der Karte; zu lange Namen werden mit "…" gekürzt (Farbe/Stil bleiben erhalten). */
    private static Component displayName(Font font, ItemStack stack) {
        Component name = stack.getHoverName();
        if (font.width(name) <= MAX_NAME_W) return name;
        String s = name.getString();
        while (s.length() > 1 && font.width(s + "…") > MAX_NAME_W) {
            s = s.substring(0, s.length() - 1);
        }
        return Component.literal(s + "…").withStyle(name.getStyle());
    }

    private static void render(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!active || System.currentTimeMillis() - lastUpdate > MAX_AGE_MS) return;

        Font font = Minecraft.getInstance().font;
        String title = "Benötigte KeyCard";
        int titleW = font.width(title);

        List<Component> names = new ArrayList<>();
        int maxRowW = 0;
        for (ItemStack stack : cards) {
            Component n = displayName(font, stack);
            names.add(n);
            maxRowW = Math.max(maxRowW, SLOT + GAP + font.width(n));
        }

        // Alle Maße unten sind "lokal" (unskaliert); gezeichnet wird mit SCALE.
        int rows = Math.max(1, cards.size());
        int contentW = cards.isEmpty() ? font.width("keine") : maxRowW;
        int panelW = Math.max(titleW, contentW) + PAD * 2;
        int panelH = PAD + font.lineHeight + 3 + rows * SLOT + PAD;

        // Position: direkt rechts neben dem Inventar, oben bündig
        int x0 = (screen.width + GUI_WIDTH) / 2 + 2;
        int y0 = (screen.height - GUI_HEIGHT) / 2;

        // Maus in lokale Koordinaten umrechnen (für das Hovern)
        float lx = (mouseX - x0) / SCALE;
        float ly = (mouseY - y0) / SCALE;

        int titleX = (panelW - titleW) / 2;
        int firstRowY = PAD + font.lineHeight + 3;

        ItemStack hovered = null;

        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().scale(SCALE, SCALE);

        // Hintergrund + Rahmen
        g.fill(-1, -1, panelW + 1, panelH + 1, 0xFF000000);
        g.fill(0, 0, panelW, panelH, 0xFFC6C6C6);
        // Titel mittig
        g.text(font, title, titleX, PAD, 0xFF404040, false);

        if (cards.isEmpty()) {
            String none = "keine";
            g.text(font, none, (panelW - font.width(none)) / 2,
                    firstRowY + (SLOT - font.lineHeight) / 2, 0xFF707070, false);
        } else {
            // Slot + Name bilden einen Block, der als Ganzes mittig im Feld sitzt
            int startX = (panelW - maxRowW) / 2;
            for (int i = 0; i < cards.size(); i++) {
                int sy = firstRowY + i * SLOT;
                ItemStack stack = cards.get(i);
                g.fill(startX, sy, startX + SLOT, sy + SLOT, 0xFF8B8B8B); // Slot-Rand
                g.fill(startX + 1, sy + 1, startX + SLOT - 1, sy + SLOT - 1, 0xFF373737);
                g.item(stack, startX + 1, sy + 1);
                // Kartenname immer direkt neben der Karte, vertikal mittig zum Slot
                g.text(font, names.get(i), startX + SLOT + GAP,
                        sy + (SLOT - font.lineHeight) / 2, 0xFF404040, false);
                if (lx >= startX + 1 && lx < startX + SLOT - 1 && ly >= sy + 1 && ly < sy + SLOT - 1) {
                    g.fill(startX + 1, sy + 1, startX + SLOT - 1, sy + SLOT - 1, 0x80FFFFFF);
                    hovered = stack;
                }
            }
        }

        g.pose().popMatrix();

        // Tooltip (normale Größe, echte Mausposition)
        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }
}
