package net.noiraude.creditseditor.ui;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.panel.CategoryPanel;
import net.noiraude.creditseditor.ui.panel.DetailPanel;
import net.noiraude.creditseditor.ui.panel.PersonPanel;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Composes the three content panels (category list, person list, detail form) and wires
 * inter-panel selection.
 *
 * <p>
 * Document state propagation is entirely event-driven through {@link DocumentBus}; this
 * class owns no document or refresh logic. It coordinates which card the detail panel
 * shows based on the current category and person selections, which are pure UI state.
 */
final class EditorView extends JPanel {

    private final @NotNull PersonPanel personPanel;
    private final @NotNull DetailPanel detailPanel;

    private @NotNull List<DocumentCategory> selectedCategories = List.of();
    private @NotNull List<DocumentPerson> selectedPersons = List.of();

    /**
     * @param bus       event bus that panels subscribe to for document mutations
     * @param onCommand executor for all structural and field-level commands
     */
    EditorView(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        detailPanel = new DetailPanel(bus, onCommand);

        personPanel = new PersonPanel(bus, onCommand, this::onPersonSelectionChanged);

        @NotNull
        CategoryPanel categoryPanel = new CategoryPanel(bus, onCommand, cats -> {
            boolean reClick = cats.equals(selectedCategories) && !selectedPersons.isEmpty();
            selectedCategories = cats;
            personPanel.setFilter(cats);
            detailPanel.setSelectedCategory(cats.size() == 1 ? cats.getFirst() : null);
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

    private void onPersonSelectionChanged(@NotNull List<DocumentPerson> persons) {
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
