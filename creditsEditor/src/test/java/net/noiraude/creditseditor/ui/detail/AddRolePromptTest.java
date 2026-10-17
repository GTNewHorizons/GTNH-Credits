package net.noiraude.creditseditor.ui.detail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.TestDocumentSession;
import net.noiraude.creditseditor.ui.SwingFinder;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.junit.jupiter.api.Test;

class AddRolePromptTest {

    @Test
    void collidingRoleInAnotherCategoryIsRejectedBeforeThePromptReturnsIt() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        CreditsDocument document = CreditsDocument.empty();
        document.categories.addAll(List.of(new DocumentCategory("dev"), new DocumentCategory("art")));
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("art", List.of(DocumentRole.local("Core.Dev"))));
        document.persons.add(alice);
        document.markClean();
        DocumentMembership destination = new DocumentMembership("dev");
        DocumentBus bus = new DocumentBus(() -> Optional.of(TestDocumentSession.of(document)));
        List<String> errors = new ArrayList<>();

        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame();
            Timer driver = new Timer(25, event -> submitRoleOrDismissError(owner, errors));
            Timer timeout = new Timer(5000, event -> owner.dispose());
            timeout.setRepeats(false);
            try {
                driver.start();
                timeout.start();
                assertTrue(
                    new AddRolePrompt(destination, document, bus).show(owner)
                        .isEmpty());
            } finally {
                driver.stop();
                timeout.stop();
                owner.dispose();
            }
        });

        assertEquals(1, errors.size());
        assertTrue(
            errors.getFirst()
                .contains("translation-key collision"));
        assertTrue(
            errors.getFirst()
                .contains("Core.Dev"));
        assertTrue(
            errors.getFirst()
                .contains("CoreDev"));
        assertTrue(destination.roles.isEmpty());
        assertFalse(document.isDirty());
    }

    private static void submitRoleOrDismissError(JFrame owner, List<String> errors) {
        for (var window : owner.getOwnedWindows()) {
            if (!window.isShowing() || !JDialog.class.isInstance(window)) continue;
            JDialog dialog = JDialog.class.cast(window);
            JOptionPane pane = (JOptionPane) dialog.getContentPane()
                .getComponent(0);
            var combo = SwingFinder.findByName(pane, "addRoleValue", JComboBox.class);
            if (combo.isPresent()) {
                combo.get()
                    .getEditor()
                    .setItem("CoreDev");
            } else {
                errors.add(Objects.toString(pane.getMessage()));
            }
            pane.setValue(JOptionPane.OK_OPTION);
        }
    }
}
