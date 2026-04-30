package net.noiraude.creditseditor.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

import org.jetbrains.annotations.NotNull;

/**
 * Pre-scaled cache of {@link UiMetrics} constants for the active screen DPI.
 *
 * <p>
 * Populated on first class-load from the default screen device. Call
 * {@link #attachTo(Window)} once the main window exists to track its
 * {@code graphicsConfiguration} property: a multi-monitor migration to a screen with a
 * different transform refreshes the cache and triggers a {@link Window#revalidate()} of
 * the window tree.
 *
 * <p>
 * Already-constructed borders, insets, and struts hold the values they were given at build
 * time and do not retroactively pick up later changes; only components that rebuild during
 * the post-event relayout reflect the new values.
 */
public final class ScaledMetrics {

    private ScaledMetrics() {}

    public static int gapHair = UiMetrics.GAP_HAIR;
    public static int gapTiny = UiMetrics.GAP_TINY;
    public static int gapToolbarGroup = UiMetrics.GAP_TOOLBAR_GROUP;
    public static int gapSmall = UiMetrics.GAP_SMALL;
    public static int gapMedium = UiMetrics.GAP_MEDIUM;
    public static int gapLarge = UiMetrics.GAP_LARGE;
    public static int gapXLarge = UiMetrics.GAP_XLARGE;
    public static int gapXXLarge = UiMetrics.GAP_XXLARGE;
    public static int gapHuge = UiMetrics.GAP_HUGE;

    public static int toolbarIconSize = UiMetrics.TOOLBAR_ICON_SIZE;
    public static int fontHeadingDelta = UiMetrics.FONT_HEADING_DELTA;
    public static int scrollUnitIncrement = UiMetrics.SCROLL_UNIT_INCREMENT;

    public static int splitDividerLeft = UiMetrics.SPLIT_DIVIDER_LEFT;
    public static int splitDividerRight = UiMetrics.SPLIT_DIVIDER_RIGHT;

    public static int importDialogWidth = UiMetrics.IMPORT_DIALOG_WIDTH;
    public static int importDialogHeight = UiMetrics.IMPORT_DIALOG_HEIGHT;

    public static int mainWindowWidth = UiMetrics.MAIN_WINDOW_WIDTH;
    public static int mainWindowHeight = UiMetrics.MAIN_WINDOW_HEIGHT;

    private static volatile double currentScale = 1.0;

    static {
        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
            refresh(
                gc.getDefaultTransform()
                    .getScaleX());
        }
    }

    /**
     * Tracks DPI changes on a top-level window. Reads the window's current
     * {@link GraphicsConfiguration} and listens for {@code "graphicsConfiguration"} property
     * changes; every transition with a different transform refreshes the cache and reflows
     * the window tree.
     */
    public static void attachTo(@NotNull Window window) {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        if (
            gc != null && refresh(
                gc.getDefaultTransform()
                    .getScaleX())
        ) {
            window.revalidate();
            window.repaint();
        }
        window.addPropertyChangeListener("graphicsConfiguration", evt -> {
            if (
                evt.getNewValue() instanceof GraphicsConfiguration newGc && refresh(
                    newGc.getDefaultTransform()
                        .getScaleX())
            ) {
                window.revalidate();
                window.repaint();
            }
        });
    }

    private static synchronized boolean refresh(double scale) {
        if (scale == currentScale) return false;
        currentScale = scale;
        recompute();
        return true;
    }

    private static void recompute() {
        gapHair = scale(UiMetrics.GAP_HAIR);
        gapTiny = scale(UiMetrics.GAP_TINY);
        gapToolbarGroup = scale(UiMetrics.GAP_TOOLBAR_GROUP);
        gapSmall = scale(UiMetrics.GAP_SMALL);
        gapMedium = scale(UiMetrics.GAP_MEDIUM);
        gapLarge = scale(UiMetrics.GAP_LARGE);
        gapXLarge = scale(UiMetrics.GAP_XLARGE);
        gapXXLarge = scale(UiMetrics.GAP_XXLARGE);
        gapHuge = scale(UiMetrics.GAP_HUGE);

        toolbarIconSize = scale(UiMetrics.TOOLBAR_ICON_SIZE);
        fontHeadingDelta = scale(UiMetrics.FONT_HEADING_DELTA);
        scrollUnitIncrement = scale(UiMetrics.SCROLL_UNIT_INCREMENT);

        splitDividerLeft = scale(UiMetrics.SPLIT_DIVIDER_LEFT);
        splitDividerRight = scale(UiMetrics.SPLIT_DIVIDER_RIGHT);

        importDialogWidth = scale(UiMetrics.IMPORT_DIALOG_WIDTH);
        importDialogHeight = scale(UiMetrics.IMPORT_DIALOG_HEIGHT);

        mainWindowWidth = scale(UiMetrics.MAIN_WINDOW_WIDTH);
        mainWindowHeight = scale(UiMetrics.MAIN_WINDOW_HEIGHT);
    }

    private static int scale(int value) {
        return Math.max(1, (int) Math.round(value * currentScale));
    }
}
