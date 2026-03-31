package net.noiraude.gtnhcredits.client.gui.credits;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.api.drawable.ITextLine;
import com.cleanroommc.modularui.drawable.text.RichText;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Widget displaying a credits category page as an optional title followed by body rich text,
 * with lazy recompilation via {@link #markDirty()}.
 */
@SideOnly(Side.CLIENT)
class CachedRichTextWidget extends Widget<CachedRichTextWidget> {

    /** Maximum width delta (px) ignored by recompile checks to absorb scrollbar appearance. */
    private static final int SCROLLBAR_TOLERANCE = 8;
    /** Vertical gap (px) between the title and the body text. */
    private static final int TITLE_BODY_GAP = 10;
    /** Font scale applied to the category title. */
    private static final float TITLE_SCALE = 1.5f;
    /** Gold color for the category title. */
    private static final int TITLE_COLOR = 0xFFAA00;

    private final RichText titleRichText = new RichText();
    private final TextRenderer titleRenderer = new TextRenderer();
    private Supplier<String> titleTextSupplier = null;
    private int lastTitleHeight = 0;

    private final RichText richText = new RichText();
    private final TextRenderer textRenderer = new TextRenderer();
    private BiConsumer<RichText, Integer> builder;
    private List<ITextLine> cachedLines;
    private boolean dirty = false;
    private int lastCompiledWidth = -1;

    CachedRichTextWidget titleText(Supplier<String> supplier) {
        this.titleTextSupplier = supplier;
        markDirty();
        return this;
    }

    CachedRichTextWidget textColor(int color) {
        richText.textColor(color);
        return this;
    }

    CachedRichTextWidget textBuilder(BiConsumer<RichText, Integer> builder) {
        this.builder = builder;
        markDirty();
        return this;
    }

    void markDirty() {
        this.dirty = true;
        this.cachedLines = null;
    }

    int getLastHeight() {
        int titlePart = lastTitleHeight > 0 ? lastTitleHeight + TITLE_BODY_GAP : 0;
        return titlePart + (int) textRenderer.getLastTrimmedHeight();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        boolean needsRecompile = checkAndUpdateRecompile(getArea().paddedWidth());
        int bodyPy = drawTitle(context, widgetTheme, needsRecompile);
        drawBody(context, widgetTheme, bodyPy, needsRecompile);
    }

    /**
     * @param width available content width in pixels (widget width minus padding)
     * @return {@code true} if text needs to be recompiled; also resets compile state
     */
    private boolean checkAndUpdateRecompile(int width) {
        boolean widthChangedSignificantly = Math.abs(width - lastCompiledWidth) > SCROLLBAR_TOLERANCE;
        boolean needsRecompile = (dirty || widthChangedSignificantly) && width > 0;
        if (needsRecompile) {
            lastCompiledWidth = width;
            dirty = false;
            cachedLines = null;
        }
        return needsRecompile;
    }

    /**
     * Draws the category title.
     *
     * @return top y offset for the body text
     */
    private int drawTitle(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme, boolean needsRecompile) {
        Area area = getArea();
        int py = area.getPadding()
            .getTop();
        if (titleTextSupplier == null) return py;
        int px = area.getPadding()
            .getLeft();
        int pw = area.paddedWidth();
        if (needsRecompile) {
            String title = titleTextSupplier.get();
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            titleRichText.clearText();
            titleRichText.scale(TITLE_SCALE);
            titleRichText.addLine(new TitleLine(title, fr.getStringWidth(title), pw));
        }
        WidgetTheme wt = widgetTheme.getTheme(isHovering());
        titleRichText.setupRenderer(titleRenderer, px, py, pw, -1, TITLE_COLOR, wt.getTextShadow());
        titleRichText.compileAndDraw(titleRenderer, context, false);
        lastTitleHeight = (int) titleRenderer.getLastTrimmedHeight();
        return py + lastTitleHeight + TITLE_BODY_GAP;
    }

    /** Draws the body text, compiling it when needed and using the cached lines otherwise. */
    private void drawBody(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme, int bodyPy,
        boolean needsRecompile) {
        Area area = getArea();
        int px = area.getPadding()
            .getLeft();
        int pw = area.paddedWidth();
        if (needsRecompile) {
            richText.clearText();
            if (builder != null) builder.accept(richText, pw);
        }
        WidgetTheme wt = widgetTheme.getTheme(isHovering());
        richText.setupRenderer(textRenderer, px, bodyPy, pw, -1, wt.getTextColor(), wt.getTextShadow());
        if (cachedLines == null && pw > 0) {
            cachedLines = richText.compileAndDraw(textRenderer, context, false);
        } else if (cachedLines != null) {
            textRenderer.drawCompiled(context, cachedLines);
        }
    }

    /** The credits category title, rendered as the first line above the body text. */
    private static final class TitleLine implements ITextLine {

        private final String text;
        private final int textWidth;
        private final int containerWidth;

        TitleLine(String text, int textWidth, int containerWidth) {
            this.text = text;
            this.textWidth = textWidth;
            this.containerWidth = containerWidth;
        }

        /**
         * {@inheritDoc}
         * 
         * @implNote Ignores {@code color}; always renders in {@link #TITLE_COLOR}, centered within
         *           {@link #containerWidth}.
         */
        @Override
        public void draw(GuiContext context, FontRenderer fr, float x, float y, int color, boolean shadow,
            int availableWidth, int availableHeight) {
            int cx = Math.round(x + (containerWidth / TITLE_SCALE - textWidth) / 2f);
            Platform.setupDrawFont();
            fr.drawString(text, cx, Math.round(y), TITLE_COLOR, shadow);
        }

        @Override
        public int getWidth() {
            return containerWidth;
        }

        @Override
        public int getHeight(FontRenderer fr) {
            return fr.FONT_HEIGHT + 1;
        }

        @Override
        public Object getHoveringElement(FontRenderer fr, int x, int y) {
            return null;
        }
    }
}
