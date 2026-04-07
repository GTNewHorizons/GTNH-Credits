package net.noiraude.libcredits.parser;

/** Thrown when a {@code credits.json} file is structurally invalid. */
public final class CreditsParseException extends Exception {

    public CreditsParseException(String message) {
        super(message);
    }
}
