package net.noiraude.gtnhcredits.client.minecraft.gui.main_menu;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.Config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsButton extends GuiButton {

    private static final int PADDING_H = 4;
    private static final int PADDING_V = 4;
    private static final int ICON_TEXT_GAP = 4;

    private final ResourceLocation iconResource;
    private final int renderedIconWidth;
    private final int renderedIconHeight;

    private CreditsButton(int id, Config config, GuiScreen guiScreen) {
        super(id, 0, 0, 200, 20, StatCollector.translateToLocal("gui.main_menu.button.credits"));

        String text = this.displayString;
        Minecraft mc = Minecraft.getMinecraft();

        this.iconResource = config.menuButtonIcon.isEmpty() ? null : new ResourceLocation(config.menuButtonIcon);
        BufferedImage image = getBufferedImage(this.iconResource);
        this.renderedIconWidth = image != null ? image.getWidth() : 0;
        this.renderedIconHeight = image != null ? image.getHeight() : 0;

        int contentHeight = Math.max(this.renderedIconHeight, mc.fontRenderer.FONT_HEIGHT);
        int textWidth = guiScreen.mc.fontRenderer.getStringWidth(text);
        this.width = Math.max(
            PADDING_H
                + (this.renderedIconWidth > 0 ? this.renderedIconWidth + (!text.isEmpty() ? ICON_TEXT_GAP : 0) : 0)
                + textWidth
                + PADDING_H,
            200);
        this.height = Math.max(contentHeight + PADDING_V * 2, 20);
        this.xPosition = config.menuButtonX >= 0 ? config.menuButtonX
            : guiScreen.width - this.width + config.menuButtonX;
        this.yPosition = config.menuButtonY >= 0 ? config.menuButtonY
            : guiScreen.height - this.height + config.menuButtonY;
    }

    /**
     * Finds a free button ID for a new button.
     * <p>
     * ID 0 is never returned, it is excluded to avoid sentinel value collisions.
     * The returned ID is always &ge; 1.
     * An empty list returns 1.
     *
     * @param buttonList the list of buttons to check for used IDs
     * @return a free button ID &ge; 1 not used by any button in the list
     * @implNote Three search strategies are attempted in order of cost:
     *           <ol>
     *           <li>Trailing gap: if max &lt; {@link Integer#MAX_VALUE}, return max + 1. O(n), no allocation.</li>
     *           <li>Leading gap: if min &gt; 1, return min - 1. O(1) extra work after the first pass.</li>
     *           <li>Interior gap: sort IDs and scan consecutive pairs for the first gap. O(n log n), one array
     *           allocation. Only reached when {@link Integer#MAX_VALUE} is genuinely in use and no leading gap
     *           exists. (rarely reached in normal operation)</li>
     *           </ol>
     *
     * @throws IllegalStateException if no free button IDs are available (i.e. all integers from 1 to
     *                               {@link Integer#MAX_VALUE} are in use)
     */
    @Contract(pure = true)
    public static int findFreeButtonId(@NotNull List<GuiButton> buttonList) throws IllegalStateException {
        int max = 0, min = Integer.MAX_VALUE;
        for (@NotNull
        GuiButton button : buttonList) {
            if (button.id > max) max = button.id;
            if (button.id < min) min = button.id;
        }
        if (max < Integer.MAX_VALUE) return max + 1; // trailing gap: max + 1 is free and overflow-safe, O(n)
        min--;
        if (min > 0) return min; // leading gap: min - 1 is free, no allocation needed, O(1) extra
        int @NotNull [] ids = new int[buttonList.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = buttonList.get(i).id;
        }
        Arrays.sort(ids);
        int prev = ids[0];
        for (int i = 1; i < ids.length; i++) {
            int next = prev + 1;
            if (ids[i] > next) return next; // interior gap between prev and ids[i], O(n log n)
            prev = ids[i];
        }
        throw new IllegalStateException("No free button IDs available");
    }

    public static @NotNull CreditsButton create(@NotNull Config config, @NotNull GuiScreen guiScreen,
        @NotNull List<GuiButton> buttonList) {
        @NotNull
        CreditsButton button = new CreditsButton(findFreeButtonId(buttonList), config, guiScreen);
        buttonList.add(button);
        return button;
    }

    private static @Nullable BufferedImage getBufferedImage(@Nullable ResourceLocation resourceLocation) {
        if (resourceLocation == null) return null;
        try {
            IResource resource = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(resourceLocation);
            // noinspection BlockingMethodInNonBlockingContext
            return ImageIO.read(resource.getInputStream());
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public void drawButton(@NotNull Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        String label = this.displayString;
        this.displayString = "";
        super.drawButton(mc, mouseX, mouseY);
        this.displayString = label;

        int centerY = this.yPosition + this.height / 2;
        int cursorX = this.xPosition + PADDING_H;
        boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        cursorX = drawIcon(mc, cursorX, centerY);
        drawLabel(mc, label, cursorX, centerY, hovered);
    }

    /** Draws the icon if present; returns the x position after the icon (and gap). */
    private int drawIcon(@NotNull Minecraft mc, int x, int centerY) {
        if (iconResource == null || renderedIconWidth <= 0 || renderedIconHeight <= 0) return x;
        mc.getTextureManager()
            .bindTexture(iconResource);
        this.drawTexturedModalRect(x, centerY - renderedIconHeight / 2, 0, 0, renderedIconWidth, renderedIconHeight);
        return x + renderedIconWidth + ICON_TEXT_GAP;
    }

    private void drawLabel(@NotNull Minecraft mc, String label, int x, int centerY, boolean hovered) {
        if (label.isEmpty()) return;
        int color = this.packedFGColour != 0 ? this.packedFGColour
            : (!this.enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0);
        mc.fontRenderer.drawStringWithShadow(label, x, centerY - mc.fontRenderer.FONT_HEIGHT / 2, color);
    }
}
