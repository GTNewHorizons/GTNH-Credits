package net.noiraude.gtnhcredits.client.gui.credits;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.Config;
import net.noiraude.gtnhcredits.GTNHCredits;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsScreen extends CustomModularScreen {

    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 36;

    public static @NotNull ModularScreen create() {
        return new CreditsScreen();
    }

    private CreditsScreen() {
        super(GTNHCredits.MODID);
        openParentOnClose(true);
        pausesGame(true);
    }

    @Override
    public @NotNull ModularPanel buildUI(@NotNull ModularGuiContext context) {
        @NotNull
        Config config = Config.getInstance();

        ModularPanel panel = new ModularPanel("credits").full()
            .background((ctx, x, y, w, h, theme) -> {
                GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                if (screen != null) screen.drawBackground(0);
            });

        // --- Header ---

        @NotNull
        ResourceLocation logoResource = new ResourceLocation(config.creditsScreenLogo);
        final int logoH = HEADER_HEIGHT - 4;
        int texW = 0, texH = 0;
        try (InputStream is = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(logoResource)
            .getInputStream()) {
            @SuppressWarnings("BlockingMethodInNonBlockingContext")
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                texW = img.getWidth();
                texH = img.getHeight();
            }
        } catch (IOException ignored) {}
        int logoW = (texH > 0) ? texW * logoH / texH : logoH;

        panel.child(
            new Widget<>().size(logoW, logoH)
                .left(2)
                .top(2)
                .background(
                    UITexture.builder()
                        .location(logoResource)
                        .fullImage()
                        .nonOpaque()
                        .build()));

        panel.child(
            new TextWidget<>("§6§l" + IKey.str(StatCollector.translateToLocal("gui.credits.title")) + "§r").scale(2.0f)
                .textAlign(Alignment.CenterLeft)
                .left(2 + logoW + 6)
                .top(0)
                .right(0)
                .height(HEADER_HEIGHT));

        // --- Middle area (between header and footer) ---

        CreditsBodyPanel bodyPanel = CreditsBodyPanel.create(HEADER_HEIGHT, FOOTER_HEIGHT);
        panel.child(bodyPanel);

        // --- Footer: person filter field ---

        panel.child(
            bodyPanel.createFilterField()
                .right(8)
                .bottom(8)
                .width(120)
                .height(20));

        // --- Footer: back button ---

        @NotNull
        ResourceLocation widgetsTexture = new ResourceLocation("textures/gui/widgets.png");
        panel.child(
            new ButtonWidget<>().size(100, 20)
                .left(8)
                .bottom(8)
                .background(
                    UITexture.builder()
                        .location(widgetsTexture)
                        .imageSize(256, 256)
                        .subAreaXYWH(0, 66, 200, 20)
                        .build())
                .hoverBackground(
                    UITexture.builder()
                        .location(widgetsTexture)
                        .imageSize(256, 256)
                        .subAreaXYWH(0, 86, 200, 20)
                        .build())
                .overlay(
                    IKey.lang("gui.credits.button.back")
                        .color(0xE0E0E0))
                .hoverOverlay(
                    IKey.lang("gui.credits.button.back")
                        .color(0xFFFFA0))
                .onMousePressed(mb -> {
                    if (mb == 0 || mb == 1) {
                        panel.closeIfOpen();
                        return true;
                    }
                    return false;
                }));

        return panel;
    }
}
