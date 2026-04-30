package net.noiraude.creditseditor.bus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-wide event bus that publishes coarse-grained document mutations to
 * self-subscribing widgets.
 *
 * <p>
 * Built on {@link PropertyChangeSupport} so it uses the same idiom as the leaf-level
 * text editors. Widgets register once with {@link #addListener(String, PropertyChangeListener)}
 * for the topics they care about and re-read whatever they need from {@link #creditsDoc()}
 * and {@link #langDoc()} when a topic fires.
 *
 * <p>
 * The bus is constructed once by the main window and lives for the whole application.
 * Opening a new resource replaces the documents via {@link #setSession(CreditsDocument, LangDocument)},
 * which fires {@link #TOPIC_SESSION} so every subscriber rebuilds against the new documents.
 */
public final class DocumentBus {

    /** Session documents were swapped. Subscribers should rebuild from scratch. */
    public static final @NotNull String TOPIC_SESSION = "session";

    /** Category list structure changed (add, remove, reorder). */
    public static final @NotNull String TOPIC_CATEGORIES = "categories";

    /** Person list structure changed (add, remove, reorder). */
    public static final @NotNull String TOPIC_PERSONS = "persons";

    /** A single person's fields or memberships changed. {@code newValue} is the person. */
    public static final @NotNull String TOPIC_PERSON = "person";

    /** A single category's fields changed. {@code newValue} is the category. */
    public static final @NotNull String TOPIC_CATEGORY = "category";

    /** A lang key changed. {@code newValue} is the key. */
    public static final @NotNull String TOPIC_LANG = "lang";

    /**
     * The undo/redo stack state changed (a command was executed, undone, or redone).
     * Subscribers should re-read {@code canUndo}, {@code canRedo}, and the peek names
     * from whatever {@link net.noiraude.creditseditor.command.CommandStack} they hold.
     */
    public static final @NotNull String TOPIC_COMMAND_STACK = "commandStack";

    private final @NotNull PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private @Nullable CreditsDocument creditsDoc;
    private @Nullable LangDocument langDoc;

    /** Returns the current credits document. Throws when no session is loaded. */
    @Contract(pure = true)
    public @NotNull CreditsDocument creditsDoc() {
        return Objects.requireNonNull(creditsDoc, "No session loaded");
    }

    /** Returns the current lang document. Throws when no session is loaded. */
    @Contract(pure = true)
    public @NotNull LangDocument langDoc() {
        return Objects.requireNonNull(langDoc, "No session loaded");
    }

    /** Returns {@code true} once a session has been loaded via {@link #setSession}. */
    @Contract(pure = true)
    public boolean hasSession() {
        return creditsDoc != null;
    }

    /**
     * Swaps the current documents and fires {@link #TOPIC_SESSION}. All subscribers that
     * hold references to the old documents should re-read from this bus.
     */
    public void setSession(@NotNull CreditsDocument credits, @NotNull LangDocument lang) {
        this.creditsDoc = credits;
        this.langDoc = lang;
        pcs.firePropertyChange(TOPIC_SESSION, null, this);
    }

    public void fireCategoriesChanged() {
        pcs.firePropertyChange(TOPIC_CATEGORIES, null, Boolean.TRUE);
    }

    public void firePersonsChanged() {
        pcs.firePropertyChange(TOPIC_PERSONS, null, Boolean.TRUE);
    }

    public void firePersonChanged(@NotNull DocumentPerson person) {
        pcs.firePropertyChange(TOPIC_PERSON, null, person);
    }

    public void fireCategoryChanged(@NotNull DocumentCategory category) {
        pcs.firePropertyChange(TOPIC_CATEGORY, null, category);
    }

    public void fireLangChanged(@NotNull String key) {
        pcs.firePropertyChange(TOPIC_LANG, null, key);
    }

    /**
     * Fires {@link #TOPIC_COMMAND_STACK}. Call after every {@code execute}, {@code undo},
     * or {@code redo} on the active command stack.
     */
    public void fireCommandStackChanged() {
        pcs.firePropertyChange(TOPIC_COMMAND_STACK, null, Boolean.TRUE);
    }

    /** Subscribes {@code listener} to events on {@code topic}. */
    public void addListener(@NotNull String topic, @NotNull PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(topic, listener);
    }
}
