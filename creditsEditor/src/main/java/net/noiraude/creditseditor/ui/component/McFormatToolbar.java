package net.noiraude.creditseditor.ui.component;

import java.awt.*;
import java.util.EnumSet;

import javax.swing.*;

/**
 * Compact formatting toolbar for Minecraft {@code §x} codes.
 *
 * <p>
 * Contains 16 palette-color swatches (§0–§f) and toggle buttons for bold ({@code §l}), italic
 * ({@code §o}), underline ({@code §n}), strikethrough ({@code §m}), obfuscated ({@code §k}), and
 * a reset ({@code §r}) button.
 *
 * <p>
 * Connect to a {@link McWysiwygPane} via {@link #connectTo}: button states track the caret
 * position and button clicks apply formatting to the selection (or to the input style when there
 * is no selection).
 */
public final class McFormatToolbar extends JPanel {

    private final JToggleButton[] colorButtons = new JToggleButton[16];
    private final ButtonGroup colorGroup = new ButtonGroup();
    private final JToggleButton boldBtn = modBtn("B", McFormatCode.BOLD.displayName(), EnumSet.of(FontStyle.BOLD));
    private final JToggleButton italicBtn = modBtn(
        "I",
        McFormatCode.ITALIC.displayName(),
        EnumSet.of(FontStyle.ITALIC));
    private final JToggleButton underlineBtn = modBtn(
        "U",
        McFormatCode.UNDERLINE.displayName(),
        EnumSet.noneOf(FontStyle.class));
    private final JToggleButton strikeBtn = modBtn(
        "S",
        McFormatCode.STRIKETHROUGH.displayName(),
        EnumSet.noneOf(FontStyle.class));
    private final JToggleButton obfBtn = modBtn(
        "K",
        McFormatCode.OBFUSCATED.displayName(),
        EnumSet.noneOf(FontStyle.class));

    private McWysiwygPane target;
    private boolean updatingState;

    public McFormatToolbar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        JButton resetBtn = buildColorSwatches();
        wireListeners(resetBtn);
    }

    private JButton buildColorSwatches() {
        for (int i = 0; i < 16; i++) {
            final McFormatCode mc = McFormatCode.COLORS.get(i);
            JToggleButton btn = new JToggleButton(new ColorIcon(mc.color, false));
            btn.setSelectedIcon(new ColorIcon(mc.color, true));
            btn.setToolTipText(mc.displayName());
            btn.setMargin(new Insets(1, 1, 1, 1));
            btn.setFocusable(false);
            colorGroup.add(btn);
            colorButtons[i] = btn;
            add(btn);
            if (i == 7) add(Box.createHorizontalStrut(3));
            btn.addActionListener(e -> apply(mc, true));
        }

        add(Box.createHorizontalStrut(6));
        for (JToggleButton mb : new JToggleButton[] { boldBtn, italicBtn, underlineBtn, strikeBtn, obfBtn }) {
            add(mb);
        }
        add(Box.createHorizontalStrut(4));

        JButton resetBtn = new JButton("§r");
        resetBtn.setMargin(new Insets(1, 4, 1, 4));
        resetBtn.setFocusable(false);
        resetBtn.setToolTipText(McFormatCode.RESET.displayName());
        add(resetBtn);
        return resetBtn;
    }

    private void wireListeners(JButton resetBtn) {
        boldBtn.addActionListener(e -> apply(McFormatCode.BOLD, boldBtn.isSelected()));
        italicBtn.addActionListener(e -> apply(McFormatCode.ITALIC, italicBtn.isSelected()));
        underlineBtn.addActionListener(e -> apply(McFormatCode.UNDERLINE, underlineBtn.isSelected()));
        strikeBtn.addActionListener(e -> apply(McFormatCode.STRIKETHROUGH, strikeBtn.isSelected()));
        obfBtn.addActionListener(e -> apply(McFormatCode.OBFUSCATED, obfBtn.isSelected()));
        resetBtn.addActionListener(e -> { if (target != null) target.applyReset(); });
    }

    private void apply(McFormatCode mc, boolean active) {
        if (!updatingState && target != null) target.applyCode(mc, active);
    }

    /**
     * Connects this toolbar to a {@link McWysiwygPane}: installs a caret listener to keep button
     * states in sync and routes apply operations back to the pane.
     */
    void connectTo(McWysiwygPane pane) {
        this.target = pane;
        pane.addCaretListener(e -> updateState());
        updateState();
    }

    private void updateState() {
        if (target == null) return;
        EnumSet<McFormatCode> active = McFormatCode.fromAttributes(target.getCaretAttributes());
        updatingState = true;
        try {
            boolean colorMatched = false;
            for (int i = 0; i < 16; i++) {
                boolean sel = active.contains(McFormatCode.COLORS.get(i));
                colorButtons[i].setSelected(sel);
                if (sel) colorMatched = true;
            }
            if (!colorMatched) colorGroup.clearSelection();
            boldBtn.setSelected(active.contains(McFormatCode.BOLD));
            italicBtn.setSelected(active.contains(McFormatCode.ITALIC));
            underlineBtn.setSelected(active.contains(McFormatCode.UNDERLINE));
            strikeBtn.setSelected(active.contains(McFormatCode.STRIKETHROUGH));
            obfBtn.setSelected(active.contains(McFormatCode.OBFUSCATED));
        } finally {
            updatingState = false;
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
        };

        abstract Font apply(Font f);
    }

    private static JToggleButton modBtn(String text, String tooltip, EnumSet<FontStyle> styles) {
        JToggleButton btn = new JToggleButton(text);
        Font font = btn.getFont();
        for (FontStyle s : styles) font = s.apply(font);
        btn.setFont(font);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(1, 4, 1, 4));
        btn.setFocusable(false);
        return btn;
    }

    // ------------------------------------------------------------------
    // ColorIcon
    // ------------------------------------------------------------------

    private static final class ColorIcon implements Icon {

        private final Color fill;
        private final boolean selected;

        ColorIcon(Color fill, boolean selected) {
            this.fill = fill;
            this.selected = selected;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(fill);
            g.fillRect(x + 1, y + 1, 12, 12);
            if (selected) {
                g.setColor(Color.WHITE);
                g.drawRect(x, y, 13, 13);
            } else {
                g.setColor(fill.darker());
                g.drawRect(x + 1, y + 1, 11, 11);
            }
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }
}
