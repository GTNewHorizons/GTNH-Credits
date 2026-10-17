package net.noiraude.libcredits.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentRole;
import net.noiraude.libcredits.serializer.CreditsSerializer;
import net.noiraude.libcredits.validation.CreditsValidator;

import org.junit.Test;

public class CreditsFormatTest {

    private static final String MACHINE = "gt.blockmachines.Test-Machine.controller.name";

    private static CreditsDocument read(int version, String roles) throws Exception {
        String source = "{\"version\":" + version
            + ",\"category\":[{\"id\":\"dev\"}],\"person\":[{\"name\":\"Alice\",\"category\":{\"dev\":"
            + roles
            + "}}]}";
        return CreditsParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void versionTwoIsReadAndSavedAsVersionThreeWithoutChangingLocalRoles() throws Exception {
        CreditsDocument document = read(2, "[\"Core.Dev\",\"support\"]");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CreditsSerializer.write(document, output);
        assertTrue(
            output.toString("UTF-8")
                .contains("\"version\": 3"));
        CreditsDocument reopened = CreditsParser.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(document.persons, reopened.persons);
        assertEquals(
            "credits.person.role.coredev",
            reopened.persons.get(0).memberships.get(0).roles.get(0)
                .translationKey());
    }

    @Test
    public void legacyVersionTwoMetadataAndCombinedMembershipsRemainReadable() throws Exception {
        String source = "{\"version\":2,\"category\":[{\"id\":\"dev\"},{\"id\":\"quests\"}],"
            + "\"person\":[{\"name\":\"Alice\",\"username\":\"alice_account\","
            + "\"category\":{\"dev\":[\"lead\",\"developer\"],\"quests\":\"editor\"}}]}";
        CreditsDocument document = CreditsParser
            .parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Alice", document.persons.get(0).name);
        assertEquals(2, document.persons.get(0).memberships.size());
        assertEquals("dev", document.persons.get(0).memberships.get(0).categoryId);
        assertEquals(
            Arrays.asList(DocumentRole.local("lead"), DocumentRole.local("developer")),
            document.persons.get(0).memberships.get(0).roles);
        assertEquals("quests", document.persons.get(0).memberships.get(1).categoryId);
        assertEquals(Arrays.asList(DocumentRole.local("editor")), document.persons.get(0).memberships.get(1).roles);
        assertFalse(document.isDirty());
    }

    @Test
    public void versionThreeStillRejectsLegacyMetadataAndCombinedMembershipObjects() {
        for (String person : Arrays.asList(
            "{\"name\":\"Alice\",\"username\":\"alice_account\",\"category\":\"dev\"}",
            "{\"name\":\"Alice\",\"category\":{\"dev\":\"developer\",\"quests\":\"editor\"}}")) {
            String source = "{\"version\":3,\"category\":[{\"id\":\"dev\"},{\"id\":\"quests\"}],\"person\":[" + person
                + "]}";
            assertThrows(
                CreditsParseException.class,
                () -> CreditsParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    public void emptyMembershipObjectsCannotHideInCategoryLists() {
        for (int version : new int[] { 2, 3 }) {
            String source = "{\"version\":" + version
                + ",\"category\":[{\"id\":\"dev\"}],\"person\":[{\"name\":\"Alice\",\"category\":[{},\"dev\"]}]}";
            assertThrows(
                "version " + version + " must reject an empty membership object",
                CreditsParseException.class,
                () -> CreditsParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    public void mixedSourcesPreserveExactKeysOrderAndIdentityThroughRoundTrip() throws Exception {
        CreditsDocument document = read(
            3,
            "[\"Core.Dev\",{\"translationKey\":\"" + MACHINE + "\"},{\"translationKey\":\"Core.Dev\"}]");
        List<DocumentRole> roles = document.persons.get(0).memberships.get(0).roles;
        assertEquals(
            MACHINE,
            roles.get(1)
                .translationKey());
        assertNotEquals(roles.get(0), roles.get(2));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CreditsSerializer.write(document, output);
        assertEquals(
            roles,
            CreditsParser.parse(new ByteArrayInputStream(output.toByteArray())).persons.get(0).memberships
                .get(0).roles);
        document.markClean();
        roles.set(1, DocumentRole.translation("Changed.Key"));
        assertTrue(document.isDirty());
        roles.set(1, DocumentRole.translation(MACHINE));
        assertFalse(document.isDirty());
    }

    @Test
    public void referencesRequireVersionThreeAndRejectMalformedShapes() throws Exception {
        for (String reference : Arrays.asList(
            "{}",
            "{\"translationKey\":\"\"}",
            "{\"translationKey\":true}",
            "{\"translationKey\":\"has spaces\"}",
            "{\"translationKey\":\"valid.key\",\"extra\":1}")) {
            assertThrows(CreditsParseException.class, () -> read(3, reference));
        }
        assertThrows(CreditsParseException.class, () -> read(2, "{\"translationKey\":\"" + MACHINE + "\"}"));
        assertThrows(CreditsParseException.class, () -> read(4, "\"dev\""));
        assertThrows(CreditsParseException.class, () -> read(1, "\"dev\""));
        assertThrows(CreditsParseException.class, () -> read(3, "[\"dev\",\"dev\"]"));
    }

    @Test
    public void collisionsAreReportedBeforeTheyCanShareAnEnglishLabel() {
        assertThrows(CreditsParseException.class, () -> read(3, "[\"Core.Dev\",\"CoreDev\"]"));
    }

    @Test
    public void externalNamesAreNotRequiredInCreditsLanguageFiles() throws Exception {
        CreditsDocument document = read(3, "[\"developer\",{\"translationKey\":\"" + MACHINE + "\"}]");
        LangDocument english = LangParser.empty();
        english.set("credits.category.dev", "Developers");
        english.set("credits.person.role.developer", "Developer");
        assertTrue(
            CreditsValidator.validateContent(document, english, null)
                .isEmpty());
        assertTrue(
            CreditsValidator.validateContent(document, english, MACHINE::equals)
                .isEmpty());
        assertTrue(
            CreditsValidator.validateContent(document, english, key -> false)
                .get(0)
                .contains(MACHINE));
        english.remove("credits.person.role.developer");
        assertTrue(
            CreditsValidator.validateContent(document, english, null)
                .get(0)
                .contains("credits.person.role.developer"));
    }

    @Test
    public void descriptionsRequireTextBeyondParagraphSeparators() throws Exception {
        CreditsDocument document = read(3, "\"developer\"");
        document.categories.get(0).classes.add("detail");
        LangDocument english = LangParser.empty();
        english.set("credits.category.dev", "Developers");
        english.set("credits.person.role.developer", "Developer");
        english.set("credits.category.dev.detail", " ");
        english.set("credits.category.dev.detail.1", "");
        english.set("credits.category.dev.detail.2", " ");

        assertEquals(
            Arrays.asList("missing English description: credits.category.dev.detail"),
            CreditsValidator.validateContent(document, english, null));

        english.set("credits.category.dev.detail.2", "The people who develop the pack.");
        assertTrue(
            CreditsValidator.validateContent(document, english, null)
                .isEmpty());
    }

    @Test
    public void malformedJsonAndWrongFieldTypesHaveCheckedParseErrors() {
        for (String source : Arrays.asList(
            "[]",
            "null",
            "{",
            "{\"version\":3.5,\"category\":[]}",
            "{\"version\":\"3\",\"category\":[]}",
            "{\"version\":3,\"category\":false}")) {
            assertThrows(
                CreditsParseException.class,
                () -> CreditsParser.parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
