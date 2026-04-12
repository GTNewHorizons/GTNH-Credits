package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.swing.*;

/**
 * Compact formatting toolbar for Minecraft {@code §x} codes.
 *
 * <p>
 * Contains 16 palette-color swatches (§0-§f) and toggle buttons for bold ({@code §l}), italic
 * ({@code §o}), underline ({@code §n}), strikethrough ({@code §m}), obfuscated ({@code §k}), and
 * a reset ({@code §r}) button.
 *
 * <p>
 * Connect to a {@link McWysiwygPane} via {@link #connectTo}: button states track the caret
 * position and button clicks apply formatting to the selection (or to the input style when there
 * is no selection).
 *
 * <p>
 * When a selection spans characters with mixed formatting, buttons reflect the indeterminate
 * state: modifier toggle buttons show a translucent gray overlay, and color swatches show a small
 * corner square indicator.
 */
public final class McFormatToolbar extends JPanel {

    private final JToggleButton[] colorButtons = new JToggleButton[16];
    // Pre-created icon pairs per color: [colorIndex][0=plain, 1=mixed]
    private final Icon[][] colorDieselIcons = new Icon[16][2];
    private final Icon[][] colorSelIcons = new Icon[16][2];

    // Pre-created NullColorIcon variants: [selected][mixed]
    private final NullColorIcon[][] nullColorIcons;

    private final JToggleButton noColorBtn = new JToggleButton();

    private final ButtonGroup colorGroup = new ButtonGroup();
    private final EnumMap<McFormatCode, MixedStateToggleButton> modifierButtons;

    private McFormatTarget target;
    private boolean updatingState;

    public McFormatToolbar() {
        int iconSize = scaled(14);
        nullColorIcons = new NullColorIcon[][] {
            { new NullColorIcon(false, false, iconSize), new NullColorIcon(false, true, iconSize) },
            { new NullColorIcon(true, false, iconSize), new NullColorIcon(true, true, iconSize) } };
        modifierButtons = buildModifierButtons();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        JButton resetBtn = buildColorSwatches(iconSize);
        wireListeners(resetBtn);
    }

    private static EnumMap<McFormatCode, MixedStateToggleButton> buildModifierButtons() {
        EnumMap<McFormatCode, MixedStateToggleButton> buttons = new EnumMap<>(McFormatCode.class);
        buttons.put(McFormatCode.BOLD, modBtn("B", McFormatCode.BOLD.displayName(), EnumSet.of(FontStyle.BOLD)));
        buttons.put(McFormatCode.ITALIC, modBtn("I", McFormatCode.ITALIC.displayName(), EnumSet.of(FontStyle.ITALIC)));
        buttons.put(
            McFormatCode.UNDERLINE,
            modBtn("U", McFormatCode.UNDERLINE.displayName(), EnumSet.of(FontStyle.UNDERLINE)));
        buttons.put(
            McFormatCode.STRIKETHROUGH,
            modBtn("S", McFormatCode.STRIKETHROUGH.displayName(), EnumSet.of(FontStyle.STRIKETHROUGH)));
        buttons.put(
            McFormatCode.OBFUSCATED,
            modBtn("K", McFormatCode.OBFUSCATED.displayName(), EnumSet.noneOf(FontStyle.class)));
        return buttons;
    }

    private JButton buildColorSwatches(int iconSize) {
        for (int i = 0; i < 16; i++) {
            final McFormatCode mc = McFormatCode.COLORS.get(i);
            colorDieselIcons[i][0] = new ColorIcon(mc.color, false, false, iconSize);
            colorDieselIcons[i][1] = new ColorIcon(mc.color, false, true, iconSize);
            colorSelIcons[i][0] = new ColorIcon(mc.color, true, false, iconSize);
            colorSelIcons[i][1] = new ColorIcon(mc.color, true, true, iconSize);

            JToggleButton btn = new JToggleButton(colorDieselIcons[i][0]);
            btn.setSelectedIcon(colorSelIcons[i][0]);
            btn.setToolTipText(mc.displayName());
            btn.setMargin(new Insets(scaled(1), scaled(1), scaled(1), scaled(1)));
            btn.setFocusable(false);
            colorGroup.add(btn);
            colorButtons[i] = btn;
            add(btn);
            if (i == 7) add(Box.createHorizontalStrut(scaled(3)));
            btn.addActionListener(e -> apply(mc, true));
        }

        noColorBtn.setIcon(nullColorIcons[0][0]);
        noColorBtn.setSelectedIcon(nullColorIcons[1][0]);
        noColorBtn.setToolTipText("Default color (no color attribute)");
        noColorBtn.setMargin(new Insets(scaled(1), scaled(1), scaled(1), scaled(1)));
        noColorBtn.setFocusable(false);
        colorGroup.add(noColorBtn);
        add(noColorBtn);

        add(Box.createHorizontalStrut(scaled(6)));
        for (MixedStateToggleButton mb : modifierButtons.values()) {
            add(mb);
        }
        add(Box.createHorizontalStrut(scaled(4)));

        JButton resetBtn = new JButton("§r");
        resetBtn.setMargin(new Insets(scaled(1), scaled(4), scaled(1), scaled(4)));
        resetBtn.setFocusable(false);
        resetBtn.setToolTipText(McFormatCode.RESET.displayName());
        add(resetBtn);
        return resetBtn;
    }

    private void wireListeners(JButton resetBtn) {
        for (Map.Entry<McFormatCode, MixedStateToggleButton> entry : modifierButtons.entrySet()) {
            McFormatCode mc = entry.getKey();
            MixedStateToggleButton btn = entry.getValue();
            btn.addActionListener(e -> apply(mc, btn.isSelected()));
        }
        resetBtn.addActionListener(e -> { if (target != null) target.applyReset(); });
        noColorBtn.addActionListener(e -> { if (target != null) target.applyColorReset(); });
    }

    private void apply(McFormatCode mc, boolean active) {
        if (!updatingState && target != null) target.applyCode(mc, active);
    }

    /**
     * Connects this toolbar to a {@link McWysiwygPane}: installs a caret listener to keep button
     * states in sync and routes apply operations back to the pane.
     */
    void connectTo(McFormatTarget pane) {
        this.target = pane;
        pane.addCaretListener(e -> updateState());
        updateState();
    }

    private void updateState() {
        if (target == null) return;
        updatingState = true;
        try {
            if (target.hasSelection()) {
                applyPresence(target.computeSelectionPresence());
            } else {
                applyActive(McFormatCode.fromAttributes(target.getCaretAttributes()));
            }
        } finally {
            updatingState = false;
        }
    }

    /** Updates button states to reflect per-code mixed/all/none presence across a selection. */
    private void applyPresence(McFormatCode.SelectionPresence presence) {
        boolean colorMatched = applyColorSwatchesPresence(presence);
        colorMatched |= applyNoColorButtonPresence(presence);
        if (!colorMatched) colorGroup.clearSelection();
        applyModifierPresence(presence);
    }

    private boolean applyColorSwatchesPresence(McFormatCode.SelectionPresence presence) {
        boolean colorMatched = false;
        for (int i = 0; i < 16; i++) {
            McFormatCode mc = McFormatCode.COLORS.get(i);
            boolean inAll = presence.all.contains(mc);
            boolean inAny = presence.any.contains(mc);
            int mixedIdx = inAny && !inAll ? 1 : 0;
            colorButtons[i].setIcon(colorDieselIcons[i][mixedIdx]);
            colorButtons[i].setSelectedIcon(colorSelIcons[i][mixedIdx]);
            colorButtons[i].setSelected(inAll);
            if (inAll) colorMatched = true;
        }
        return colorMatched;
    }

    private boolean applyNoColorButtonPresence(McFormatCode.SelectionPresence presence) {
        boolean anyColorInAny = McFormatCode.activeColor(presence.any) != null;
        boolean noColorAll = presence.anyLacksColor && !anyColorInAny;
        int mixedIdx = presence.anyLacksColor && anyColorInAny ? 1 : 0;
        noColorBtn.setIcon(nullColorIcons[0][mixedIdx]);
        noColorBtn.setSelectedIcon(nullColorIcons[1][mixedIdx]);
        noColorBtn.setSelected(noColorAll);
        return noColorAll;
    }

    private void applyModifierPresence(McFormatCode.SelectionPresence presence) {
        for (Map.Entry<McFormatCode, MixedStateToggleButton> entry : modifierButtons.entrySet()) {
            McFormatCode mc = entry.getKey();
            entry.getValue()
                .setPresence(presence.all.contains(mc), presence.any.contains(mc));
        }
    }

    /** Updates button states to reflect a uniform (no-selection) attribute set. */
    private void applyActive(EnumSet<McFormatCode> active) {
        boolean colorMatched = false;
        for (int i = 0; i < 16; i++) {
            boolean sel = active.contains(McFormatCode.COLORS.get(i));
            colorButtons[i].setIcon(colorDieselIcons[i][0]);
            colorButtons[i].setSelectedIcon(colorSelIcons[i][0]);
            colorButtons[i].setSelected(sel);
            if (sel) colorMatched = true;
        }
        boolean noColor = McFormatCode.activeColor(active) == null;
        noColorBtn.setIcon(nullColorIcons[0][0]);
        noColorBtn.setSelectedIcon(nullColorIcons[1][0]);
        noColorBtn.setSelected(noColor);
        if (noColor) colorMatched = true;
        if (!colorMatched) colorGroup.clearSelection();
        for (Map.Entry<McFormatCode, MixedStateToggleButton> entry : modifierButtons.entrySet()) {
            entry.getValue()
                .setPresence(active.contains(entry.getKey()), false);
        }
    }

    private enum FontStyle {

        BOLD {

            @Override
            Font apply(Font f) {
                return f.deriveFont(Font.BOLD);
            }
        },
        ITALIC {

            @Override
            Font apply(Font f) {
                return f.deriveFont(Font.ITALIC);
            }
        },
        UNDERLINE {

            @Override
            Font apply(Font f) {
                return f.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
            }
        },
        STRIKETHROUGH {

            @Override
            Font apply(Font f) {
                return f
                    .deriveFont(Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON));
            }
        };

        abstract Font apply(Font f);
    }

    private static MixedStateToggleButton modBtn(String text, String tooltip, EnumSet<FontStyle> styles) {
        MixedStateToggleButton btn = new MixedStateToggleButton(text);
        Font font = btn.getFont();
        for (FontStyle s : styles) font = s.apply(font);
        btn.setFont(font);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(scaled(1), scaled(4), scaled(1), scaled(4)));
        btn.setFocusable(false);
        return btn;
    }

    // ------------------------------------------------------------------
    // MixedStateToggleButton
    // ------------------------------------------------------------------

    /**
     * A {@link JToggleButton} with a mixed (indeterminate) state in addition to the normal
     * selected and deselected states.
     *
     * <p>
     * When mixed, the button renders as deselected but with a translucent gray overlay to signal
     * that some but not all characters in the selection carry the associated attribute.
     */
    private static final class MixedStateToggleButton extends JToggleButton {

        private boolean mixed;

        MixedStateToggleButton(String text) {
            super(text);
        }

        /**
         * Sets the button state. {@code inAll} true means selected; {@code inAny && !inAll}
         * means mixed (indeterminate).
         */
        void setPresence(boolean inAll, boolean inAny) {
            this.mixed = inAny && !inAll;
            setSelected(inAll);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (mixed) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.setColor(Color.GRAY);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // ColorIcon
    // ------------------------------------------------------------------

    private record ColorIcon(Color fill, boolean selected, boolean mixed, int size) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int inner = size - 2;
            int pipFill = Math.max(2, size * 2 / 7);
            int pipOffset = size - pipFill - 1;
            g.setColor(fill);
            g.fillRect(x + 1, y + 1, inner, inner);
            g.setColor(fill.darker());
            g.drawRect(x + 1, y + 1, inner - 1, inner - 1);
            if (selected) {
                // Selection pip: small white square with dark border in the bottom-left corner
                g.setColor(Color.WHITE);
                g.fillRect(x + 1, y + pipOffset, pipFill, pipFill);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x + 1, y + pipOffset, pipFill - 1, pipFill - 1);
            }
            if (mixed) {
                // Mixed pip: small white square with dark border in the bottom-right corner
                g.setColor(Color.WHITE);
                g.fillRect(x + pipOffset, y + pipOffset, pipFill, pipFill);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x + pipOffset, y + pipOffset, pipFill - 1, pipFill - 1);
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    // ------------------------------------------------------------------
    // NullColorIcon
    // ------------------------------------------------------------------

    /**
     * Icon for the "no color / default color" button: a light gray swatch with a crossed X.
     */
    private record NullColorIcon(boolean selected, boolean mixed, int size) implements Icon {

        private static final Color BG = new Color(210, 210, 210);
        private static final Color CROSS = new Color(90, 90, 90);
        private static final Color BORDER = new Color(150, 150, 150);

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int inner = size - 2;
            int pipFill = Math.max(2, size * 2 / 7);
            int pipOffset = size - pipFill - 1;
            int crossMargin = Math.max(1, size * 3 / 14);
            int crossFar = size - crossMargin;
            g.setColor(BG);
            g.fillRect(x + 1, y + 1, inner, inner);
            g.setColor(CROSS);
            g.drawLine(x + crossMargin, y + crossMargin, x + crossFar, y + crossFar);
            g.drawLine(x + crossFar, y + crossMargin, x + crossMargin, y + crossFar);
            g.setColor(BORDER);
            g.drawRect(x + 1, y + 1, inner - 1, inner - 1);
            if (selected) {
                // Selection pip: small white square with dark border in the bottom-left corner
                g.setColor(Color.WHITE);
                g.fillRect(x + 1, y + pipOffset, pipFill, pipFill);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x + 1, y + pipOffset, pipFill - 1, pipFill - 1);
            }
            if (mixed) {
                g.setColor(Color.WHITE);
                g.fillRect(x + pipOffset, y + pipOffset, pipFill, pipFill);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x + pipOffset, y + pipOffset, pipFill - 1, pipFill - 1);
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
