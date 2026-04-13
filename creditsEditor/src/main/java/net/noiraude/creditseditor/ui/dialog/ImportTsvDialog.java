package net.noiraude.creditseditor.ui.dialog;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.command.impl.AddMembershipCommand;
import net.noiraude.creditseditor.command.impl.AddPersonCommand;
import net.noiraude.creditseditor.command.impl.AddPersonRoleCommand;
import net.noiraude.creditseditor.command.impl.CompoundCommand;
import net.noiraude.creditseditor.service.TsvImporter;
import net.noiraude.creditseditor.service.TsvImporter.ImportLine;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Dialog for importing persons from a TSV file into a target category.
 *
 * <p>
 * Shows a file chooser, a target category selector, a preview table with the computed
 * action per line, and an import button. The entire import is dispatched as a single
 * {@link CompoundCommand} for atomic undo.
 */
public final class ImportTsvDialog extends JDialog {

    private final CommandExecutor onCommand;
    private final CreditsDocument creditsDoc;

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> categoryCombo = new JComboBox<>();
    private final PreviewTableModel previewModel = new PreviewTableModel();
    private final JTable previewTable = new JTable(previewModel);
    private final JButton importButton = new JButton("Import");
    private final JLabel statusLabel = new JLabel(" ");

    private List<ImportLine> importLines = List.of();
    private File selectedFile;

    public ImportTsvDialog(Frame owner, CommandExecutor onCommand, CreditsDocument creditsDoc) {
        super(owner, "Import TSV", true);
        this.onCommand = onCommand;
        this.creditsDoc = creditsDoc;

        setLayout(new BorderLayout(scaled(8), scaled(8)));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(scaled(8), scaled(8), scaled(8), scaled(8)));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildPreviewPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        populateCategoryCombo();
        updateImportButton();

        setSize(scaled(600), scaled(450));
        setLocationRelativeTo(owner);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints lbl = new GridBagConstraints();
        lbl.gridx = 0;
        lbl.anchor = GridBagConstraints.WEST;
        lbl.insets = new Insets(scaled(2), 0, scaled(2), scaled(4));
        GridBagConstraints fld = new GridBagConstraints();
        fld.gridx = 1;
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1.0;
        fld.insets = new Insets(scaled(2), 0, scaled(2), 0);
        GridBagConstraints btn = new GridBagConstraints();
        btn.gridx = 2;
        btn.insets = new Insets(scaled(2), scaled(4), scaled(2), 0);

        // File row
        lbl.gridy = 0;
        panel.add(new JLabel("File:"), lbl);
        fld.gridy = 0;
        fileField.setEditable(false);
        panel.add(fileField, fld);
        btn.gridy = 0;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> onBrowse());
        panel.add(browseButton, btn);

        // Category row
        lbl.gridy = 1;
        panel.add(new JLabel("Target category:"), lbl);
        fld.gridy = 1;
        fld.gridwidth = 2;
        categoryCombo.addActionListener(e -> reloadPreview());
        panel.add(categoryCombo, fld);

        return panel;
    }

    private JPanel buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewTable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(previewTable);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusLabel, BorderLayout.WEST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        importButton.addActionListener(e -> onImport());
        buttonPanel.add(importButton);
        JButton cancelButton = new JButton("Cancel");
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
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("TSV files", "tsv", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
            reloadPreview();
        }
    }

    private void reloadPreview() {
        if (selectedFile == null || categoryCombo.getSelectedIndex() < 0) {
            importLines = List.of();
            previewModel.setLines(importLines);
            updateImportButton();
            return;
        }

        String categoryId = creditsDoc.categories.get(categoryCombo.getSelectedIndex()).id;
        try (FileReader reader = new FileReader(selectedFile, StandardCharsets.UTF_8)) {
            importLines = TsvImporter.parse(reader, creditsDoc, categoryId);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to read file:\n" + ex.getMessage(),
                "Read error",
                JOptionPane.ERROR_MESSAGE);
            importLines = List.of();
        }

        previewModel.setLines(importLines);
        updateImportButton();
        updateStatusLabel();
    }

    private void onImport() {
        if (importLines.isEmpty() || categoryCombo.getSelectedIndex() < 0) return;

        String categoryId = creditsDoc.categories.get(categoryCombo.getSelectedIndex()).id;
        long actionCount = importLines.stream()
            .filter(l -> l.action != TsvImporter.Action.NO_CHANGE)
            .count();
        if (actionCount == 0) {
            JOptionPane.showMessageDialog(
                this,
                "Nothing to import: all entries already present.",
                "No changes",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        CompoundCommand.Builder builder = new CompoundCommand.Builder(
            "Import TSV (" + importLines.size() + " persons)");

        for (ImportLine line : importLines) {
            buildCommandsForLine(builder, line, categoryId);
        }

        if (!builder.isEmpty()) {
            onCommand.execute(builder.build());
        }
        dispose();
    }

    private void buildCommandsForLine(CompoundCommand.Builder builder, ImportLine line, String categoryId) {
        switch (line.action) {
            case CREATE -> {
                DocumentPerson person = new DocumentPerson(line.name);
                builder.add(new AddPersonCommand(creditsDoc, person));
                DocumentMembership membership = new DocumentMembership(categoryId);
                builder.add(new AddMembershipCommand(person, membership));
                for (String role : line.roles) {
                    builder.add(new AddPersonRoleCommand(membership, role));
                }
            }
            case ADD -> {
                DocumentPerson person = creditsDoc.persons.stream()
                    .filter(p -> p.name.equals(line.name))
                    .findFirst()
                    .orElseThrow();
                DocumentMembership membership = new DocumentMembership(categoryId);
                builder.add(new AddMembershipCommand(person, membership));
                for (String role : line.roles) {
                    builder.add(new AddPersonRoleCommand(membership, role));
                }
            }
            case COMPLETE -> {
                DocumentPerson person = creditsDoc.persons.stream()
                    .filter(p -> p.name.equals(line.name))
                    .findFirst()
                    .orElseThrow();
                DocumentMembership membership = person.memberships.stream()
                    .filter(m -> m.categoryId.equals(categoryId))
                    .findFirst()
                    .orElseThrow();
                for (String role : line.roles) {
                    if (!membership.roles.contains(role)) {
                        builder.add(new AddPersonRoleCommand(membership, role));
                    }
                }
            }
            case NO_CHANGE -> {
                // nothing to do
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void populateCategoryCombo() {
        categoryCombo.removeAllItems();
        for (DocumentCategory cat : creditsDoc.categories) {
            categoryCombo.addItem(cat.id);
        }
    }

    private void updateImportButton() {
        boolean hasActions = importLines.stream()
            .anyMatch(l -> l.action != TsvImporter.Action.NO_CHANGE);
        importButton.setEnabled(hasActions);
    }

    private void updateStatusLabel() {
        if (importLines.isEmpty()) {
            statusLabel.setText(" ");
            return;
        }
        long create = importLines.stream()
            .filter(l -> l.action == TsvImporter.Action.CREATE)
            .count();
        long add = importLines.stream()
            .filter(l -> l.action == TsvImporter.Action.ADD)
            .count();
        long complete = importLines.stream()
            .filter(l -> l.action == TsvImporter.Action.COMPLETE)
            .count();
        long noChange = importLines.stream()
            .filter(l -> l.action == TsvImporter.Action.NO_CHANGE)
            .count();
        statusLabel.setText(
            importLines.size() + " lines: "
                + create
                + " create, "
                + add
                + " add, "
                + complete
                + " complete, "
                + noChange
                + " unchanged");
    }

    // -----------------------------------------------------------------------
    // Preview table model
    // -----------------------------------------------------------------------

    private static final class PreviewTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = { "Name", "Roles", "Action" };
        private List<ImportLine> lines = List.of();

        void setLines(List<ImportLine> lines) {
            this.lines = lines;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            ImportLine line = lines.get(row);
            return switch (col) {
                case 0 -> line.name;
                case 1 -> String.join(", ", line.roles);
                case 2 -> line.action.name()
                    .replace('_', ' ')
                    .toLowerCase();
                default -> "";
            };
        }
    }
}
