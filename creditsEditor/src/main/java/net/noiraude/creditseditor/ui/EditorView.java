package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.panel.CategoryPanel;
import net.noiraude.creditseditor.ui.panel.DetailPanel;
import net.noiraude.creditseditor.ui.panel.PersonPanel;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Composes the three content panels (category list, person list, detail form) and owns the
 * inter-panel selection state.
 *
 * <p>
 * Extracted from {@link MainWindow} to separate panel-level coordination from frame-level
 * concerns (I/O dialogs, menu bar, title). {@code MainWindow} holds one instance of this
 * class and delegates all panel interaction through it.
 */
final class EditorView extends JPanel {

    private final CategoryPanel categoryPanel;
    private final PersonPanel personPanel;
    private final DetailPanel detailPanel;

    private EditorSession session;
    private DocumentCategory selectedCategory;
    private DocumentPerson selectedPerson;

    /**
     * Constructs the view and wires inter-panel selection callbacks.
     *
     * @param onCommand executor for all structural and field-level commands
     */
    EditorView(CommandExecutor onCommand) {
        detailPanel = new DetailPanel(onCommand);

        personPanel = new PersonPanel(onCommand, person -> {
            selectedPerson = person;
            if (person != null) {
                detailPanel.showPerson(person);
            } else if (selectedCategory != null) {
                detailPanel.showCategory(selectedCategory);
            } else {
                detailPanel.showEmpty();
            }
        });

        categoryPanel = new CategoryPanel(onCommand, cat -> {
            selectedCategory = cat;
            if (cat != null) {
                personPanel.setFilter(cat);
                if (selectedPerson == null) {
                    detailPanel.showCategory(cat);
                }
            } else {
                personPanel.setFilter(null);
                if (selectedPerson == null) {
                    detailPanel.showEmpty();
                }
            }
        });

        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, personPanel, detailPanel);
        rightSplit.setDividerLocation(scaled(250));
        rightSplit.setResizeWeight(0.3);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryPanel, rightSplit);
        mainSplit.setDividerLocation(scaled(200));
        mainSplit.setResizeWeight(0.2);
        mainSplit.setBorder(new EmptyBorder(scaled(4), scaled(4), scaled(4), scaled(4)));

        setLayout(new BorderLayout());
        add(mainSplit, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Session lifecycle
    // -----------------------------------------------------------------------

    /**
     * Populates all panels from the session's documents and resets the selection state.
     * Call once immediately after a new session is loaded.
     */
    void loadSession(EditorSession newSession) {
        this.session = newSession;
        selectedCategory = null;
        selectedPerson = null;
        detailPanel.setContext(session.creditsDoc(), session.langDoc());
        categoryPanel.refresh(session.creditsDoc(), session.langDoc());
        personPanel.refresh(session.creditsDoc());
        detailPanel.showEmpty();
    }

    // -----------------------------------------------------------------------
    // Refresh
    // -----------------------------------------------------------------------

    /**
     * Rebuilds all panels from the current document state and re-applies the selection.
     *
     * <p>
     * Re-resolves {@code selectedCategory} and {@code selectedPerson} by id/name so that
     * reverting an Add command (which removes the object from the list) does not leave a
     * stale selection.
     *
     * <p>
     * Call after any structural command execute, undo, or redo.
     */
    void refreshAll() {
        var creditsDoc = session.creditsDoc();
        var langDoc = session.langDoc();

        if (selectedCategory != null) {
            String id = selectedCategory.id;
            selectedCategory = creditsDoc.categories.stream()
                .filter(c -> c.id.equals(id))
                .findFirst()
                .orElse(null);
        }
        if (selectedPerson != null) {
            String name = selectedPerson.name;
            selectedPerson = creditsDoc.persons.stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
        }

        categoryPanel.refresh(creditsDoc, langDoc);
        personPanel.refresh(creditsDoc);
        detailPanel.refresh(selectedCategory, selectedPerson);
    }

    /**
     * Repaints the category and person list cells without rebuilding the list models.
     * Sufficient after a light edit that changed only a display-name field.
     */
    void repaintLists() {
        categoryPanel.getList()
            .repaint();
        personPanel.getList()
            .repaint();
    }
}
