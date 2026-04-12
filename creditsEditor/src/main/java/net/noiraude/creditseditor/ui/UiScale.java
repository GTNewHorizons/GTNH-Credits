package net.noiraude.creditseditor.ui;

import java.awt.Font;

import javax.swing.UIManager;

/** Utility for scaling pixel constants to the current UI font size. */
public final class UiScale {

    private UiScale() {}

    /**
     * Returns {@code value} multiplied by the ratio of the current UI label font size to the
     * 12pt baseline, rounded to the nearest integer and clamped to a minimum of 1.
     *
     * <p>
     * At 100% system scaling (12pt default label font), the return value equals the input. At 200%
     * (24pt) it doubles, and so on. Call this from Swing constructors or static initializers that
     * set pixel-based sizes such as {@link java.awt.Insets}, border widths, or split-pane divider
     * positions.
     */
    public static int scaled(int value) {
        Font font = UIManager.getFont("Label.font");
        float scale = font != null ? font.getSize() / 12f : 1f;
        return Math.max(1, Math.round(value * scale));
    }
}
