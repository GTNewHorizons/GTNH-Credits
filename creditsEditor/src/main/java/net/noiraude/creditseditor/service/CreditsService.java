package net.noiraude.creditseditor.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.noiraude.creditseditor.ResourceManager;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.libcredits.model.CreditsCategory;
import net.noiraude.libcredits.model.CreditsData;
import net.noiraude.libcredits.model.CreditsPerson;
import net.noiraude.libcredits.parser.CreditsParseException;
import net.noiraude.libcredits.parser.CreditsParser;
import net.noiraude.libcredits.serializer.CreditsSerializer;

/**
 * Loads and saves the {@code credits.json} file via libCredits, bridging between the
 * immutable {@link CreditsData} model and the mutable {@link EditorModel}.
 *
 * <p>
 * On save, the data is serialized, then parsed back as a round-trip validation step.
 * Any schema violation surfaces as a {@link CreditsParseException} before writing to disk.
 */
public final class CreditsService {

    static final String CREDITS_PATH = "assets/gtnhcredits/credits.json";

    private CreditsService() {}

    /**
     * Loads {@code credits.json} from the resource manager and converts it to an
     * {@link EditorModel}.
     *
     * @throws IOException           if the file cannot be read
     * @throws CreditsParseException if the JSON is structurally invalid
     */
    public static EditorModel load(ResourceManager rm) throws IOException, CreditsParseException {
        try (InputStream in = rm.openRead(CREDITS_PATH)) {
            return toEditorModel(CreditsParser.parse(in));
        }
    }

    /**
     * Saves the {@link EditorModel} to {@code credits.json} via the resource manager.
     * Performs a round-trip parse before writing; throws if the data is invalid.
     *
     * @throws IOException           if the file cannot be written
     * @throws CreditsParseException if the serialized data fails re-parsing
     */
    public static void save(EditorModel model, ResourceManager rm) throws IOException, CreditsParseException {
        CreditsData data = toCreditsData(model);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CreditsSerializer.write(data, buf);
        byte[] bytes = buf.toByteArray();
        CreditsParser.parse(new ByteArrayInputStream(bytes)); // round-trip validation
        try (OutputStream out = rm.openWrite(CREDITS_PATH)) {
            out.write(bytes);
        }
    }

    // -----------------------------------------------------------------------
    // Conversion: CreditsData -> EditorModel
    // -----------------------------------------------------------------------

    /**
     * Converts an immutable {@link CreditsData} to a mutable {@link EditorModel}.
     * Lang-derived fields ({@code displayName}, {@code description}) are left empty;
     * callers should populate them from {@link LangDocument} after loading.
     */
    public static EditorModel toEditorModel(CreditsData data) {
        EditorModel model = new EditorModel();

        for (CreditsCategory cat : data.categories) {
            EditorCategory ec = new EditorCategory(cat.id);
            ec.classes = new LinkedHashSet<>(cat.classes);
            model.categories.add(ec);
        }

        for (CreditsPerson p : data.persons) {
            EditorPerson ep = new EditorPerson(p.name);
            for (Map.Entry<String, List<String>> entry : p.categoryRoles.entrySet()) {
                ep.memberships.add(new EditorMembership(entry.getKey(), entry.getValue()));
            }
            model.persons.add(ep);
        }

        return model;
    }

    // -----------------------------------------------------------------------
    // Conversion: EditorModel -> CreditsData
    // -----------------------------------------------------------------------

    /** Converts the mutable {@link EditorModel} to an immutable {@link CreditsData}. */
    public static CreditsData toCreditsData(EditorModel model) {
        List<CreditsCategory> categories = new ArrayList<>();
        for (EditorCategory ec : model.categories) {
            categories.add(new CreditsCategory(ec.id, Collections.unmodifiableSet(new LinkedHashSet<>(ec.classes))));
        }

        List<CreditsPerson> persons = new ArrayList<>();
        for (EditorPerson ep : model.persons) {
            Map<String, List<String>> categoryRoles = new LinkedHashMap<>();
            for (EditorMembership m : ep.memberships) {
                categoryRoles.put(m.categoryId, Collections.unmodifiableList(new ArrayList<>(m.roles)));
            }
            persons.add(new CreditsPerson(ep.name, categoryRoles));
        }

        return new CreditsData(categories, persons);
    }
}
