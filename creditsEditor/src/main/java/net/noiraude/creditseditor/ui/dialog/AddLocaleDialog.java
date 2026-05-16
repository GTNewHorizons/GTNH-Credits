package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapLarge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Modal picker for a JDK-known locale to register in the session. */
public final class AddLocaleDialog extends JDialog {

    private final @NotNull List<LocaleChoice> allChoices;
    private final @NotNull DefaultListModel<LocaleChoice> model = new DefaultListModel<>();
    private final @NotNull JList<LocaleChoice> list = new JList<>(model);
    private final @NotNull JTextField filterField = new JTextField();
    private final @NotNull JButton okButton = new JButton(I18n.get("button.add"));

    private @Nullable String result;

    public AddLocaleDialog(@Nullable Frame owner, @NotNull Set<String> alreadyLoaded) {
        super(owner, I18n.get("dialog.add_locale.title"), true);
        this.allChoices = buildChoices(alreadyLoaded);

        JPanel content = new JPanel(new BorderLayout(gapLarge, gapLarge));
        content.setBorder(BorderFactory.createEmptyBorder(gapLarge, gapLarge, gapLarge, gapLarge));
        content.add(buildFilterPanel(), BorderLayout.NORTH);
        content.add(buildListPanel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);
        setContentPane(content);

        installCloseOnEscape();
        wireListeners();
        applyFilter("");

        pack();
        setLocationRelativeTo(owner);
    }

    /** Returns the basename of the picked locale, or empty when cancelled. */
    public @NotNull Optional<String> showDialog() {
        setVisible(true);
        return Optional.ofNullable(result);
    }

    private @NotNull JComponent buildFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel label = new JLabel(I18n.get("dialog.add_locale.filter.label"));
        label.setLabelFor(filterField);
        panel.add(label);
        panel.add(Box.createHorizontalStrut(gapSmall));
        panel.add(filterField);
        return panel;
    }

    private @NotNull JComponent buildListPanel() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(15);
        return new JScrollPane(list);
    }

    private @NotNull JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton cancelButton = new JButton(I18n.get("button.cancel"));
        cancelButton.addActionListener(e -> dispose());
        panel.add(okButton);
        panel.add(Box.createHorizontalStrut(gapSmall));
        panel.add(cancelButton);
        getRootPane().setDefaultButton(okButton);
        return panel;
    }

    private void installCloseOnEscape() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void wireListeners() {
        filterField.getDocument()
            .addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    applyFilter(filterField.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    applyFilter(filterField.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    applyFilter(filterField.getText());
                }
            });
        list.addListSelectionListener(e -> okButton.setEnabled(list.getSelectedValue() != null));
        okButton.addActionListener(e -> onOk());
    }

    private void applyFilter(@NotNull String typed) {
        String needle = typed.strip()
            .toLowerCase(Locale.ROOT);
        LocaleChoice previousSelection = list.getSelectedValue();
        model.clear();
        for (LocaleChoice choice : allChoices) {
            if (needle.isEmpty() || choice.matches(needle)) {
                model.addElement(choice);
            }
        }
        if (previousSelection != null && model.contains(previousSelection)) {
            list.setSelectedValue(previousSelection, true);
        } else if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        } else {
            list.clearSelection();
        }
        okButton.setEnabled(list.getSelectedValue() != null);
    }

    private void onOk() {
        LocaleChoice selected = list.getSelectedValue();
        if (selected == null) return;
        result = selected.basename();
        dispose();
    }

    private static @NotNull List<LocaleChoice> buildChoices(@NotNull Set<String> alreadyLoaded) {
        Locale uiLocale = Locale.getDefault();
        Collator collator = Collator.getInstance(uiLocale);
        Set<String> seenBasenames = new HashSet<>();
        List<LocaleChoice> choices = new ArrayList<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language.isEmpty() || country.isEmpty()) continue;
            String basename = language + "_" + country;
            if (alreadyLoaded.contains(basename)) continue;
            if (!seenBasenames.add(basename)) continue;
            choices.add(new LocaleChoice(basename, locale.getDisplayName(uiLocale)));
        }
        choices.sort(Comparator.comparing(LocaleChoice::displayName, collator));
        return choices;
    }

    private record LocaleChoice(@NotNull String basename, @NotNull String displayName) {

        boolean matches(@NotNull String lowerNeedle) {
            return basename.toLowerCase(Locale.ROOT)
                .contains(lowerNeedle)
                || displayName.toLowerCase(Locale.getDefault())
                    .contains(lowerNeedle);
        }

        @Override
        public @NotNull String toString() {
            return basename + "  " + displayName;
        }
    }
}
