package net.noiraude.creditseditor.ui;

import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import net.noiraude.creditseditor.ResourceManager;
import net.noiraude.creditseditor.command.CommandStack;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.creditseditor.service.CreditsService;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.LangDocument;
import net.noiraude.creditseditor.service.LangService;
import net.noiraude.creditseditor.ui.panel.CategoryPanel;
import net.noiraude.creditseditor.ui.panel.DetailPanel;
import net.noiraude.creditseditor.ui.panel.PersonPanel;

/**
 * Main application window for the GTNH Credits Editor.
 *
 * <p>
 * Layout: a horizontal split between the category list (left), a horizontal split between
 * the person list (centre) and the detail form (right). The File menu manages resource
 * open/save; the Edit menu provides undo and redo.
 */
public final class MainWindow extends JFrame {

    private ResourceManager resourceManager;
    private EditorModel model;
    private LangDocument langDoc;
    private final CommandStack stack = new CommandStack();

    // Selection state shared between panels
    private EditorCategory selectedCategory;
    private EditorPerson selectedPerson;

    /**
     * Set to {@code true} while {@link #refreshAll()} is rebuilding panel list models.
     * Callbacks that modify selection or switch the detail card must be suppressed during
     * this window to prevent the panel's model-clear events from cascading into spurious
     * "nothing selected" states.
     */
    private boolean ignoreCallbacks = false;

    // Panels
    private final CategoryPanel categoryPanel;
    private final PersonPanel personPanel;
    private final DetailPanel detailPanel;

    // Menu items that need enable/disable management
    private final JMenuItem menuSave;
    private final JMenuItem menuUndo;
    private final JMenuItem menuRedo;

    public MainWindow(ResourceManager resourceManager) {
        super("GTNH Credits Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                handleQuit();
            }
        });

        // Build panels personPanel must be initialized before categoryPanel because the
        // category-selection callback references personPanel.
        detailPanel = new DetailPanel(cmd -> executeCommand(cmd));

        personPanel = new PersonPanel(cmd -> executeCommand(cmd), person -> {
            if (ignoreCallbacks) return;
            selectedPerson = person;
            if (person != null) {
                detailPanel.showPerson(person, model);
            } else if (selectedCategory != null) {
                detailPanel.showCategory(selectedCategory);
            } else {
                detailPanel.showEmpty();
            }
        });

        categoryPanel = new CategoryPanel(cmd -> {
            stack.execute(cmd);
            refreshAll();
            updateMenuState();
            updateTitle();
        }, cat -> {
            if (ignoreCallbacks) return;
            selectedCategory = cat;
            if (cat != null) {
                // Category selected: filter persons and show category detail if no person
                personPanel.setFilter(cat);
                if (selectedPerson == null) {
                    detailPanel.showCategory(cat);
                }
            } else {
                // "All" selected
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

        // Menu bar
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem menuOpen = new JMenuItem("Open Resources…");
        JMenuItem menuNew = new JMenuItem("New Resources…");
        menuSave = new JMenuItem("Save Resources");
        JMenuItem menuQuit = new JMenuItem("Quit");

        menuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        menuOpen.addActionListener(e -> handleOpen(false));
        menuNew.addActionListener(e -> handleOpen(true));
        menuSave.addActionListener(e -> handleSave());
        menuQuit.addActionListener(e -> handleQuit());

        fileMenu.add(menuOpen);
        fileMenu.add(menuNew);
        fileMenu.addSeparator();
        fileMenu.add(menuSave);
        fileMenu.addSeparator();
        fileMenu.add(menuQuit);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuUndo = new JMenuItem("Undo");
        menuRedo = new JMenuItem("Redo");

        menuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        menuRedo.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        menuUndo.addActionListener(e -> handleUndo());
        menuRedo.addActionListener(e -> handleRedo());

        editMenu.add(menuUndo);
        editMenu.add(menuRedo);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);

        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Load initial resource if one was provided at startup
        if (resourceManager != null) {
            loadResource(resourceManager);
        } else {
            updateMenuState();
            updateTitle();
        }
    }

    // -----------------------------------------------------------------------
    // Command execution
    // -----------------------------------------------------------------------

    /**
     * Executes a command through the stack. For {@link EditFieldCommand} instances (text-field
     * edits) only repaints the lists so the display name can update without rebuilding the
     * entire list model. For structural commands a full {@link #refreshAll()} is done.
     */
    private void executeCommand(net.noiraude.creditseditor.command.Command cmd) {
        stack.execute(cmd);
        if (cmd instanceof net.noiraude.creditseditor.command.impl.EditFieldCommand) {
            categoryPanel.getList()
                .repaint();
            personPanel.getList()
                .repaint();
        } else {
            refreshAll();
        }
        updateMenuState();
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Resource loading and saving
    // -----------------------------------------------------------------------

    private void loadResource(ResourceManager rm) {
        EditorModel loaded;
        LangDocument lang;
        try {
            if (!rm.exists(CreditsService.CREDITS_PATH)) {
                // New resource: start from an empty model; LangService handles missing file
                loaded = new EditorModel();
            } else {
                loaded = CreditsService.load(rm);
            }
            lang = LangService.load(rm);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to load credits data:\n" + ex.getMessage(),
                "Load error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Populate lang-derived fields from the lang document
        for (EditorCategory cat : loaded.categories) {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            String name = lang.get(key);
            cat.displayName = name != null ? name : "";
            String detail = lang.get(key + ".detail");
            cat.description = detail != null ? detail : "";
        }

        // Commit the loaded state
        if (this.resourceManager != null && this.resourceManager != rm) {
            try {
                this.resourceManager.close();
            } catch (IOException ignored) {}
        }
        this.resourceManager = rm;
        this.model = loaded;
        this.langDoc = lang;
        this.selectedCategory = null;
        this.selectedPerson = null;
        stack.clear();

        ignoreCallbacks = true;
        try {
            categoryPanel.refresh(model);
            personPanel.refresh(model);
        } finally {
            ignoreCallbacks = false;
        }
        detailPanel.showEmpty();

        updateMenuState();
        updateTitle();
    }

    private void handleOpen(boolean createNew) {
        if (stack.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "There are unsaved changes. Discard and open a different resource?",
                "Unsaved changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

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
        } catch (IOException ex) {
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
        if (resourceManager == null || model == null) return;

        // Write lang-derived fields back into the lang document
        for (EditorCategory cat : model.categories) {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            if (cat.displayName.isEmpty()) {
                langDoc.remove(key);
            } else {
                langDoc.set(key, cat.displayName);
            }
            if (cat.description.isEmpty()) {
                langDoc.remove(key + ".detail");
            } else {
                langDoc.set(key + ".detail", cat.description);
            }
        }

        try {
            CreditsService.save(model, resourceManager);
            LangService.save(langDoc, resourceManager);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save:\n" + ex.getMessage(),
                "Save error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        stack.markClean();
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Undo / redo
    // -----------------------------------------------------------------------

    private void handleUndo() {
        if (!stack.canUndo()) return;
        stack.undo();
        refreshAll();
        updateTitle();
    }

    private void handleRedo() {
        if (!stack.canRedo()) return;
        stack.redo();
        refreshAll();
        updateTitle();
    }

    // -----------------------------------------------------------------------
    // Quit
    // -----------------------------------------------------------------------

    private void handleQuit() {
        if (stack.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "There are unsaved changes. Quit anyway?",
                "Unsaved changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        if (resourceManager != null) {
            try {
                resourceManager.close();
            } catch (IOException ignored) {}
        }
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // UI refresh helpers
    // -----------------------------------------------------------------------

    /**
     * Refreshes all panels from the current model state and re-applies the selection.
     * Called after every command execute, undo, or redo.
     */
    private void refreshAll() {
        if (model == null) return;

        // Re-resolve selectedCategory and selectedPerson by id/name so we survive
        // undo of add (which removes the object from the list).
        EditorCategory resolvedCat = null;
        if (selectedCategory != null) {
            String id = selectedCategory.id;
            resolvedCat = model.categories.stream()
                .filter(c -> c.id.equals(id))
                .findFirst()
                .orElse(null);
            selectedCategory = resolvedCat;
        }

        EditorPerson resolvedPerson = null;
        if (selectedPerson != null) {
            String name = selectedPerson.name;
            resolvedPerson = model.persons.stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
            selectedPerson = resolvedPerson;
        }

        ignoreCallbacks = true;
        try {
            categoryPanel.refresh(model);
            personPanel.refresh(model);
        } finally {
            ignoreCallbacks = false;
        }
        detailPanel.refresh(selectedCategory, selectedPerson, model);

        updateMenuState();
    }

    private void updateMenuState() {
        boolean loaded = resourceManager != null && model != null;
        menuSave.setEnabled(loaded);
        menuUndo.setEnabled(stack.canUndo());
        menuRedo.setEnabled(stack.canRedo());

        String undoName = stack.peekUndoName();
        menuUndo.setText(undoName != null ? "Undo " + undoName : "Undo");
        String redoName = stack.peekRedoName();
        menuRedo.setText(redoName != null ? "Redo " + redoName : "Redo");
    }

    private void updateTitle() {
        String base = "GTNH Credits Editor";
        if (resourceManager != null) {
            base += " " + resourceManager.getDiskPath()
                .getFileName();
            if (stack.isDirty()) {
                base += " *";
            }
        }
        setTitle(base);
    }
}
