package net.noiraude.creditseditor.bus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.noiraude.creditseditor.command.CommandStackSnapshot;
import net.noiraude.creditseditor.command.EditAbortedException;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

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
 * The bus is constructed once by the application and lives for the whole session. It
 * reads document state through an injected {@link DocumentSessionSource}; opening a new
 * resource updates the source and fires {@link #TOPIC_SESSION} so every subscriber
 * rebuilds against the new documents.
 */
public final class DocumentBus {

    /**
     * Session presence and/or documents changed. {@code newValue} is the {@link Boolean}
     * session-presence flag after the change.
     */
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
     * The active editing locale changed. {@code oldValue} and {@code newValue} are the
     * previous and current locale tags (e.g. {@code en_US}, {@code fr_FR}). Subscribers
     * should re-resolve every visible lang key against the new locale.
     */
    public static final @NotNull String TOPIC_LOCALE = "locale";

    /**
     * The undo/redo stack state changed (a command was executed, undone, or redone).
     * Subscribers should re-read {@code canUndo}, {@code canRedo}, and the peek names
     * from whatever {@link net.noiraude.creditseditor.command.CommandStack} they hold.
     */
    public static final @NotNull String TOPIC_COMMAND_STACK = "commandStack";

    /**
     * The unsaved-changes flag changed. {@code oldValue} and {@code newValue} are the
     * previous and current boolean dirty flags.
     */
    public static final @NotNull String TOPIC_DIRTY = "dirty";

    /** The user requested to load an existing credits resource. */
    public static final @NotNull String TOPIC_REQUEST_OPEN = "request.open";

    /** The user requested to create a new credits resource. */
    public static final @NotNull String TOPIC_REQUEST_NEW = "request.new";

    /** The user requested to persist the active session to its current location. */
    public static final @NotNull String TOPIC_REQUEST_SAVE = "request.save";

    /** The user requested to persist the active session to a chosen location. */
    public static final @NotNull String TOPIC_REQUEST_SAVE_AS = "request.saveAs";

    /** The user requested to close the editor. */
    public static final @NotNull String TOPIC_REQUEST_QUIT = "request.quit";

    /** The user requested to revert the most recent command. */
    public static final @NotNull String TOPIC_REQUEST_UNDO = "request.undo";

    /** The user requested to reapply the most recently undone command. */
    public static final @NotNull String TOPIC_REQUEST_REDO = "request.redo";

    /** The user requested to view the keyboard-shortcuts reference. */
    public static final @NotNull String TOPIC_REQUEST_SHORTCUTS = "request.shortcuts";

    /** The user requested to view the about dialog. */
    public static final @NotNull String TOPIC_REQUEST_ABOUT = "request.about";

    /** The user requested to open the manage-locales window. */
    public static final @NotNull String TOPIC_REQUEST_MANAGE_LOCALES = "request.manageLocales";

    /** A command refused to apply. {@code newValue} is the {@link EditAbortedException}. */
    public static final @NotNull String TOPIC_EDIT_ABORTED = "editAborted";

    private final @NotNull PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final @NotNull DocumentSessionSource source;
    private @NotNull String activeLocale = LangResolver.DEFAULT_LOCALE;

    public DocumentBus(@NotNull DocumentSessionSource source) {
        this.source = Objects.requireNonNull(source);
    }

    /** Returns the current credits document. Throws when no session is loaded. */
    @Contract(pure = true)
    public @NotNull CreditsDocument creditsDoc() {
        return requireSession().creditsDoc();
    }

    /**
     * Returns the {@link Locale#US} lang document. Throws when no session is loaded or
     * when the default locale's document is missing.
     */
    @Contract(pure = true)
    public @NotNull LangDocument langDoc() {
        LangDocument doc = requireSession().langDocs()
            .get(LangResolver.DEFAULT_LOCALE);
        return Objects.requireNonNull(doc, "No " + LangResolver.DEFAULT_LOCALE + " lang document loaded");
    }

    /** Returns the lang document for {@code locale}, if loaded. Throws when no session is loaded. */
    @Contract(pure = true)
    public @NotNull Optional<LangDocument> langDoc(@NotNull String locale) {
        return requireSession().langDoc(locale);
    }

    private @NotNull DocumentSession requireSession() {
        return source.session()
            .orElseThrow(() -> new IllegalStateException("No session loaded"));
    }

    /** Returns whether a session is currently loaded. */
    @Contract(pure = true)
    public boolean hasSession() {
        return source.session()
            .isPresent();
    }

    /**
     * Returns the locale tags loaded in the current session, in insertion order, or an empty
     * set when no session is loaded.
     */
    @Contract(pure = true)
    public @NotNull @UnmodifiableView Set<String> availableLocales() {
        return source.session()
            .map(
                s -> Collections.unmodifiableSet(
                    s.langDocs()
                        .keySet()))
            .orElse(Collections.emptySet());
    }

    /**
     * Returns the active editing locale tag, defaulting to {@link Locale#US}.
     */
    @Contract(pure = true)
    public @NotNull String activeLocale() {
        return activeLocale;
    }

    /**
     * Sets the active editing locale and fires {@link #TOPIC_LOCALE}. The change event
     * fires only when {@code locale} differs from the current value, since
     * {@link PropertyChangeSupport} suppresses no-op changes.
     */
    public void setActiveLocale(@NotNull String locale) {
        String old = this.activeLocale;
        this.activeLocale = locale;
        pcs.firePropertyChange(TOPIC_LOCALE, old, locale);
    }

    /** Fires {@link #TOPIC_DIRTY} carrying the session's current unsaved-changes flag. */
    public void fireDirtyChanged(boolean dirty) {
        pcs.firePropertyChange(TOPIC_DIRTY, null, dirty);
    }

    /** Fires {@link #TOPIC_REQUEST_OPEN}. */
    public void fireOpenRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_OPEN, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_NEW}. */
    public void fireNewRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_NEW, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_SAVE}. */
    public void fireSaveRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_SAVE, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_SAVE_AS}. */
    public void fireSaveAsRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_SAVE_AS, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_QUIT}. */
    public void fireQuitRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_QUIT, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_UNDO}. */
    public void fireUndoRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_UNDO, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_REDO}. */
    public void fireRedoRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_REDO, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_SHORTCUTS}. */
    public void fireShortcutsRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_SHORTCUTS, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_ABOUT}. */
    public void fireAboutRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_ABOUT, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_MANAGE_LOCALES}. */
    public void fireManageLocalesRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_MANAGE_LOCALES, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_LOCALE} to signal a change in the loaded-locales set. */
    public void fireAvailableLocalesChanged() {
        pcs.firePropertyChange(TOPIC_LOCALE, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_EDIT_ABORTED} carrying {@code ex}. */
    public void fireEditAborted(@NotNull EditAbortedException ex) {
        pcs.firePropertyChange(TOPIC_EDIT_ABORTED, null, ex);
    }

    /** Fires {@link #TOPIC_SESSION} so subscribers re-read through the source. */
    public void fireSessionChanged() {
        pcs.firePropertyChange(TOPIC_SESSION, null, Boolean.TRUE);
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

    /** Fires {@link #TOPIC_COMMAND_STACK} carrying the current command-stack snapshot. */
    public void fireCommandStackChanged(@NotNull CommandStackSnapshot snapshot) {
        pcs.firePropertyChange(TOPIC_COMMAND_STACK, null, snapshot);
    }

    /** Subscribes {@code listener} to events on {@code topic}. */
    public void addListener(@NotNull String topic, @NotNull PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(topic, listener);
    }

    /** Unsubscribes {@code listener} from events on {@code topic}. */
    public void removeListener(@NotNull String topic, @NotNull PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(topic, listener);
    }
}
