package net.noiraude.creditseditor.ui.panel;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.component.McText;
import net.noiraude.creditseditor.ui.detail.BulkPersonView;
import net.noiraude.creditseditor.ui.detail.CategoryDetailView;
import net.noiraude.creditseditor.ui.detail.PersonDetailView;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Right-side panel that displays the appropriate detail form depending on what is selected
 * in the category or person panel.
 *
 * <p>
 * Uses a {@link CardLayout} to switch between an empty hint, a {@link CategoryDetailView},
 * a {@link PersonDetailView}, and a {@link BulkPersonView}. Callers drive the active card
 * via {@link #showEmpty()}, {@link #showCategory(DocumentCategory)},
 * {@link #showPerson(DocumentPerson)}, and {@link #showBulkPersons(List)}.
 * Call {@link #setContext(CreditsDocument, LangDocument)} once after a session loads.
 */
public final class DetailPanel extends JPanel {

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CATEGORY = "category";
    private static final String CARD_PERSON = "person";
    private static final String CARD_BULK = "bulk";

    private final TitledBorder detailBorder = BorderFactory.createTitledBorder("Details");
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel;
    private final CategoryDetailView categoryView;
    private final PersonDetailView personView;
    private final BulkPersonView bulkPersonView;

    public DetailPanel(CommandExecutor onCommand) {
        setLayout(new BorderLayout());
        setBorder(detailBorder);
        categoryView = new CategoryDetailView(onCommand);
        personView = new PersonDetailView(onCommand);
        bulkPersonView = new BulkPersonView(onCommand);

        cardPanel = new JPanel(cards);

        // Empty card
        JLabel hint = new JLabel("Select a category or person", SwingConstants.CENTER);
        hint.setFont(
            hint.getFont()
                .deriveFont(Font.ITALIC));
        hint.setForeground(java.awt.Color.GRAY);
        cardPanel.add(hint, CARD_EMPTY);

        cardPanel.add(categoryView, CARD_CATEGORY);
        cardPanel.add(personView, CARD_PERSON);
        cardPanel.add(bulkPersonView, CARD_BULK);

        add(cardPanel, BorderLayout.CENTER);
        cards.show(cardPanel, CARD_EMPTY);
    }

    /**
     * Sets the document context used by the detail views for lang reads/writes and
     * category lookups. Call once after a session is loaded.
     */
    public void setContext(CreditsDocument creditsDoc, LangDocument langDoc) {
        categoryView.setContext(langDoc);
        personView.setContext(creditsDoc, langDoc);
        bulkPersonView.setContext(creditsDoc, langDoc);
    }

    /** Shows the empty hint card. */
    public void showEmpty() {
        setDetailTitle("Details");
        cards.show(cardPanel, CARD_EMPTY);
    }

    /** Loads {@code category} into the category detail view and shows it. */
    public void showCategory(DocumentCategory category) {
        setDetailTitle("Category: " + category.id);
        categoryView.load(category);
        cards.show(cardPanel, CARD_CATEGORY);
    }

    /** Loads {@code person} into the person detail view and shows it. */
    public void showPerson(DocumentPerson person) {
        setDetailTitle("Person: " + McText.strip(person.name));
        personView.load(person);
        cards.show(cardPanel, CARD_PERSON);
    }

    /** Loads the bulk-operation view for the given multi-selection. */
    public void showBulkPersons(List<DocumentPerson> persons) {
        setDetailTitle(persons.size() + " persons selected");
        bulkPersonView.load(persons);
        cards.show(cardPanel, CARD_BULK);
    }

    /**
     * Refreshes the currently visible detail view from the document, preserving the displayed
     * item. Call after any external document change (undo, redo).
     */
    public void refresh(DocumentCategory selectedCategory, List<DocumentPerson> selectedPersons) {
        if (selectedPersons != null && selectedPersons.size() > 1) {
            showBulkPersons(selectedPersons);
        } else if (selectedPersons != null && selectedPersons.size() == 1) {
            showPerson(selectedPersons.getFirst());
        } else if (selectedCategory != null) {
            showCategory(selectedCategory);
        } else {
            showEmpty();
        }
    }

    private void setDetailTitle(String title) {
        detailBorder.setTitle(title);
        repaint();
    }
}
