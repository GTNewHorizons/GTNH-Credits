package net.noiraude.libcredits.model;

import java.util.Objects;

/**
 * Immutable role label: either a credits-owned id or an exact Minecraft translation key.
 * Identity includes the source, so an external key never aliases a local role with the same value.
 */
public final class DocumentRole {

    private final String value;
    private final boolean external;

    private DocumentRole(String value, boolean external) {
        this.value = Objects.requireNonNull(value, "value");
        this.external = external;
        if (!(external ? CreditsKeys.isValidTranslationKey(value) : CreditsKeys.isValidId(value))) {
            throw new IllegalArgumentException("Invalid " + (external ? "translation key" : "role id") + ": " + value);
        }
    }

    /**
     * Creates a credits-owned role whose translation key is derived from its id.
     *
     * @throws NullPointerException     if {@code id} is null
     * @throws IllegalArgumentException if {@link CreditsKeys#isValidId(String)} rejects the id
     */
    public static DocumentRole local(String id) {
        return new DocumentRole(id, false);
    }

    /**
     * Creates a reference to an exact Minecraft translation key, without normalizing it
     * or checking whether it exists in any loaded language resources.
     *
     * @throws NullPointerException     if {@code key} is null
     * @throws IllegalArgumentException if {@link CreditsKeys#isValidTranslationKey(String)} rejects the key
     */
    public static DocumentRole translation(String key) {
        return new DocumentRole(key, true);
    }

    public String value() {
        return value;
    }

    public boolean isExternal() {
        return external;
    }

    /** Returns the derived credits-owned key or the external key exactly as supplied. */
    public String translationKey() {
        return external ? value : "credits.person.role." + CreditsKeys.sanitize(value);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        DocumentRole role = (DocumentRole) other;
        return external == role.external && value.equals(role.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, external);
    }

    @Override
    public String toString() {
        return value;
    }
}
