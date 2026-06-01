package net.noiraude.libcredits.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class LangDocumentTest {

    private static LangDocument parse(String text) throws Exception {
        return LangParser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String serialize(LangDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LangSerializer.write(doc, out);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    public void applyOwnedKeysFrom_overridesExistingOwnedKey() throws Exception {
        LangDocument target = parse("credits.category.team=Old\n");
        LangDocument source = parse("credits.category.team=New\n");

        target.applyOwnedKeysFrom(source);

        assertEquals("New", target.get("credits.category.team"));
    }

    @Test
    public void applyOwnedKeysFrom_addsOwnedKeyAbsentFromTarget() throws Exception {
        LangDocument target = parse("credits.category.team=Team\n");
        LangDocument source = parse("credits.category.team=Team\ncredits.person.role.dev=Developer\n");

        target.applyOwnedKeysFrom(source);

        assertEquals("Developer", target.get("credits.person.role.dev"));
    }

    @Test
    public void applyOwnedKeysFrom_dropsOwnedKeyAbsentFromSource() throws Exception {
        LangDocument target = parse("credits.category.team=Team\ncredits.category.support=Support\n");
        LangDocument source = parse("credits.category.team=Team\n");

        target.applyOwnedKeysFrom(source);

        assertNull("owned key absent from source must be dropped", target.get("credits.category.support"));
        assertEquals("Team", target.get("credits.category.team"));
    }

    @Test
    public void applyOwnedKeysFrom_leavesForeignKeysUntouched() throws Exception {
        LangDocument target = parse(
            "gui.credits.title=Credits\n" + "credits.category.team=Old\n" + "menu.singleplayer=Singleplayer\n");
        LangDocument source = parse("credits.category.team=New\n");

        target.applyOwnedKeysFrom(source);

        assertEquals("Credits", target.get("gui.credits.title"));
        assertEquals("Singleplayer", target.get("menu.singleplayer"));
        assertEquals("New", target.get("credits.category.team"));
    }

    @Test
    public void applyOwnedKeysFrom_ignoresForeignKeysInSource() throws Exception {
        LangDocument target = parse("credits.category.team=Team\n");
        LangDocument source = parse("credits.category.team=Team\ngui.foreign.key=Should not appear\n");

        target.applyOwnedKeysFrom(source);

        assertNull("source's foreign keys must not be transferred to target", target.get("gui.foreign.key"));
    }

    @Test
    public void applyOwnedKeysFrom_preservesBlankLinesAndComments() throws Exception {
        String original = "# header comment\n" + "\n"
            + "gui.credits.title=Credits\n"
            + "\n"
            + "# credits section\n"
            + "credits.category.team=Old\n";
        LangDocument target = parse(original);
        LangDocument source = parse("credits.category.team=New\n");

        target.applyOwnedKeysFrom(source);

        String written = serialize(target);
        assertTrue("comment must be preserved", written.contains("# header comment"));
        assertTrue("section comment must be preserved", written.contains("# credits section"));
        assertTrue("foreign key must be preserved", written.contains("gui.credits.title=Credits"));
        assertTrue("owned key must be updated", written.contains("credits.category.team=New"));
        assertFalse("old owned value must be gone", written.contains("credits.category.team=Old"));
    }

    @Test
    public void applyOwnedKeysFrom_emptySourceStripsAllOwnedKeys() throws Exception {
        LangDocument target = parse(
            "gui.credits.title=Credits\n" + "credits.category.team=Team\n" + "credits.person.role.dev=Developer\n");
        LangDocument source = LangParser.empty();

        target.applyOwnedKeysFrom(source);

        assertNull(target.get("credits.category.team"));
        assertNull(target.get("credits.person.role.dev"));
        assertEquals("Credits", target.get("gui.credits.title"));
    }

    @Test
    public void applyOwnedKeysFrom_appliesPendingInsertsFromSource() throws Exception {
        LangDocument target = parse("credits.category.team=Team\n");
        LangDocument source = LangParser.empty();
        source.set("credits.category.team", "Team");
        source.set("credits.category.support", "Support");

        target.applyOwnedKeysFrom(source);

        assertEquals("Team", target.get("credits.category.team"));
        assertEquals("Support", target.get("credits.category.support"));
    }

    @Test
    public void applyOwnedKeysFrom_dropsPendingInsertsAbsentFromSource() throws Exception {
        LangDocument target = parse("");
        target.set("credits.category.team", "Team");
        target.set("credits.category.support", "Support");
        LangDocument source = LangParser.empty();
        source.set("credits.category.team", "Team");

        target.applyOwnedKeysFrom(source);

        assertEquals("Team", target.get("credits.category.team"));
        assertNull(target.get("credits.category.support"));
    }

    @Test
    public void applyOwnedKeysFrom_coversNestedDetailKeys() throws Exception {
        LangDocument target = parse("credits.category.team.detail=old detail\n");
        LangDocument source = parse(
            "credits.category.team.detail=new detail\n" + "credits.category.team.detail.0=para zero\n"
                + "credits.category.team.detail.1=para one\n");

        target.applyOwnedKeysFrom(source);

        assertEquals("new detail", target.get("credits.category.team.detail"));
        assertEquals("para zero", target.get("credits.category.team.detail.0"));
        assertEquals("para one", target.get("credits.category.team.detail.1"));
    }
}
