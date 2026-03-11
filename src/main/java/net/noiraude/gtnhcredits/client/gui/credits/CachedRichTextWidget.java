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
 * A rich text widget that compiles its text content only when dirty.
 * The builder receives the {@link RichText} and the current content width so callers can perform
 * width-aware layout such as manual line-wrapping with custom alignment.
 * Recompilation is triggered by an explicit {@link #markDirty()} call or a significant width change.
 * Small width fluctuations (≤ {@value #SCROLLBAR_TOLERANCE}px) are ignored — they are caused by
 * the scroll widget reserving/releasing space for its scrollbar, and a few pixels of difference
 * do not meaningfully affect text layout.
 */
@SideOnly(Side.CLIENT)
class CachedRichTextWidget extends Widget<CachedRichTextWidget> {

    /**
     * Maximum width change (in pixels) that will NOT trigger a recompile.
     * This absorbs the per-category scrollbar toggle: the vertical scrollbar is 4px wide,
     * so content width oscillates ±4px as it appears/disappears. Compiled text at the
     * wider width renders correctly at 4px narrower — no visible difference.
     */
    private static final int SCROLLBAR_TOLERANCE = 8;
    /** Vertical gap (px) between the title and the body text. */
    private static final int TITLE_BODY_GAP = 10;
    /** Font scale applied to the category title. */
    private static final float TITLE_SCALE = 1.5f;
    /** Gold colour for the category title. */
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
        Area area = getArea();
        int px = area.getPadding()
            .getLeft();
        int py = area.getPadding()
            .getTop();
        int pw = area.paddedWidth();
        WidgetTheme wt = getActiveWidgetTheme(widgetTheme, isHovering());
        // Recompile only when explicitly marked dirty (category change) or when the width
        // changes significantly. Small fluctuations from the scrollbar appearing/disappearing
        // (SCROLLBAR_TOLERANCE) are intentionally ignored to prevent spurious recompiles.
        boolean widthChangedSignificantly = Math.abs(pw - lastCompiledWidth) > SCROLLBAR_TOLERANCE;
        boolean needsRecompile = (dirty || widthChangedSignificantly) && pw > 0;
        if (needsRecompile) {
            lastCompiledWidth = pw;
            dirty = false;
            cachedLines = null;
        }

        // --- Title (TITLE_SCALE, gold, centered) ---
        int bodyPy = py;
        if (titleTextSupplier != null) {
            if (needsRecompile) {
                String title = titleTextSupplier.get();
                FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
                titleRichText.clearText();
                titleRichText.scale(TITLE_SCALE);
                titleRichText.addLine(new TitleLine(title, fr.getStringWidth(title), pw));
            }
            titleRichText.setupRenderer(titleRenderer, px, py, pw, -1, TITLE_COLOR, wt.getTextShadow());
            titleRichText.compileAndDraw(titleRenderer, context, false);
            lastTitleHeight = (int) titleRenderer.getLastTrimmedHeight();
            bodyPy = py + lastTitleHeight + TITLE_BODY_GAP;
        }

        // --- Body ---
        if (needsRecompile) {
            richText.clearText();
            if (builder != null) builder.accept(richText, pw);
        }
        richText.setupRenderer(textRenderer, px, bodyPy, pw, -1, wt.getTextColor(), wt.getTextShadow());
        if (cachedLines == null && pw > 0) {
            cachedLines = richText.compileAndDraw(textRenderer, context, false);
        } else if (cachedLines != null) {
            textRenderer.drawCompiled(context, cachedLines);
        }
    }

    /**
     * A centered, gold, scaled title line.
     *
     * <p>
     * {@link TextRenderer#drawCompiled} calls {@code draw()} with coordinates already in the
     * pre-GL-scale space: the renderer has pushed a {@code scale(TITLE_SCALE)} matrix before
     * invoking each line. To appear horizontally centered inside {@code containerWidth} screen
     * pixels the unscaled x offset must be {@code (containerWidth/TITLE_SCALE - textWidth) / 2},
     * which maps to {@code (containerWidth - textWidth*TITLE_SCALE) / 2} on screen — exactly the
     * same centering formula used by {@code CenteredLine}, adjusted for scale.
     */
    private static final class TitleLine implements ITextLine {

        private final String text;
        private final int textWidth;
        private final int containerWidth;

        TitleLine(String text, int textWidth, int containerWidth) {
            this.text = text;
            this.textWidth = textWidth;
            this.containerWidth = containerWidth;
        }

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
