package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapMedium;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;

import net.noiraude.creditseditor.ui.component.MinecraftTextEditor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right-hand card shown beside the role list in {@link MembershipRolePanel}.
 *
 * <p>
 * Two CardLayout pages: a placeholder hint when zero or multiple roles are selected, and a
 * detail form (role value, lang key, display-name editor) when exactly one role is selected.
 */
public final class RoleDetailCard {

    private static final @NotNull String CARD_EMPTY = "empty";
    private static final @NotNull String CARD_DETAIL = "detail";

    private final @NotNull CardLayout cards = new CardLayout();
    final @NotNull JPanel panel = new JPanel(cards) {

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }
    };
    private final @NotNull JTextField roleValueField = new JTextField();
    private final @NotNull JLabel langKeyLabel = new JLabel();
    private final @NotNull MinecraftTextEditor displayNameEditor = new MinecraftTextEditor();
    private boolean loading;

    RoleDetailCard(@NotNull Consumer<String> onDisplayNameChanged,
        @NotNull Consumer<UndoableEditEvent> onUndoableEdit) {
        roleValueField.setEditable(false);
        roleValueField.setBackground(UIManager.getColor("Panel.background"));
        langKeyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        buildEmptyCard();
        buildDetailCard();
        clear();

        displayNameEditor.addPropertyChangeListener(
            MinecraftTextEditor.PROP_TEXT,
            e -> { if (!loading) onDisplayNameChanged.accept((String) e.getNewValue()); });
        displayNameEditor.addUndoableEditListener(e -> { if (!loading) onUndoableEdit.accept(e); });
    }

    @NotNull
    JPanel getPanel() {
        return panel;
    }

    private void buildEmptyCard() {
        JLabel hint = new JLabel("Select a single role to edit its display name");
        hint.setFont(
            hint.getFont()
                .deriveFont(Font.ITALIC));
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(hint, CARD_EMPTY);
    }

    private void buildDetailCard() {
        JPanel detail = new JPanel(new GridBagLayout());
        GridBagConstraints lbl = labelConstraints();
        GridBagConstraints fld = fieldConstraints();

        lbl.gridy = 0;
        detail.add(new JLabel("Role:"), lbl);
        fld.gridy = 0;
        detail.add(roleValueField, fld);

        lbl.gridy = 1;
        detail.add(new JLabel("Lang key:"), lbl);
        fld.gridy = 1;
        detail.add(langKeyLabel, fld);

        lbl.gridy = 2;
        lbl.anchor = GridBagConstraints.NORTHWEST;
        detail.add(new JLabel("Display name:"), lbl);
        fld.gridy = 2;
        detail.add(displayNameEditor, fld);

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridy = 3;
        spacer.gridx = 0;
        spacer.gridwidth = 2;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.VERTICAL;
        detail.add(Box.createVerticalGlue(), spacer);

        panel.add(detail, CARD_DETAIL);
    }

    void load(@NotNull String role, @NotNull String langKey, @Nullable String displayName) {
        loading = true;
        try {
            roleValueField.setText(role);
            langKeyLabel.setText(langKey);
            displayNameEditor.setText(displayName != null ? displayName : "");
        } finally {
            loading = false;
        }
        cards.show(panel, CARD_DETAIL);
    }

    void clear() {
        cards.show(panel, CARD_EMPTY);
    }

    @Contract(value = " -> new", pure = true)
    private static @NotNull GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(gapSmall, gapMedium, gapSmall, gapSmall);
        return c;
    }

    @Contract(value = " -> new", pure = true)
    private static @NotNull GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0;
        c.insets = new Insets(gapSmall, 0, gapSmall, gapMedium);
        return c;
    }
}
