package net.noiraude.gtnhcredits.credits;

import java.util.Set;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsCategory {

    public final String id;
    /** Semantic class markers, e.g. {@code "person"}, {@code "role"}, {@code "detail"}. */
    public final Set<String> classes;

    public CreditsCategory(String id, Set<String> classes) {
        this.id = id;
        this.classes = classes;
    }
}
