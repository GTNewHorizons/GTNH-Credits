package net.noiraude.creditseditor.ui;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.jetbrains.annotations.NotNull;

/**
 * Menu bar layout for the credit editor window. Holds no callback state of its own:
 * every {@link javax.swing.JMenuItem} is bound to a {@link javax.swing.Action} on
 * {@link EditorActions}, which owns enablement, label, accelerator, and mnemonic.
 */
final class EditorMenuBar extends JMenuBar {

    EditorMenuBar(@NotNull EditorActions actions) {
        JMenu fileMenu = newMenu("menu.file", "menu.file.mnemonic");
        fileMenu.add(actions.open);
        fileMenu.add(actions.newDoc);
        fileMenu.addSeparator();
        fileMenu.add(actions.save);
        fileMenu.add(actions.saveAs);
        fileMenu.addSeparator();
        fileMenu.add(actions.quit);

        JMenu editMenu = newMenu("menu.edit", "menu.edit.mnemonic");
        editMenu.add(actions.undo);
        editMenu.add(actions.redo);

        JMenu helpMenu = newMenu("menu.help", "menu.help.mnemonic");
        helpMenu.add(actions.shortcuts);
        helpMenu.addSeparator();
        helpMenu.add(actions.about);

        add(fileMenu);
        add(editMenu);
        add(helpMenu);
    }

    private static @NotNull JMenu newMenu(@NotNull String labelKey, @NotNull String mnemonicKey) {
        JMenu menu = new JMenu(I18n.get(labelKey));
        String mnemonic = I18n.get(mnemonicKey);
        if (mnemonic.length() == 1) {
            menu.setMnemonic(mnemonic.charAt(0));
        }
        return menu;
    }
}
