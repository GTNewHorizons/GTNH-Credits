package net.noiraude.creditseditor.ui;

import java.io.IOException;

import net.noiraude.creditseditor.ResourceManager;
import net.noiraude.creditseditor.command.CommandStack;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.service.CreditsService;
import net.noiraude.creditseditor.service.KeySanitizer;
import net.noiraude.creditseditor.service.LangDocument;
import net.noiraude.creditseditor.service.LangService;

/**
 * Holds the open resource for one editing session: the resource manager, the model, the lang
 * document, and the undo/redo stack.
 *
 * <p>
 * Instances are created via the {@link #load} factory and are immutable with respect to the
 * resource they point to. The model data inside is mutable (edits happen in place).
 */
final class EditorSession {

    final ResourceManager resourceManager;
    final EditorModel model;
    final LangDocument langDoc;
    final CommandStack stack = new CommandStack();

    private EditorSession(ResourceManager resourceManager, EditorModel model, LangDocument langDoc) {
        this.resourceManager = resourceManager;
        this.model = model;
        this.langDoc = langDoc;
    }

    /**
     * Opens a session from {@code rm}, loading and merging the credit JSON and lang file.
     *
     * @throws Exception if either file cannot be read or parsed
     */
    static EditorSession load(ResourceManager rm) throws Exception {
        EditorModel model;
        if (rm.notExists(CreditsService.CREDITS_PATH)) {
            model = new EditorModel();
        } else {
            model = CreditsService.load(rm);
        }
        LangDocument lang = LangService.load(rm);

        for (EditorCategory cat : model.categories) {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            String name = lang.get(key);
            cat.displayName = name != null ? name : "";
            String detail = lang.get(key + ".detail");
            cat.description = detail != null ? detail : "";
        }

        return new EditorSession(rm, model, lang);
    }

    /**
     * Writes model data back to the resource files.
     *
     * @throws Exception if either file cannot be written
     */
    void save() throws Exception {
        for (EditorCategory cat : model.categories) {
            String key = "credits.category." + KeySanitizer.sanitize(cat.id);
            if (cat.displayName.isEmpty()) langDoc.remove(key);
            else langDoc.set(key, cat.displayName);
            if (cat.description.isEmpty()) langDoc.remove(key + ".detail");
            else langDoc.set(key + ".detail", cat.description);
        }
        CreditsService.save(model, resourceManager);
        LangService.save(langDoc, resourceManager);
    }

    /** Closes the resource manager, silently swallowing any {@link IOException}. */
    void close() {
        try {
            resourceManager.close();
        } catch (IOException ignored) {}
    }
}
