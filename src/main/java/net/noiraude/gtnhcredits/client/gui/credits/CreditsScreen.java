package net.noiraude.gtnhcredits.client.gui.credits;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.noiraude.gtnhcredits.Config;
import net.noiraude.gtnhcredits.GTNHCredits;
import net.noiraude.gtnhcredits.client.credits.CreditsController;
import net.noiraude.gtnhcredits.client.credits.CreditsController.FilterMethod;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsScreen extends CustomModularScreen {

    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 36;

    private static final int FILTER_WIDTH = 120;
    private static final int FILTER_HEIGHT = 20;
    private static final int FILTER_MARGIN = 8;

    // Initialized in buildUI(), which is called by the superclass constructor
    // before our field
    // initializers run — so it cannot be a field initializer expression.
    private CreditsController controller;
    private GuiTextField filterField;
    private FilterMethod currentFilterType;
    private String currentFilterLang;

    public static @NotNull ModularScreen create() {
        return new CreditsScreen();
    }

    private CreditsScreen() {
        super(GTNHCredits.MODID);
        openParentOnClose(true);
        pausesGame(true);
        this.currentFilterType = CreditsController.FilterMethod.EXACT;
        this.currentFilterLang = this.currentFilterType.getLang();
    }

    @Override
    public @NotNull ModularPanel buildUI(@NotNull ModularGuiContext context) {
        controller = new CreditsController();

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

        panel.child(CreditsBodyPanel.create(HEADER_HEIGHT, FOOTER_HEIGHT, controller));

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
                }))
            .child(
                new ButtonWidget<>().size(100, 20)
                    .right(8 + FILTER_WIDTH + FILTER_MARGIN)
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
                        IKey.dynamic(() -> StatCollector.translateToLocal(currentFilterLang))
                            .color(0xE0E0E0))
                    .hoverOverlay(
                        IKey.dynamic(() -> StatCollector.translateToLocal(currentFilterLang))
                            .color(0xFFFFA0))
                    .tooltipDynamic(t -> {
                        t.addLine(
                            EnumChatFormatting.BOLD + StatCollector.translateToLocal("gui.credits.button.filter.exact")
                                + EnumChatFormatting.RESET
                                + ": "
                                + StatCollector.translateToLocal("gui.credits.button.filter.exact_tip_line"));
                        t.addLine(
                            EnumChatFormatting.BOLD + StatCollector.translateToLocal("gui.credits.button.filter.fuzzy")
                                + EnumChatFormatting.RESET
                                + ": "
                                + StatCollector.translateToLocal("gui.credits.button.filter.fuzzy_tip_line"));
                    })
                    .onMousePressed(mb -> {
                        if (mb == 0 || mb == 1) {
                            currentFilterType = currentFilterType == FilterMethod.EXACT ? FilterMethod.FUZZY
                                : FilterMethod.EXACT;

                            currentFilterLang = currentFilterType.getLang();
                            controller.setFilterMethod(currentFilterType);
                            return true;
                        }
                        return false;
                    }));
        return panel;
    }

    @Override
    public void onOpen() {
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onClose() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        // Recreate the filter field at the new position, preserving current text.
        String text = filterField != null ? filterField.getText() : controller.getPersonFilter();
        Area area = getContext().getScreenArea();
        filterField = new GuiTextField(
            Minecraft.getMinecraft().fontRenderer,
            area.w() - FILTER_MARGIN - FILTER_WIDTH,
            area.h() - FILTER_MARGIN - FILTER_HEIGHT,
            FILTER_WIDTH,
            FILTER_HEIGHT);
        filterField.setMaxStringLength(256);
        filterField.setFocused(true);
        filterField.setText(text);
        filterField.setCursorPositionEnd();
    }

    @Override
    public void drawScreen() {
        super.drawScreen();
        filterField.drawTextBox();
        if (
            filterField.getText()
                .isEmpty()
        ) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.fontRenderer.drawStringWithShadow(
                StatCollector.translateToLocal("gui.credits.filter.hint"),
                filterField.xPosition + 4,
                filterField.yPosition + (FILTER_HEIGHT - mc.fontRenderer.FONT_HEIGHT) / 2,
                0x888888);
        }
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        if (filterField.textboxKeyTyped(typedChar, keyCode)) {
            controller.setPersonFilter(filterField.getText());
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        filterField.mouseClicked(getContext().getMouseX(), getContext().getMouseY(), mouseButton);
        return super.onMousePressed(mouseButton);
    }
}
