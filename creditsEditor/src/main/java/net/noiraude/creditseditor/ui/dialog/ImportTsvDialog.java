package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapLarge;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;
import static net.noiraude.creditseditor.ui.ScaledMetrics.importDialogHeight;
import static net.noiraude.creditseditor.ui.ScaledMetrics.importDialogWidth;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.service.TsvImportCommandFactory;
import net.noiraude.creditseditor.service.TsvImporter;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;
import net.noiraude.creditseditor.ui.I18n;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog for importing persons from a TSV file into a target category.
 *
 * <p>
 * Shows a file chooser, a target category selector, a preview table with the computed
 * action per line, and an import button. The entire import is dispatched as a single
 * {@link net.noiraude.creditseditor.command.impl.CompoundCommand} for atomic undo.
 */
public final class ImportTsvDialog extends JDialog {

    private final @NotNull DocumentBus bus;
    private final @NotNull CommandExecutor onCommand;
    private final @NotNull TsvPreviewController preview;

    private final @NotNull JTextField fileField = new JTextField();
    private final @NotNull JComboBox<String> categoryCombo = new JComboBox<>();
    private final @NotNull JButton importButton = new JButton(I18n.get("button.import"));
    private final @NotNull JLabel statusLabel = new JLabel(" ");

    private @Nullable File selectedFile;

    /**
     * @param defaultCategoryId category to pre-select, or {@code null} for the first entry
     */
    public ImportTsvDialog(@Nullable Frame owner, @NotNull DocumentBus bus, @NotNull CommandExecutor onCommand,
        @Nullable String defaultCategoryId) {
        super(owner, I18n.get("dialog.import_tsv.title"), true);
        this.bus = bus;
        this.onCommand = onCommand;
        this.preview = new TsvPreviewController(bus, this::onPreviewChanged, this::showReadError);

        setLayout(new BorderLayout(gapLarge, gapLarge));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(gapLarge, gapLarge, gapLarge, gapLarge));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildPreviewPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        populateCategoryCombo();
        preselectCategory(defaultCategoryId);
        updateImportButton();

        setSize(importDialogWidth, importDialogHeight);
        setLocationRelativeTo(owner);
    }

    private @NotNull JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints lbl = new GridBagConstraints();
        lbl.gridx = 0;
        lbl.anchor = GridBagConstraints.WEST;
        lbl.insets = new Insets(gapTiny, 0, gapTiny, gapSmall);
        GridBagConstraints fld = new GridBagConstraints();
        fld.gridx = 1;
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1.0;
        fld.insets = new Insets(gapTiny, 0, gapTiny, 0);
        GridBagConstraints btn = new GridBagConstraints();
        btn.gridx = 2;
        btn.insets = new Insets(gapTiny, gapSmall, gapTiny, 0);

        // File row
        lbl.gridy = 0;
        panel.add(new JLabel(I18n.get("dialog.import_tsv.file.label")), lbl);
        fld.gridy = 0;
        fileField.setEditable(false);
        panel.add(fileField, fld);
        btn.gridy = 0;
        JButton browseButton = new JButton(I18n.get("button.browse"));
        browseButton.addActionListener(e -> onBrowse());
        panel.add(browseButton, btn);

        // Category row
        lbl.gridy = 1;
        panel.add(new JLabel(I18n.get("dialog.import_tsv.category.label")), lbl);
        fld.gridy = 1;
        fld.gridwidth = 2;
        categoryCombo.addActionListener(e -> reloadPreview());
        panel.add(categoryCombo, fld);

        return panel;
    }

    private @NotNull JPanel buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(I18n.get("dialog.import_tsv.preview.title")));
        JTable previewTable = new JTable(preview.tableModel());
        previewTable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(previewTable);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusLabel, BorderLayout.WEST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        importButton.addActionListener(e -> onImport());
        buttonPanel.add(importButton);
        JButton cancelButton = new JButton(I18n.get("button.cancel"));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter(
                I18n.get("dialog.import_tsv.filter.description"),
                "tsv",
                "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
            reloadPreview();
        }
    }

    private void reloadPreview() {
        if (selectedFile == null || categoryCombo.getSelectedIndex() < 0) {
            preview.clear();
            return;
        }
        String categoryId = bus.creditsDoc().categories.get(categoryCombo.getSelectedIndex()).id;
        preview.reload(selectedFile, categoryId);
    }

    private void onImport() {
        List<ImportLine> lines = preview.lines();
        if (lines.isEmpty() || categoryCombo.getSelectedIndex() < 0) return;

        String categoryId = bus.creditsDoc().categories.get(categoryCombo.getSelectedIndex()).id;
        Command cmd = TsvImportCommandFactory.build(bus, categoryId, lines);
        if (cmd == null) {
            JOptionPane.showMessageDialog(
                this,
                I18n.get("dialog.import_tsv.no_changes.message"),
                I18n.get("dialog.import_tsv.no_changes.title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        onCommand.execute(cmd);
        dispose();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void populateCategoryCombo() {
        categoryCombo.removeAllItems();
        for (DocumentCategory cat : bus.creditsDoc().categories) {
            categoryCombo.addItem(cat.id);
        }
    }

    private void preselectCategory(@Nullable String categoryId) {
        if (categoryId == null) return;
        List<DocumentCategory> categories = bus.creditsDoc().categories;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id.equals(categoryId)) {
                categoryCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void onPreviewChanged() {
        updateImportButton();
        updateStatusLabel();
    }

    private void showReadError(@NotNull String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            I18n.get("dialog.import_tsv.read_error.title"),
            JOptionPane.ERROR_MESSAGE);
    }

    private void updateImportButton() {
        boolean hasActions = preview.lines()
            .stream()
            .anyMatch(l -> l.action != TsvImporter.Action.NO_CHANGE);
        importButton.setEnabled(hasActions);
    }

    private void updateStatusLabel() {
        List<ImportLine> lines = preview.lines();
        if (lines.isEmpty()) {
            statusLabel.setText(" ");
            return;
        }
        long create = lines.stream()
            .filter(l -> l.action == TsvImporter.Action.CREATE)
            .count();
        long add = lines.stream()
            .filter(l -> l.action == TsvImporter.Action.ADD)
            .count();
        long complete = lines.stream()
            .filter(l -> l.action == TsvImporter.Action.COMPLETE)
            .count();
        long noChange = lines.stream()
            .filter(l -> l.action == TsvImporter.Action.NO_CHANGE)
            .count();
        statusLabel.setText(I18n.get("dialog.import_tsv.status", lines.size(), create, add, complete, noChange));
    }
}
