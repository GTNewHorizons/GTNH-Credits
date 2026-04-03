package net.noiraude.libcredits.model;

import java.util.Collections;
import java.util.List;

public final class CreditsData {

    public static final CreditsData EMPTY = new CreditsData(Collections.emptyList(), Collections.emptyList());

    public final List<CreditsCategory> categories;
    public final List<CreditsPerson> persons;

    public CreditsData(List<CreditsCategory> categories, List<CreditsPerson> persons) {
        this.categories = Collections.unmodifiableList(categories);
        this.persons = Collections.unmodifiableList(persons);
    }
}
