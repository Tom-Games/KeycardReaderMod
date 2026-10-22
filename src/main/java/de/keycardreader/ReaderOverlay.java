package de.keycardreader;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Zeichnet rechts neben dem Reader-Inventar die Keycards aus dem Trichter darunter. */
public final class ReaderOverlay {
    private static final int GUI_WIDTH = 176;   // Breite des Dispenser/Dropper-Menüs
    private static final int GUI_HEIGHT = 166;
    private static final long MAX_AGE_MS = 2500; // Server sendet alle ~0,5 s neu

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
                ScreenEvents.afterRender(screen).register(ReaderOverlay::render);
            }
        });
    }

    private static void render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (!active || System.currentTimeMillis() - lastUpdate > MAX_AGE_MS) return;

        Font font = Minecraft.getInstance().font;
        String title = "Karte im Trichter";
        int slots = Math.max(1, cards.size());
        int panelW = Math.max(font.width(title), slots * 18) + 12;
        int panelH = 12 + font.lineHeight + 4 + 18 + 6;

        int x0 = (screen.width + GUI_WIDTH) / 2 + 6;
        int y0 = (screen.height - GUI_HEIGHT) / 2;

        // Hintergrund + Rahmen
        g.fill(x0 - 1, y0 - 1, x0 + panelW + 1, y0 + panelH + 1, 0xFF000000);
        g.fill(x0, y0, x0 + panelW, y0 + panelH, 0xFFC6C6C6);
        g.drawString(font, title, x0 + 6, y0 + 6, 0xFF404040, false);

        int sy = y0 + 6 + font.lineHeight + 4;
        if (cards.isEmpty()) {
            g.drawString(font, "keine", x0 + 6, sy + 5, 0xFF707070, false);
            return;
        }

        ItemStack hovered = null;
        for (int i = 0; i < cards.size(); i++) {
            int sx = x0 + 6 + i * 18;
            ItemStack stack = cards.get(i);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF8B8B8B); // Slot-Look
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF373737);
            g.renderItem(stack, sx, sy);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                hovered = stack;
            }
        }
        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }
}
