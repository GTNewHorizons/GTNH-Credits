package net.noiraude.creditseditor.ui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.creditseditor.ui.detail.CategoryDetailView;
import net.noiraude.creditseditor.ui.detail.PersonDetailView;

/**
 * Right-side panel that displays the appropriate detail form depending on what is selected
 * in the category or person panel.
 *
 * <p>
 * Uses a {@link CardLayout} to switch between an empty hint, a {@link CategoryDetailView},
 * and a {@link PersonDetailView}. Callers drive the active card via {@link #showEmpty()},
 * {@link #showCategory(EditorCategory)}, and {@link #showPerson(EditorPerson, EditorModel)}.
 */
public final class DetailPanel extends JPanel {

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CATEGORY = "category";
    private static final String CARD_PERSON = "person";

    private final CardLayout cards = new CardLayout();
    private final CategoryDetailView categoryView;
    private final PersonDetailView personView;

    public DetailPanel(Consumer<Command> onCommand) {
        setLayout(new BorderLayout());
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

    /** Shows the empty hint card. */
    public void showEmpty() {
        cards.show((JPanel) getComponent(0), CARD_EMPTY);
    }

    /** Loads {@code category} into the category detail view and shows it. */
    public void showCategory(EditorCategory category) {
        categoryView.load(category);
        cards.show((JPanel) getComponent(0), CARD_CATEGORY);
    }

    /** Loads {@code person} into the person detail view and shows it. */
    public void showPerson(EditorPerson person, EditorModel model) {
        personView.load(person, model);
        cards.show((JPanel) getComponent(0), CARD_PERSON);
    }

    /**
     * Refreshes the currently visible detail view from the model, preserving the displayed
     * item. Call after any external model change (undo, redo).
     */
    public void refresh(EditorCategory selectedCategory, EditorPerson selectedPerson, EditorModel model) {
        if (selectedPerson != null) {
            personView.load(selectedPerson, model);
            cards.show((JPanel) getComponent(0), CARD_PERSON);
        } else if (selectedCategory != null) {
            categoryView.load(selectedCategory);
            cards.show((JPanel) getComponent(0), CARD_CATEGORY);
        } else {
            cards.show((JPanel) getComponent(0), CARD_EMPTY);
        }
    }
}
