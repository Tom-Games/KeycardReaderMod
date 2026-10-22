package de.keycardreader;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Zeichnet rechts neben dem Reader-Inventar ein kleines Feld "Benötigte KeyCard"
 * mit den Keycards aus dem Trichter direkt unter dem Reader.
 * Beim Hovern über eine Karte erscheint ihr Name (Tooltip).
 */
public final class ReaderOverlay {
    private static final int GUI_WIDTH = 176;    // Breite des Dispenser/Dropper-Menüs
    private static final int GUI_HEIGHT = 166;
    private static final long MAX_AGE_MS = 2500;  // Server sendet alle ~0,5 s neu
    private static final float SCALE = 0.75f;     // Anzeige insgesamt verkleinert
    private static final int PAD = 4;
    private static final int SLOT = 18;

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

    private static void render(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!active || System.currentTimeMillis() - lastUpdate > MAX_AGE_MS) return;

        Font font = Minecraft.getInstance().font;
        String title = "Benötigte KeyCard";
        int titleW = font.width(title);
        int slots = Math.max(1, cards.size());

        // Alle Maße unten sind "lokal" (unskaliert); gezeichnet wird mit SCALE.
        int panelW = Math.max(titleW, slots * SLOT) + PAD * 2;
        int panelH = PAD + font.lineHeight + 3 + SLOT + PAD;

        // Position: direkt rechts neben dem Inventar, oben bündig
        int x0 = (screen.width + GUI_WIDTH) / 2 + 2;
        int y0 = (screen.height - GUI_HEIGHT) / 2;

        // Maus in lokale Koordinaten umrechnen (für das Hovern)
        float lx = (mouseX - x0) / SCALE;
        float ly = (mouseY - y0) / SCALE;

        int titleX = (panelW - titleW) / 2;
        int slotY = PAD + font.lineHeight + 3;

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
            g.text(font, none, (panelW - font.width(none)) / 2, slotY + (SLOT - font.lineHeight) / 2,
                    0xFF707070, false);
        } else {
            // Slots als Gruppe exakt mittig im Feld
            int startX = (panelW - cards.size() * SLOT) / 2;
            for (int i = 0; i < cards.size(); i++) {
                int sx = startX + i * SLOT;
                ItemStack stack = cards.get(i);
                g.fill(sx, slotY, sx + SLOT, slotY + SLOT, 0xFF8B8B8B); // Slot-Rand
                g.fill(sx + 1, slotY + 1, sx + SLOT - 1, slotY + SLOT - 1, 0xFF373737);
                g.item(stack, sx + 1, slotY + 1);
                if (lx >= sx + 1 && lx < sx + SLOT - 1 && ly >= slotY + 1 && ly < slotY + SLOT - 1) {
                    g.fill(sx + 1, slotY + 1, sx + SLOT - 1, slotY + SLOT - 1, 0x80FFFFFF);
                    hovered = stack;
                }
            }
        }

        g.pose().popMatrix();

        // Tooltip mit dem Kartennamen in normaler Größe, an der echten Mausposition
        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }
}
