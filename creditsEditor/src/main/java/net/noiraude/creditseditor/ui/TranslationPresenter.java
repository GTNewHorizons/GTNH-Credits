package net.noiraude.creditseditor.ui;

import java.awt.Frame;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.swing.JFileChooser;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.dialog.ProgressDialog;

/** Loads optional instance translations on a worker; never edits the selected Minecraft instance. */
final class TranslationPresenter {

    TranslationPresenter(DocumentBus bus, Frame owner, SessionHolder holder, Consumer<Path> onInstanceSelected) {
        bus.addListener(DocumentBus.TOPIC_REQUEST_VALIDATE, event -> holder.ifPresent(session -> {
            var errors = session.validateContent();
            String text = errors.isEmpty()
                ? I18n.get(session.hasTranslationCatalog() ? "validation.success" : "validation.success.no_catalog")
                : String.join("\n", errors);
            javax.swing.JOptionPane.showMessageDialog(
                owner,
                text,
                I18n.get("validation.title"),
                errors.isEmpty() ? javax.swing.JOptionPane.INFORMATION_MESSAGE
                    : javax.swing.JOptionPane.WARNING_MESSAGE);
        }));
        bus.addListener(DocumentBus.TOPIC_REQUEST_TRANSLATIONS, event -> holder.ifPresent(session -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(I18n.get("translations.choose_instance"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
            Path instance = chooser.getSelectedFile()
                .toPath();
            TranslationLoadWorker worker = new TranslationLoadWorker(session, instance);
            ProgressDialog.runWith(owner, I18n.get("translations.loading"), instance.toString(), worker);
            try {
                worker.get();
                onInstanceSelected.accept(instance);
                bus.fireTranslationsChanged();
            } catch (Exception ex) {
                ErrorPresenter
                    .show(owner, I18n.get("dialog.load.error.title"), ex.getCause() == null ? ex : ex.getCause());
            }
        }));
    }
}
