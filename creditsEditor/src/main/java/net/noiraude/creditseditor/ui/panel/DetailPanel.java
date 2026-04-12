package net.noiraude.creditseditor.ui.panel;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.ui.component.McText;
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
 * and a {@link PersonDetailView}. Callers drive the active card via {@link #showEmpty()},
 * {@link #showCategory(DocumentCategory)}, and {@link #showPerson(DocumentPerson)}.
 * Call {@link #setContext(CreditsDocument, LangDocument)} once after a session loads.
 */
public final class DetailPanel extends JPanel {

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CATEGORY = "category";
    private static final String CARD_PERSON = "person";

    private final TitledBorder detailBorder = BorderFactory.createTitledBorder("Details");
    private final CardLayout cards = new CardLayout();
    private final CategoryDetailView categoryView;
    private final PersonDetailView personView;

    public DetailPanel(CommandExecutor onCommand) {
        setLayout(new BorderLayout());
        setBorder(detailBorder);
        categoryView = new CategoryDetailView(onCommand);
        personView = new PersonDetailView(onCommand);

        JPanel cardPanel = new JPanel(cards);

        // Empty card
        JLabel hint = new JLabel("Select a category or person", SwingConstants.CENTER);
        hint.setFont(
            hint.getFont()
                .deriveFont(Font.ITALIC));
        hint.setForeground(java.awt.Color.GRAY);
        cardPanel.add(hint, CARD_EMPTY);

        cardPanel.add(categoryView, CARD_CATEGORY);
        cardPanel.add(personView, CARD_PERSON);

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
    }

    /** Shows the empty hint card. */
    public void showEmpty() {
        setDetailTitle("Details");
        cards.show((JPanel) getComponent(0), CARD_EMPTY);
    }

    /** Loads {@code category} into the category detail view and shows it. */
    public void showCategory(DocumentCategory category) {
        setDetailTitle("Category: " + category.id);
        categoryView.load(category);
        cards.show((JPanel) getComponent(0), CARD_CATEGORY);
    }

    /** Loads {@code person} into the person detail view and shows it. */
    public void showPerson(DocumentPerson person) {
        setDetailTitle("Person: " + McText.strip(person.name));
        personView.load(person);
        cards.show((JPanel) getComponent(0), CARD_PERSON);
    }

    /**
     * Refreshes the currently visible detail view from the document, preserving the displayed
     * item. Call after any external document change (undo, redo).
     */
    public void refresh(DocumentCategory selectedCategory, DocumentPerson selectedPerson) {
        if (selectedPerson != null) {
            setDetailTitle("Person: " + McText.strip(selectedPerson.name));
            personView.load(selectedPerson);
            cards.show((JPanel) getComponent(0), CARD_PERSON);
        } else if (selectedCategory != null) {
            setDetailTitle("Category: " + selectedCategory.id);
            categoryView.load(selectedCategory);
            cards.show((JPanel) getComponent(0), CARD_CATEGORY);
        } else {
            setDetailTitle("Details");
            cards.show((JPanel) getComponent(0), CARD_EMPTY);
        }
    }

    private void setDetailTitle(String title) {
        detailBorder.setTitle(title);
        repaint();
    }
}
