package net.noiraude.creditseditor.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Read-only Swing component that renders a raw string containing Minecraft {@code §x} formatting
 * codes using Java2D.
 *
 * <p>
 * Bold, italic, underline, and strikethrough are composed from the standard {@link Font} API
 * and {@link Graphics2D} line drawing. Obfuscated segments ({@code §k}) cycle through random
 * characters every ~100 ms via a {@link Timer} that runs only while the component is showing, to
 * avoid background CPU usage.
 *
 * <p>
 * Suitable for use as a standalone label or wrapped as a list/table cell renderer.
 */
public final class MinecraftTextRenderer extends JComponent {

    private static final char[] OBF_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        .toCharArray();
    private static final Random RANDOM = new Random();

    private String raw = "";
    private List<McFormatCode.Segment> segments = Collections.emptyList();
    private boolean hasObfuscated = false;

    private final Timer obfTimer = new Timer(100, e -> repaint());

    public MinecraftTextRenderer() {
        obfTimer.setRepeats(true);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                updateObfTimer();
            }
        });
    }

    /** Sets the raw string (may contain {@code §x} codes) to display. */
    public void setText(String raw) {
        this.raw = (raw != null) ? raw : "";
        this.segments = McFormatCode.parse(this.raw);
        this.hasObfuscated = segments.stream()
            .anyMatch(s -> s.obfuscated);
        updateObfTimer();
        revalidate();
        repaint();
    }

    /** Returns the raw string currently displayed. */
    public String getText() {
        return raw;
    }

    private void updateObfTimer() {
        if (hasObfuscated && isShowing()) {
            if (!obfTimer.isRunning()) obfTimer.start();
        } else {
            obfTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (segments.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font baseFont = getFont();
            Color defaultFg = getForeground();
            FontMetrics baseFm = g2.getFontMetrics(baseFont);
            Insets ins = getInsets();
            int x = ins.left;
            int baseline = ins.top + baseFm.getAscent();

            for (McFormatCode.Segment seg : segments) {
                Font segFont = deriveFont(baseFont, seg);
                g2.setFont(segFont);
                FontMetrics fm = g2.getFontMetrics(segFont);

                g2.setColor(seg.color != null ? seg.color : defaultFg);

                String drawText = seg.obfuscated ? randomize(seg.text) : seg.text;
                g2.drawString(drawText, x, baseline);

                int w = fm.stringWidth(drawText);
                if (seg.underline) {
                    g2.drawLine(x, baseline + 1, x + w - 1, baseline + 1);
                }
                if (seg.strikethrough) {
                    int midY = baseline - fm.getAscent() / 2;
                    g2.drawLine(x, midY, x + w - 1, midY);
                }
                x += w;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        Font baseFont = getFont();
        FontMetrics baseFm = getFontMetrics(baseFont);
        int totalWidth = 0;
        for (McFormatCode.Segment seg : segments) {
            totalWidth += getFontMetrics(deriveFont(baseFont, seg)).stringWidth(seg.text);
        }
        Insets ins = getInsets();
        return new Dimension(ins.left + totalWidth + ins.right, ins.top + baseFm.getHeight() + ins.bottom);
    }

    private static Font deriveFont(Font base, McFormatCode.Segment seg) {
        int style = Font.PLAIN;
        if (seg.bold) style |= Font.BOLD;
        if (seg.italic) style |= Font.ITALIC;
        return (style == Font.PLAIN) ? base : base.deriveFont(style);
    }

    private static String randomize(String text) {
        char[] result = new char[text.length()];
        for (int i = 0; i < text.length(); i++) {
            result[i] = (text.charAt(i) == ' ') ? ' ' : OBF_CHARS[RANDOM.nextInt(OBF_CHARS.length)];
        }
        return new String(result);
    }
}
