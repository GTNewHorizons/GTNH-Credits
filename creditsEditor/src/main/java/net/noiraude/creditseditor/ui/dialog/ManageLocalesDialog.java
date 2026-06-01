package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapLarge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddLocaleCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.command.impl.RemoveLocaleCommand;
import net.noiraude.creditseditor.service.KnownLocales;
import net.noiraude.creditseditor.service.LocaleChoice;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.creditseditor.ui.MsgArg;
import net.noiraude.creditseditor.ui.component.dnd.LocaleSetMoveTransferHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Window for installing and uninstalling editing locales. */
public final class ManageLocalesDialog extends JDialog {

    private final @NotNull DocumentBus bus;
    private final @NotNull LocaleEditor editor;
    private final @NotNull CommandExecutor onCommand;

    private final @NotNull DefaultListModel<LocaleChoice> installedModel = new DefaultListModel<>();
    private final @NotNull DefaultListModel<LocaleChoice> possibleModel = new DefaultListModel<>();
    private final @NotNull JList<LocaleChoice> installedList = new JList<>(installedModel);
    private final @NotNull JList<LocaleChoice> possibleList = new JList<>(possibleModel);
    private final @NotNull JButton installButton = new JButton("←");
    private final @NotNull JButton uninstallButton = new JButton("→");

    private final @NotNull PropertyChangeListener localeListener = e -> rebuild();

    public ManageLocalesDialog(@Nullable Frame owner, @NotNull DocumentBus bus, @NotNull LocaleEditor editor,
        @NotNull CommandExecutor onCommand) {
        super(owner, I18n.get("dialog.manage_locales.title"), true);
        this.bus = bus;
        this.editor = editor;
        this.onCommand = onCommand;

        JPanel content = new JPanel(new BorderLayout(gapLarge, gapLarge));
        content.setBorder(BorderFactory.createEmptyBorder(gapLarge, gapLarge, gapLarge, gapLarge));
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);
        setContentPane(content);

        installCloseOnEscape();
        wireListeners();
        bus.addListener(DocumentBus.TOPIC_LOCALE, localeListener);
        addWindowListener(new BusListenerCleanup(bus, localeListener));

        rebuild();
        pack();
        setLocationRelativeTo(owner);
    }

    private @NotNull JComponent buildBody() {
        installedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        installedList.setVisibleRowCount(15);
        installedList.setDragEnabled(true);
        installedList.setDropMode(DropMode.ON);
        installedList.setTransferHandler(new LocaleSetMoveTransferHandler(installedList, this::requestInstall));

        possibleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        possibleList.setVisibleRowCount(15);
        possibleList.setDragEnabled(true);
        possibleList.setDropMode(DropMode.ON);
        possibleList.setTransferHandler(new LocaleSetMoveTransferHandler(possibleList, this::requestUninstall));

        JPanel body = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, gapSmall, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0.5;
        body.add(label("dialog.manage_locales.installed.label", installedList), c);
        c.gridx = 1;
        c.weightx = 0;
        body.add(Box.createHorizontalStrut(gapLarge), c);
        c.gridx = 2;
        c.weightx = 0.5;
        body.add(label("dialog.manage_locales.possible.label", possibleList), c);

        c.gridy = 1;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = 0.5;
        body.add(new JScrollPane(installedList), c);

        c.gridx = 1;
        c.weightx = 0;
        body.add(buildArrowStrip(), c);

        c.gridx = 2;
        c.weightx = 0.5;
        body.add(new JScrollPane(possibleList), c);
        return body;
    }

    private @NotNull JComponent buildArrowStrip() {
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
        installButton.setToolTipText(I18n.get("dialog.manage_locales.install.tooltip"));
        uninstallButton.setToolTipText(I18n.get("dialog.manage_locales.uninstall.tooltip"));
        installButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        uninstallButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        strip.add(Box.createVerticalGlue());
        strip.add(installButton);
        strip.add(Box.createVerticalStrut(gapSmall));
        strip.add(uninstallButton);
        strip.add(Box.createVerticalGlue());
        return strip;
    }

    private @NotNull JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton closeButton = new JButton(I18n.get("button.close"));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        getRootPane().setDefaultButton(closeButton);
        return panel;
    }

    private static @NotNull JLabel label(@NotNull String key, @NotNull JComponent target) {
        JLabel l = new JLabel(I18n.get(key), SwingConstants.LEFT);
        l.setLabelFor(target);
        return l;
    }

    private void installCloseOnEscape() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void wireListeners() {
        installedList.addListSelectionListener(e -> refreshButtons());
        possibleList.addListSelectionListener(e -> refreshButtons());
        installButton.addActionListener(e -> requestInstall(basenames(possibleList.getSelectedValuesList())));
        uninstallButton.addActionListener(e -> requestUninstall(basenames(installedList.getSelectedValuesList())));
        refreshButtons();
    }

    private void refreshButtons() {
        installButton.setEnabled(!possibleList.isSelectionEmpty());
        uninstallButton.setEnabled(!installedList.isSelectionEmpty());
    }

    private void rebuild() {
        Set<String> installed = new HashSet<>(bus.availableLocales());
        List<LocaleChoice> installedChoices = new ArrayList<>(installed.size());
        for (String code : installed) {
            installedChoices.add(KnownLocales.choiceFor(code));
        }
        installedChoices
            .sort(Comparator.comparing(LocaleChoice::displayName, Collator.getInstance(Locale.getDefault())));
        installedModel.clear();
        for (LocaleChoice choice : installedChoices) installedModel.addElement(choice);

        possibleModel.clear();
        for (LocaleChoice choice : KnownLocales.jdkBasenamesExcluding(installed)) {
            possibleModel.addElement(choice);
        }
        refreshButtons();
    }

    private void requestInstall(@NotNull List<String> codes) {
        if (codes.isEmpty()) return;
        List<LocaleChoice> choices = codes.stream()
            .map(KnownLocales::choiceFor)
            .toList();
        if (!confirm("install", choices)) return;
        dispatch(choices, true);
    }

    private void requestUninstall(@NotNull List<String> codes) {
        if (codes.isEmpty()) return;
        List<LocaleChoice> choices = codes.stream()
            .map(KnownLocales::choiceFor)
            .toList();
        if (!confirm("uninstall", choices)) return;
        dispatch(choices, false);
    }

    private boolean confirm(@NotNull String kind, @NotNull List<LocaleChoice> choices) {
        String title = I18n.get("dialog.manage_locales.confirm." + kind + ".title");
        String message = I18n
            .get("dialog.manage_locales.confirm." + kind + ".message", MsgArg.text(formatList(choices)));
        int result = JOptionPane
            .showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.OK_OPTION;
    }

    private void dispatch(@NotNull List<LocaleChoice> choices, boolean install) {
        String key = install ? "command.install_locales" : "command.uninstall_locales";
        if (choices.size() == 1) {
            String code = choices.getFirst()
                .basename();
            Command single = install ? AddLocaleCommand.create(editor, bus, code)
                : RemoveLocaleCommand.create(editor, bus, code);
            onCommand.execute(single);
            return;
        }
        CompoundCommand.Builder builder = new CompoundCommand.Builder(I18n.get(key, MsgArg.count(choices.size())));
        for (LocaleChoice choice : choices) {
            String code = choice.basename();
            builder.add(
                install ? AddLocaleCommand.create(editor, bus, code) : RemoveLocaleCommand.create(editor, bus, code));
        }
        onCommand.execute(builder.build());
    }

    private static @NotNull String formatList(@NotNull List<LocaleChoice> choices) {
        StringBuilder out = new StringBuilder();
        for (LocaleChoice choice : choices) {
            if (out.length() > 0) out.append('\n');
            out.append(choice.basename())
                .append("  ")
                .append(choice.displayName());
        }
        return out.toString();
    }

    private static @NotNull List<String> basenames(@NotNull List<LocaleChoice> values) {
        return values.stream()
            .map(LocaleChoice::basename)
            .toList();
    }

    private static final class BusListenerCleanup extends WindowAdapter {

        private final @NotNull DocumentBus bus;
        private final @NotNull PropertyChangeListener listener;

        BusListenerCleanup(@NotNull DocumentBus bus, @NotNull PropertyChangeListener listener) {
            this.bus = Objects.requireNonNull(bus);
            this.listener = Objects.requireNonNull(listener);
        }

        @Override
        public void windowClosed(WindowEvent e) {
            bus.removeListener(DocumentBus.TOPIC_LOCALE, listener);
        }
    }
}
