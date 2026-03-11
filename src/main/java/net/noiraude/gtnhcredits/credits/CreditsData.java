package net.noiraude.gtnhcredits.credits;

import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CreditsData {

    public static final CreditsData EMPTY = new CreditsData(Collections.emptyList(), Collections.emptyList());

    public final List<CreditsCategory> categories;
    public final List<CreditsPerson> persons;

    public CreditsData(List<CreditsCategory> categories, List<CreditsPerson> persons) {
        this.categories = Collections.unmodifiableList(categories);
        this.persons = Collections.unmodifiableList(persons);
    }
}
