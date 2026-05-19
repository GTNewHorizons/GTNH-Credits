package net.noiraude.creditseditor.ui;

/** Persists the active session. */
@FunctionalInterface
interface SaveService {

    /** Returns whether the save completed successfully. */
    boolean trySave();
}
