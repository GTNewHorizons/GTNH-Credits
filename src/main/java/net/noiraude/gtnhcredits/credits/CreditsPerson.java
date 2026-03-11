package net.noiraude.gtnhcredits.credits;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsPerson {

    public final String name;
    public final List<String> categories;
    public final List<String> roles;

    public CreditsPerson(String name, List<String> categories, List<String> roles) {
        this.name = name;
        this.categories = categories;
        this.roles = roles;
    }
}
