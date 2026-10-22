package de.keycardreader;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Zeichnet rechts neben dem Reader-Inventar die Keycards aus dem Trichter darunter. */
public final class ReaderOverlay {
    private static final int GUI_WIDTH = 176;   // Breite des Dispenser/Dropper-Menüs
    private static final int GUI_HEIGHT = 166;
    private static final long MAX_AGE_MS = 2500; // Server sendet alle ~0,5 s neu
    private static final int PAD = 4;

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
        int slots = Math.max(1, cards.size());
        int titleW = font.width(title);
        int panelW = Math.max(titleW, slots * 18) + PAD * 2;
        int panelH = PAD + font.lineHeight + 3 + 18 + PAD;

        int x0 = (screen.width + GUI_WIDTH) / 2 + 2;
        int y0 = (screen.height - GUI_HEIGHT) / 2;

        // Hintergrund + Rahmen
        g.fill(x0 - 1, y0 - 1, x0 + panelW + 1, y0 + panelH + 1, 0xFF000000);
        g.fill(x0, y0, x0 + panelW, y0 + panelH, 0xFFC6C6C6);
        // Titel mittig
        g.text(font, title, x0 + (panelW - titleW) / 2, y0 + PAD, 0xFF404040, false);

        int sy = y0 + PAD + font.lineHeight + 3;
        if (cards.isEmpty()) {
            String none = "keine";
            g.text(font, none, x0 + (panelW - font.width(none)) / 2, sy + 5, 0xFF707070, false);
            return;
        }

        // Slots als Gruppe mittig unter dem Titel
        int startX = x0 + (panelW - cards.size() * 18) / 2 + 1;
        ItemStack hovered = null;
        for (int i = 0; i < cards.size(); i++) {
            int sx = startX + i * 18;
            ItemStack stack = cards.get(i);
            g.fill(sx - 1, sy, sx + 17, sy + 18, 0xFF8B8B8B); // Slot-Look
            g.fill(sx, sy + 1, sx + 16, sy + 17, 0xFF373737);
            g.item(stack, sx, sy + 1);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy + 1 && mouseY < sy + 17) {
                g.fill(sx, sy + 1, sx + 16, sy + 17, 0x80FFFFFF);
                hovered = stack;
            }
        }
        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }
}
