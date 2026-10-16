package net.noiraude.creditseditor.ui;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.jetbrains.annotations.NotNull;

/** Composes the editor's menu bar from the shared action registry. */
final class EditorMenuBar extends JMenuBar {

    EditorMenuBar(@NotNull EditorActions actions) {
        JMenu fileMenu = newMenu("menu.file", "menu.file.mnemonic");
        fileMenu.add(actions.open);
        JMenu recent = new JMenu(I18n.get("menu.open_recent"));
        recent.addMenuListener(new javax.swing.event.MenuListener() {

            public void menuSelected(javax.swing.event.MenuEvent event) {
                recent.removeAll();
                for (String path : RecentLocations.user()
                    .paths()) {
                    javax.swing.JMenuItem item = new javax.swing.JMenuItem(path);
                    item.addActionListener(e -> actions.bus.fireOpenPathRequested(path));
                    recent.add(item);
                }
                if (recent.getItemCount() == 0) {
                    javax.swing.JMenuItem empty = new javax.swing.JMenuItem(I18n.get("menu.open_recent.empty"));
                    empty.setEnabled(false);
                    recent.add(empty);
                }
            }

            public void menuDeselected(javax.swing.event.MenuEvent event) {}

            public void menuCanceled(javax.swing.event.MenuEvent event) {}
        });
        fileMenu.add(recent);
        fileMenu.add(actions.newDoc);
        fileMenu.addSeparator();
        fileMenu.add(actions.save);
        fileMenu.add(actions.saveAs);
        fileMenu.addSeparator();
        fileMenu.add(actions.quit);

        JMenu editMenu = newMenu("menu.edit", "menu.edit.mnemonic");
        editMenu.add(actions.undo);
        editMenu.add(actions.redo);
        editMenu.addSeparator();
        editMenu.add(actions.manageLocales);
        javax.swing.JMenuItem translations = new javax.swing.JMenuItem(I18n.get("translations.load"));
        translations.setEnabled(actions.bus.hasSession());
        actions.bus.addListener(
            net.noiraude.creditseditor.bus.DocumentBus.TOPIC_SESSION,
            e -> translations.setEnabled(actions.bus.hasSession()));
        translations.addActionListener(e -> actions.bus.fireTranslationsRequested());
        editMenu.add(translations);
        javax.swing.JMenuItem validate = new javax.swing.JMenuItem(I18n.get("validation.title"));
        validate.setEnabled(actions.bus.hasSession());
        actions.bus.addListener(
            net.noiraude.creditseditor.bus.DocumentBus.TOPIC_SESSION,
            e -> validate.setEnabled(actions.bus.hasSession()));
        validate.addActionListener(e -> actions.bus.fireValidationRequested());
        editMenu.add(validate);

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
