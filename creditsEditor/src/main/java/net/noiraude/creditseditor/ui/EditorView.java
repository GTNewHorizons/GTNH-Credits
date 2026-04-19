package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.panel.CategoryPanel;
import net.noiraude.creditseditor.ui.panel.DetailPanel;
import net.noiraude.creditseditor.ui.panel.PersonPanel;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private final @NotNull CategoryPanel categoryPanel;
    private final @NotNull PersonPanel personPanel;
    private final @NotNull DetailPanel detailPanel;

    private @Nullable EditorSession session;
    private @NotNull List<DocumentCategory> selectedCategories = List.of();
    private @NotNull List<DocumentPerson> selectedPersons = List.of();

    /**
     * Constructs the view and wires inter-panel selection callbacks.
     *
     * @param onCommand executor for all structural and field-level commands
     */
    EditorView(@NotNull CommandExecutor onCommand) {
        detailPanel = new DetailPanel(onCommand);

        personPanel = new PersonPanel(onCommand, this::accept);

        categoryPanel = new CategoryPanel(onCommand, cats -> {
            DocumentCategory sole = cats.size() == 1 ? cats.getFirst() : null;
            DocumentCategory prevSole = selectedCategories.size() == 1 ? selectedCategories.getFirst() : null;
            boolean reClick = sole != null && sole == prevSole && !selectedPersons.isEmpty();
            selectedCategories = cats;
            detailPanel.setSelectedCategory(sole);
            personPanel.setFilter(cats);
            if (selectedPersons.isEmpty() || reClick) {
                selectedPersons = List.of();
                showSoleCategoryOrEmpty();
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
    void loadSession(@NotNull EditorSession newSession) {
        this.session = newSession;
        selectedCategories = List.of();
        selectedPersons = List.of();
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
        if (session == null) return;
        var creditsDoc = session.creditsDoc();
        var langDoc = session.langDoc();

        if (!selectedCategories.isEmpty()) {
            List<String> ids = selectedCategories.stream()
                .map(c -> c.id)
                .toList();
            selectedCategories = creditsDoc.categories.stream()
                .filter(c -> ids.contains(c.id))
                .toList();
        }
        if (!selectedPersons.isEmpty()) {
            List<String> names = selectedPersons.stream()
                .map(p -> p.name)
                .toList();
            selectedPersons = creditsDoc.persons.stream()
                .filter(p -> names.contains(p.name))
                .toList();
        }

        categoryPanel.refresh(creditsDoc, langDoc);
        personPanel.refresh(creditsDoc);
        DocumentCategory soleCategory = selectedCategories.size() == 1 ? selectedCategories.getFirst() : null;
        detailPanel.refresh(soleCategory, selectedPersons.isEmpty() ? null : selectedPersons);
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

    private void accept(@NotNull List<DocumentPerson> persons) {
        selectedPersons = persons;
        if (persons.size() > 1) {
            detailPanel.showBulkPersons(persons);
            return;
        }
        if (persons.size() == 1) {
            detailPanel.showPerson(persons.getFirst());
            return;
        }
        showSoleCategoryOrEmpty();
    }

    private void showSoleCategoryOrEmpty() {
        if (selectedCategories.size() == 1) detailPanel.showCategory(selectedCategories.getFirst());
        else detailPanel.showEmpty();
    }
}
