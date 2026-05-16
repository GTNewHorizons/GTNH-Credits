package net.noiraude.creditseditor.bus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import net.noiraude.creditseditor.command.CommandStackSnapshot;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * The bus is constructed once by the main window and lives for the whole application.
 * Opening a new resource replaces the documents via {@link #setSession}, which fires
 * {@link #TOPIC_SESSION} so every subscriber rebuilds against the new documents.
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

    /** The user requested to add a new editing locale to the active session. */
    public static final @NotNull String TOPIC_REQUEST_ADD_LOCALE = "request.addLocale";

    /** The user requested to remove the active editing locale from the session. */
    public static final @NotNull String TOPIC_REQUEST_REMOVE_LOCALE = "request.removeLocale";

    private final @NotNull PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private @Nullable DocumentSession session;
    private @NotNull String activeLocale = LangResolver.DEFAULT_LOCALE;

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

    /**
     * Returns the lang document for {@code locale}, or {@code null} if that locale is
     * not loaded. Throws when no session is loaded.
     */
    @Contract(pure = true)
    public @Nullable LangDocument langDoc(@NotNull String locale) {
        return requireSession().langDocs()
            .get(locale);
    }

    private @NotNull DocumentSession requireSession() {
        return Objects.requireNonNull(session, "No session loaded");
    }

    /** Returns {@code true} once a session has been loaded via {@link #setSession}. */
    @Contract(pure = true)
    public boolean hasSession() {
        return session != null;
    }

    /**
     * Returns the locale tags loaded in the current session, in insertion order, or an empty
     * set when no session is loaded.
     */
    @Contract(pure = true)
    public @NotNull @UnmodifiableView Set<String> availableLocales() {
        if (session == null) return Collections.emptySet();
        return Collections.unmodifiableSet(
            session.langDocs()
                .keySet());
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

    /** Fires {@link #TOPIC_REQUEST_ADD_LOCALE}. */
    public void fireAddLocaleRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_ADD_LOCALE, null, Boolean.TRUE);
    }

    /** Fires {@link #TOPIC_REQUEST_REMOVE_LOCALE}. */
    public void fireRemoveLocaleRequested() {
        pcs.firePropertyChange(TOPIC_REQUEST_REMOVE_LOCALE, null, Boolean.TRUE);
    }

    /** Binds the bus to the active session and fires {@link #TOPIC_SESSION}. */
    public void setSession(@NotNull DocumentSession session) {
        this.session = session;
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
}
