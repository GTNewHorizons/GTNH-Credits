package net.noiraude.creditseditor.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

/**
 * Menu bar for the credit editor window.
 *
 * <p>
 * Owns the three stateful menu items (Save, Undo, Redo) whose enabled state and text depend on
 * the current {@link EditorSession}. Call {@link #refresh(EditorSession)} after any command
 * execution, undo, redo, load, or save.
 */
final class EditorMenuBar extends JMenuBar {

    /**
     * File-menu callbacks: open, new, save, quit.
     */
    record FileActions(Runnable onOpen, Runnable onNew, Runnable onSave, Runnable onQuit) {

    }

    /** Edit-menu callbacks: undo, redo, and roles editor. */
    static final class EditActions {

        final Runnable onUndo;
        final Runnable onRedo;
        final Runnable onRoles;

        EditActions(Runnable onUndo, Runnable onRedo, Runnable onRoles) {
            this.onUndo = onUndo;
            this.onRedo = onRedo;
            this.onRoles = onRoles;
        }
    }

    private final JMenuItem menuSave;
    private final JMenuItem menuUndo;
    private final JMenuItem menuRedo;

    EditorMenuBar(FileActions fileActions, EditActions editActions) {
        Runnable onOpen = fileActions.onOpen;
        Runnable onNew = fileActions.onNew;
        Runnable onSave = fileActions.onSave;
        Runnable onQuit = fileActions.onQuit;
        Runnable onUndo = editActions.onUndo;
        Runnable onRedo = editActions.onRedo;
        Runnable onRoles = editActions.onRoles;
        JMenu fileMenu = new JMenu("File");
        JMenuItem menuOpen = new JMenuItem("Open Resources…");
        JMenuItem menuNew = new JMenuItem("New Resources…");
        menuSave = new JMenuItem("Save Resources");
        JMenuItem menuQuit = new JMenuItem("Quit");

        menuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        menuOpen.addActionListener(e -> onOpen.run());
        menuNew.addActionListener(e -> onNew.run());
        menuSave.addActionListener(e -> onSave.run());
        menuQuit.addActionListener(e -> onQuit.run());

        fileMenu.add(menuOpen);
        fileMenu.add(menuNew);
        fileMenu.addSeparator();
        fileMenu.add(menuSave);
        fileMenu.addSeparator();
        fileMenu.add(menuQuit);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem menuRoles = new JMenuItem("Roles...");
        menuRoles.addActionListener(e -> onRoles.run());
        editMenu.add(menuRoles);
        editMenu.addSeparator();

        menuUndo = new JMenuItem("Undo");
        menuRedo = new JMenuItem("Redo");

        menuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        menuRedo.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        menuUndo.addActionListener(e -> onUndo.run());
        menuRedo.addActionListener(e -> onRedo.run());

        editMenu.add(menuUndo);
        editMenu.add(menuRedo);

        add(fileMenu);
        add(editMenu);

        refresh(null);
    }

    /**
     * Updates enabled state and display text of the stateful menu items to reflect
     * {@code session}. Pass {@code null} when no resource is open.
     */
    void refresh(EditorSession session) {
        boolean loaded = session != null;
        menuSave.setEnabled(loaded);

        boolean canUndo = loaded && session.stack.canUndo();
        boolean canRedo = loaded && session.stack.canRedo();
        menuUndo.setEnabled(canUndo);
        menuRedo.setEnabled(canRedo);

        String undoName = loaded ? session.stack.peekUndoName() : null;
        menuUndo.setText(undoName != null ? "Undo " + undoName : "Undo");
        String redoName = loaded ? session.stack.peekRedoName() : null;
        menuRedo.setText(redoName != null ? "Redo " + redoName : "Redo");
    }
}
