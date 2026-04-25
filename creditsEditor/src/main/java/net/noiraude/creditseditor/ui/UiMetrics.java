package net.noiraude.creditseditor.ui;

/**
 * Semantic raw-pixel layout constants. Wrap with {@link UiScale#scaled(int)} at the call site
 * so values track the current UI font scale dynamically.
 */
public final class UiMetrics {

    private UiMetrics() {}

    public static final int GAP_HAIR = 1;
    public static final int GAP_TINY = 2;
    public static final int GAP_TOOLBAR_GROUP = 3;
    public static final int GAP_SMALL = 4;
    public static final int GAP_MEDIUM = 6;
    public static final int GAP_LARGE = 8;
    public static final int GAP_XLARGE = 10;
    public static final int GAP_XXLARGE = 12;
    public static final int GAP_HUGE = 16;

    public static final int TOOLBAR_ICON_SIZE = 14;
    public static final int FONT_HEADING_DELTA = 6;
    public static final int SCROLL_UNIT_INCREMENT = 16;

    public static final int SPLIT_DIVIDER_LEFT = 200;
    public static final int SPLIT_DIVIDER_RIGHT = 250;

    public static final int IMPORT_DIALOG_WIDTH = 600;
    public static final int IMPORT_DIALOG_HEIGHT = 450;

    public static final int MAIN_WINDOW_WIDTH = 1100;
    public static final int MAIN_WINDOW_HEIGHT = 700;
}
