package net.noiraude.creditseditor.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    record FileActions(@NotNull Runnable onOpen, @NotNull Runnable onNew, @NotNull Runnable onSave,
        @NotNull Runnable onQuit) {

    }

    /**
     * Edit-menu callbacks: undo, redo.
     */
    record EditActions(@NotNull Runnable onUndo, @NotNull Runnable onRedo) {

        @Contract(pure = true)
        EditActions {}
    }

    /**
     * Help-menu callbacks: about.
     */
    record HelpActions(@NotNull Runnable onAbout) {

        @Contract(pure = true)
        HelpActions {}
    }

    private final @NotNull JMenuItem menuSave;
    private final @NotNull JMenuItem menuUndo;
    private final @NotNull JMenuItem menuRedo;

    EditorMenuBar(@NotNull FileActions fileActions, @NotNull EditActions editActions,
        @NotNull HelpActions helpActions) {
        Runnable onOpen = fileActions.onOpen;
        Runnable onNew = fileActions.onNew;
        Runnable onSave = fileActions.onSave;
        Runnable onQuit = fileActions.onQuit;
        Runnable onUndo = editActions.onUndo;
        Runnable onRedo = editActions.onRedo;
        Runnable onAbout = helpActions.onAbout;
        JMenu fileMenu = new JMenu(I18n.get("menu.file"));
        applyMnemonic(fileMenu, "menu.file.mnemonic");
        JMenuItem menuOpen = new JMenuItem(I18n.get("menu.file.open"));
        applyMnemonic(menuOpen, "menu.file.open.mnemonic");
        JMenuItem menuNew = new JMenuItem(I18n.get("menu.file.new"));
        applyMnemonic(menuNew, "menu.file.new.mnemonic");
        menuSave = new JMenuItem(I18n.get("menu.file.save"));
        applyMnemonic(menuSave, "menu.file.save.mnemonic");
        JMenuItem menuQuit = new JMenuItem(I18n.get("menu.file.quit"));
        applyMnemonic(menuQuit, "menu.file.quit.mnemonic");

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

        JMenu editMenu = new JMenu(I18n.get("menu.edit"));
        applyMnemonic(editMenu, "menu.edit.mnemonic");

        menuUndo = new JMenuItem(I18n.get("menu.edit.undo"));
        applyMnemonic(menuUndo, "menu.edit.undo.mnemonic");
        menuRedo = new JMenuItem(I18n.get("menu.edit.redo"));
        applyMnemonic(menuRedo, "menu.edit.redo.mnemonic");

        menuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        menuRedo.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        menuUndo.addActionListener(e -> onUndo.run());
        menuRedo.addActionListener(e -> onRedo.run());

        editMenu.add(menuUndo);
        editMenu.add(menuRedo);

        JMenu helpMenu = new JMenu(I18n.get("menu.help"));
        applyMnemonic(helpMenu, "menu.help.mnemonic");
        JMenuItem menuAbout = new JMenuItem(I18n.get("menu.help.about", AppInfo.name()));
        applyMnemonic(menuAbout, "menu.help.about.mnemonic");
        menuAbout.addActionListener(e -> onAbout.run());
        helpMenu.add(menuAbout);

        add(fileMenu);
        add(editMenu);
        add(helpMenu);

        refresh(null);
    }

    private static void applyMnemonic(@NotNull AbstractButton button, @NotNull String key) {
        String value = I18n.get(key);
        if (value.isEmpty() || value.equals(key)) return;
        button.setMnemonic(value.charAt(0));
    }

    /**
     * Updates enabled state and display text of the stateful menu items to reflect
     * {@code session}. Pass {@code null} when no resource is open.
     */
    void refresh(@Nullable EditorSession session) {
        boolean loaded = session != null;
        menuSave.setEnabled(loaded);

        boolean canUndo = loaded && session.stack.canUndo();
        boolean canRedo = loaded && session.stack.canRedo();
        menuUndo.setEnabled(canUndo);
        menuRedo.setEnabled(canRedo);

        String undoName = loaded ? session.stack.peekUndoName() : null;
        menuUndo.setText(undoName != null ? I18n.get("menu.edit.undo.named", undoName) : I18n.get("menu.edit.undo"));
        String redoName = loaded ? session.stack.peekRedoName() : null;
        menuRedo.setText(redoName != null ? I18n.get("menu.edit.redo.named", redoName) : I18n.get("menu.edit.redo"));
    }
}
