package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import net.noiraude.creditseditor.service.RoleIndex;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.component.KeyHintLabel;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal prompt for entering a new role on a membership. Combines a free-text editable combo
 * box with a "show all roles" toggle that switches between roles already used in the same
 * category and the full role index of the document.
 */
final class AddRolePrompt {

    private final @NotNull DocumentMembership membership;
    private final @Nullable RoleIndex index;
    private final @NotNull JComboBox<String> combo = new JComboBox<>();
    private final @NotNull JCheckBox showAll = new JCheckBox(I18n.get("dialog.add_role.show_all"));
    private final @NotNull JPanel content;

    AddRolePrompt(@NotNull DocumentMembership membership, @Nullable CreditsDocument creditsDoc) {
        this.membership = membership;
        this.index = creditsDoc != null ? RoleIndex.build(creditsDoc) : null;
        combo.setEditable(true);

        JPanel below = new JPanel();
        below.setLayout(new BoxLayout(below, BoxLayout.Y_AXIS));
        below.setBorder(BorderFactory.createEmptyBorder(gapSmall, 0, 0, 0));
        if (
            combo.getEditor()
                .getEditorComponent() instanceof JTextComponent editor
        ) {
            below.add(new KeyHintLabel(editor));
            below.add(Box.createVerticalStrut(gapSmall));
        }
        below.add(showAll);

        content = new JPanel(new BorderLayout(0, gapSmall));
        content.add(combo, BorderLayout.CENTER);
        content.add(below, BorderLayout.SOUTH);
        populate();
        showAll.addActionListener(e -> populate());
    }

    private void populate() {
        String typed = combo.getEditor()
            .getItem() instanceof String s ? s : "";
        combo.removeAllItems();
        if (index != null) {
            for (RoleIndex.Entry entry : index.entries()) {
                if (
                    !membership.roles.contains(entry.raw)
                        && (showAll.isSelected() || entry.categoryIds.contains(membership.categoryId))
                ) {
                    combo.addItem(entry.raw);
                }
            }
        }
        combo.getEditor()
            .setItem(typed);
    }

    @Nullable
    String show(@NotNull Component parent) {
        int result = JOptionPane.showConfirmDialog(
            parent,
            content,
            I18n.get("dialog.add_role.title", membership.categoryId),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        String role = ((String) combo.getEditor()
            .getItem()).strip();
        return role.isEmpty() ? null : role;
    }
}
