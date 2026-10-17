package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.service.RoleIndex;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.MsgArg;
import net.noiraude.creditseditor.ui.component.AnyChangeListener;
import net.noiraude.creditseditor.ui.component.KeyHintLabel;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentRole;

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
    private final JCheckBox external = new JCheckBox(I18n.get("role.external.use"));
    private final JTextField search = new JTextField();
    private final List<String> translationKeys;
    private final DocumentBus bus;

    AddRolePrompt(@NotNull DocumentMembership membership, @Nullable CreditsDocument creditsDoc,
        @NotNull DocumentBus bus) {
        this.membership = membership;
        this.bus = bus;
        this.translationKeys = bus.externalTranslationKeys();
        this.index = creditsDoc != null ? RoleIndex.build(creditsDoc) : null;
        combo.setEditable(true);

        JPanel below = new JPanel();
        below.setLayout(new BoxLayout(below, BoxLayout.Y_AXIS));
        below.setBorder(BorderFactory.createEmptyBorder(gapSmall, 0, 0, 0));
        JTextComponent editor = (JTextComponent) combo.getEditor()
            .getEditorComponent();
        KeyHintLabel keyHint = new KeyHintLabel(editor);
        below.add(keyHint);
        below.add(Box.createVerticalStrut(gapSmall));
        below.add(showAll);
        below.add(external);
        below.add(new JLabel(I18n.get("role.external.search")));
        below.add(search);
        search.setEnabled(false);
        search.getDocument()
            .addDocumentListener(new AnyChangeListener(this::populate));
        external.addActionListener(e -> {
            search.setEnabled(external.isSelected());
            showAll.setVisible(!external.isSelected());
            keyHint.setVisible(!external.isSelected());
            populate();
        });

        content = new JPanel(new BorderLayout(0, gapSmall));
        content.add(combo, BorderLayout.CENTER);
        content.add(below, BorderLayout.SOUTH);
        populate();
        showAll.addActionListener(e -> populate());
    }

    private void populate() {
        String typed = java.util.Objects.toString(
            combo.getEditor()
                .getItem(),
            "");
        combo.removeAllItems();
        if (external.isSelected()) {
            String query = search.getText()
                .toLowerCase(java.util.Locale.ROOT);
            translationKeys.stream()
                .filter(
                    key -> key.toLowerCase(java.util.Locale.ROOT)
                        .contains(query)
                        || bus.externalTranslation(key, "en_US")
                            .orElse("")
                            .toLowerCase(java.util.Locale.ROOT)
                            .contains(query))
                .limit(200)
                .forEach(combo::addItem);
        } else if (index != null) {
            for (RoleIndex.Entry entry : index.entries()) {
                if (
                    !entry.raw.isExternal() && !membership.roles.contains(entry.raw)
                        && (showAll.isSelected() || entry.categoryIds.contains(membership.categoryId))
                ) {
                    combo.addItem(entry.raw.value());
                }
            }
        }
        combo.getEditor()
            .setItem(typed);
    }

    Optional<DocumentRole> show(@NotNull Component parent) {
        int result = JOptionPane.showConfirmDialog(
            parent,
            content,
            I18n.get("dialog.add_role.title", MsgArg.text(membership.categoryId)),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return Optional.empty();
        String role = ((String) combo.getEditor()
            .getItem()).strip();
        if (role.isEmpty()) return Optional.empty();
        try {
            return Optional.of(external.isSelected() ? DocumentRole.translation(role) : DocumentRole.local(role));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                parent,
                ex.getMessage(),
                I18n.get("dialog.load.error.title"),
                JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
    }
}
