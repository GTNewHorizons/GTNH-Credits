package net.noiraude.creditseditor.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import net.noiraude.creditseditor.ResourceManager;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.impl.DocumentEditCommand;
import net.noiraude.creditseditor.command.impl.EditFieldCommand;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.creditseditor.ui.panel.CategoryPanel;
import net.noiraude.creditseditor.ui.panel.DetailPanel;
import net.noiraude.creditseditor.ui.panel.PersonPanel;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * Layout: a horizontal split between the category list (left), a horizontal split between
 * the person list (center) and the detail form (right). The File menu manages resource
 * open/save; the Edit menu provides undo and redo.
 */
public final class MainWindow extends JFrame {

    private EditorSession session; // null when no resource is open

    // Selection state shared between panels
    private EditorCategory selectedCategory;
    private EditorPerson selectedPerson;

    // Panels
    private final CategoryPanel categoryPanel;
    private final PersonPanel personPanel;
    private final DetailPanel detailPanel;

    private final EditorMenuBar menuBar;

    public MainWindow(ResourceManager initialResource) {
        super("GTNH Credits Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                handleQuit();
            }
        });

        // Build panels: personPanel must be initialized before categoryPanel because the
        // category-selection callback references personPanel.
        detailPanel = new DetailPanel(this::executeCommand);

        personPanel = new PersonPanel(this::executeCommand, person -> {
            selectedPerson = person;
            if (person != null) {
                detailPanel.showPerson(person, session.model);
            } else if (selectedCategory != null) {
                detailPanel.showCategory(selectedCategory);
            } else {
                detailPanel.showEmpty();
            }
        });

        categoryPanel = new CategoryPanel(cmd -> {
            session.stack.execute(cmd);
            refreshAll();
            updateTitle();
        }, cat -> {
            selectedCategory = cat;
            if (cat != null) {
                personPanel.setFilter(cat);
                if (selectedPerson == null) {
                    detailPanel.showCategory(cat);
                }
            } else {
                personPanel.setFilter(null);
                if (selectedPerson == null) {
                    detailPanel.showEmpty();
                }
            }
        });

        // Layout: category | person | detail
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, personPanel, detailPanel);
        rightSplit.setDividerLocation(250);
        rightSplit.setResizeWeight(0.3);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryPanel, rightSplit);
        mainSplit.setDividerLocation(200);
        mainSplit.setResizeWeight(0.2);
        mainSplit.setBorder(new EmptyBorder(4, 4, 4, 4));

        setLayout(new BorderLayout());
        add(mainSplit, BorderLayout.CENTER);

        menuBar = new EditorMenuBar(
            () -> handleOpen(false),
            () -> handleOpen(true),
            this::handleSave,
            this::handleQuit,
            this::handleUndo,
            this::handleRedo);
        setJMenuBar(menuBar);

        setSize(1100, 700);
        setLocationRelativeTo(null);

        if (initialResource != null) {
            loadResource(initialResource);
        } else {
            menuBar.refresh(null);
            updateTitle();
        }
    }

    // -----------------------------------------------------------------------
    // Command execution
    // -----------------------------------------------------------------------

    /**
     * Executes a command through the stack. For field-level edits only repaints the lists so
     * the display name can update without rebuilding the entire list model. For structural
     * commands a full {@link #refreshAll()} is done.
     */
    private void executeCommand(Command cmd) {
        session.stack.execute(cmd);
        if (cmd instanceof EditFieldCommand || cmd instanceof DocumentEditCommand) {
            categoryPanel.getList()
                .repaint();
            personPanel.getList()
                .repaint();
            menuBar.refresh(session);
        } else {
            refreshAll(); // calls menuBar.refresh internally
        }
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Resource loading and saving
    // -----------------------------------------------------------------------

    private void loadResource(ResourceManager rm) {
        EditorSession newSession;
        try {
            newSession = EditorSession.load(rm);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to load credits data:\n" + ex.getMessage(),
                "Load error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (session != null && session.resourceManager != rm) {
            session.close();
        }
        session = newSession;
        selectedCategory = null;
        selectedPerson = null;

        categoryPanel.refresh(session.model);
        personPanel.refresh(session.model);
        detailPanel.showEmpty();

        menuBar.refresh(session);
        updateTitle();
    }

    private void handleOpen(boolean createNew) {
        if (shouldAbort()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle(createNew ? "New resource location" : "Open resource");

        int result = createNew ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile()
            .getAbsolutePath();
        ResourceManager rm;
        try {
            rm = ResourceManager.open(path);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Cannot open resource:\n" + ex.getMessage(),
                "Open error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadResource(rm);
    }

    private void handleSave() {
        if (session == null) return;
        try {
            session.save();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save:\n" + ex.getMessage(),
                "Save error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        session.stack.markClean();
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Undo / redo
    // -----------------------------------------------------------------------

    private void handleUndo() {
        if (session == null || !session.stack.canUndo()) return;
        afterUndoRedo(session.stack.undo());
    }

    private void handleRedo() {
        if (session == null || !session.stack.canRedo()) return;
        afterUndoRedo(session.stack.redo());
    }

    /**
     * Post-{@code undo}/{@code redo} UI refresh. For {@link DocumentEditCommand}s the document
     * is already in the correct state and the model was already synced via a PROP_TEXT event,
     * so only list repaints are needed. Calling {@code refreshAll()} (which invokes
     * {@code setText}) would destroy the Swing {@code UndoableEdit} references and break later
     * redo operations.
     */
    private void afterUndoRedo(Command cmd) {
        if (cmd instanceof DocumentEditCommand) {
            categoryPanel.getList()
                .repaint();
            personPanel.getList()
                .repaint();
            menuBar.refresh(session);
        } else {
            refreshAll(); // calls menuBar.refresh internally
        }
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Quit
    // -----------------------------------------------------------------------

    private void handleQuit() {
        if (shouldAbort()) return;
        if (session != null) session.close();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Refreshes all panels from the current model state and re-applies the selection.
     * Called after every structural command execute, undo, or redo.
     */
    private void refreshAll() {
        if (session == null) return;

        // Re-resolve selectedCategory and selectedPerson by id/name so we survive
        // reverting an Add command, which removes the object from the list.
        if (selectedCategory != null) {
            String id = selectedCategory.id;
            selectedCategory = session.model.categories.stream()
                .filter(c -> c.id.equals(id))
                .findFirst()
                .orElse(null);
        }
        if (selectedPerson != null) {
            String name = selectedPerson.name;
            selectedPerson = session.model.persons.stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
        }

        categoryPanel.refresh(session.model);
        personPanel.refresh(session.model);
        detailPanel.refresh(selectedCategory, selectedPerson, session.model);

        menuBar.refresh(session);
    }

    private void updateTitle() {
        String base = "GTNH Credits Editor";
        if (session != null) {
            base += " " + session.resourceManager.getDiskPath()
                .getFileName();
            if (session.stack.isDirty()) base += " *";
        }
        setTitle(base);
    }

    /**
     * Shows an "unsaved changes" confirmation dialog if needed.
     *
     * @return {@code true} if the caller should abort (no session, dirty and user declined),
     *         {@code false} if the caller may proceed
     */
    private boolean shouldAbort() {
        if (session == null || !session.stack.isDirty()) return false;
        int choice = JOptionPane.showConfirmDialog(
            this,
            "There are unsaved changes. Discard them?",
            "Unsaved changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.YES_OPTION;
    }
}
