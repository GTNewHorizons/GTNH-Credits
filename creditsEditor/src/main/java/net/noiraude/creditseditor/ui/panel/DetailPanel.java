package net.noiraude.creditseditor.ui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.command.CommandExecutor;
import net.noiraude.creditseditor.mc.McText;
import net.noiraude.creditseditor.ui.detail.BulkPersonView;
import net.noiraude.creditseditor.ui.detail.CategoryDetailView;
import net.noiraude.creditseditor.ui.detail.PersonDetailView;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right-side panel that displays the appropriate detail form depending on what is selected
 * in the category or person panel.
 *
 * <p>
 * Uses a {@link CardLayout} to switch between an empty hint, a {@link CategoryDetailView},
 * a {@link PersonDetailView}, and a {@link BulkPersonView}. Callers drive the active card
 * via {@link #showEmpty()}, {@link #showCategory(DocumentCategory)},
 * {@link #showPerson(DocumentPerson)}, and {@link #showBulkPersons(List)}. This panel owns
 * the bus subscription that detects when the currently displayed subject was removed from
 * the document and falls back to the empty card in that case, so the individual detail
 * views remain pure form widgets.
 */
public final class DetailPanel extends JPanel {

    private static final @NotNull String CARD_EMPTY = "empty";
    private static final @NotNull String CARD_CATEGORY = "category";
    private static final @NotNull String CARD_PERSON = "person";
    private static final @NotNull String CARD_BULK = "bulk";

    private enum Mode {
        EMPTY,
        CATEGORY,
        PERSON,
        BULK
    }

    private final @NotNull DocumentBus bus;
    private final @NotNull TitledBorder detailBorder = BorderFactory.createTitledBorder("Details");
    private final @NotNull CardLayout cards = new CardLayout();
    private final @NotNull JPanel cardPanel;
    private final @NotNull CategoryDetailView categoryView;
    private final @NotNull PersonDetailView personView;
    private final @NotNull BulkPersonView bulkPersonView;

    private @NotNull Mode mode = Mode.EMPTY;
    private @Nullable DocumentCategory currentCategory;
    private @Nullable DocumentPerson currentPerson;

    public DetailPanel(@NotNull DocumentBus bus, @NotNull CommandExecutor onCommand) {
        assert SwingUtilities.isEventDispatchThread();
        this.bus = bus;
        setLayout(new BorderLayout());
        setBorder(detailBorder);
        categoryView = new CategoryDetailView(bus, onCommand);
        personView = new PersonDetailView(bus, onCommand);
        bulkPersonView = new BulkPersonView(bus, onCommand, this::onBulkResolved);

        cardPanel = new JPanel(cards);

        JLabel hint = new JLabel("Select a category or person", SwingConstants.CENTER);
        hint.setFont(
            hint.getFont()
                .deriveFont(Font.ITALIC));
        hint.setForeground(java.awt.Color.GRAY);
        cardPanel.add(hint, CARD_EMPTY);

        cardPanel.add(categoryView, CARD_CATEGORY);
        cardPanel.add(createPersonScroll(), CARD_PERSON);
        cardPanel.add(bulkPersonView, CARD_BULK);

        add(cardPanel, BorderLayout.CENTER);
        cards.show(cardPanel, CARD_EMPTY);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> showEmpty());
        bus.addListener(DocumentBus.TOPIC_CATEGORIES, e -> onCategoriesChanged());
        bus.addListener(DocumentBus.TOPIC_PERSONS, e -> onPersonsChanged());
    }

    /** Shows the empty hint card. */
    public void showEmpty() {
        assert SwingUtilities.isEventDispatchThread();
        mode = Mode.EMPTY;
        currentCategory = null;
        currentPerson = null;
        categoryView.clear();
        personView.clear();
        setDetailTitle("Details");
        cards.show(cardPanel, CARD_EMPTY);
    }

    /** Loads {@code category} into the category detail view and shows it. */
    public void showCategory(@NotNull DocumentCategory category) {
        assert SwingUtilities.isEventDispatchThread();
        mode = Mode.CATEGORY;
        currentCategory = category;
        currentPerson = null;
        setDetailTitle("Category: " + category.id);
        categoryView.load(category);
        cards.show(cardPanel, CARD_CATEGORY);
    }

    /** Loads {@code person} into the person detail view and shows it. */
    public void showPerson(@NotNull DocumentPerson person) {
        assert SwingUtilities.isEventDispatchThread();
        mode = Mode.PERSON;
        currentPerson = person;
        currentCategory = null;
        setDetailTitle("Person: " + McText.strip(person.name));
        personView.load(person);
        cards.show(cardPanel, CARD_PERSON);
    }

    /** Updates the selected category so bulk-operation pickers can default to it. */
    public void setSelectedCategory(@Nullable DocumentCategory category) {
        assert SwingUtilities.isEventDispatchThread();
        bulkPersonView.setSelectedCategory(category);
    }

    /** Loads the bulk-operation view for the given multi-selection. */
    public void showBulkPersons(@NotNull List<DocumentPerson> persons) {
        assert SwingUtilities.isEventDispatchThread();
        mode = Mode.BULK;
        setDetailTitle(persons.size() + " persons selected");
        bulkPersonView.load(persons);
        cards.show(cardPanel, CARD_BULK);
    }

    private void onCategoriesChanged() {
        if (mode != Mode.CATEGORY || currentCategory == null) return;
        DocumentCategory target = currentCategory;
        boolean present = bus.creditsDoc().categories.stream()
            .anyMatch(c -> c == target);
        if (!present) showEmpty();
    }

    private void onPersonsChanged() {
        if (mode != Mode.PERSON || currentPerson == null) return;
        DocumentPerson target = currentPerson;
        boolean present = bus.creditsDoc().persons.stream()
            .anyMatch(p -> p == target);
        if (!present) showEmpty();
    }

    private void onBulkResolved(@NotNull List<DocumentPerson> remaining) {
        if (mode != Mode.BULK) return;
        if (remaining.isEmpty()) showEmpty();
        else if (remaining.size() == 1) showPerson(remaining.getFirst());
        else setDetailTitle(remaining.size() + " persons selected");
    }

    private void setDetailTitle(@NotNull String title) {
        detailBorder.setTitle(title);
        repaint();
    }

    // Report the view's full minimum height as the scroll pane's own minimum, so it
    // propagates up through DetailPanel, EditorView, and the frame's content pane when
    // MainWindow computes setMinimumSize(). The scroll pane still lets the user see
    // overflow if the viewport does become smaller than the view.
    private @NotNull JScrollPane createPersonScroll() {
        JScrollPane personScroll = new JScrollPane(personView) {

            @Override
            public Dimension getMinimumSize() {
                Dimension viewMin = personView.getMinimumSize();
                Dimension superMin = super.getMinimumSize();
                return new Dimension(
                    Math.max(superMin.width, viewMin.width),
                    Math.max(superMin.height, viewMin.height));
            }
        };
        personScroll.setBorder(null);
        personScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        personScroll.getVerticalScrollBar()
            .setUnitIncrement(16);
        return personScroll;
    }
}
