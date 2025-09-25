package mage.client.game;

import mage.client.cards.BigCard;
import mage.client.cards.Cards;
import mage.client.util.GUISizeHelper;
import mage.view.CardsView;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

/**
 * Floating window for displaying the stack
 */
public class FloatingStackWindow extends JFrame {

    private final Cards stackCards;
    private boolean isVisible = false;

    public FloatingStackWindow() {
        super("Stack");

        // Create the stack cards panel
        stackCards = new Cards();
        stackCards.setBackgroundColor(new Color(0, 0, 0, 40));

        // Set up the window
        initializeWindow();
    }

    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);

        // Create scroll pane for the stack
        JScrollPane scrollPane = new JScrollPane(stackCards);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Set up layout
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // Set initial size and position
        setSize(300, 400);
        setLocationRelativeTo(null);

        // Make it resizable
        setResizable(true);
    }

    public void updateStack(CardsView stackCards, BigCard bigCard, UUID gameId) {
        if (stackCards == null || stackCards.isEmpty()) {
            if (isVisible) {
                setVisible(false);
                isVisible = false;
            }
            return;
        }

        // Load the stack cards
        this.stackCards.loadCards(stackCards, bigCard, gameId, true);

        // Show the window if it's not visible and there are cards
        if (!isVisible && !stackCards.isEmpty()) {
            setVisible(true);
            isVisible = true;
        }
    }

    public void changeGUISize() {
        stackCards.setCardDimension(GUISizeHelper.handCardDimension);
        stackCards.changeGUISize();
    }

    public void cleanUp() {
        stackCards.cleanUp();
        setVisible(false);
        isVisible = false;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        isVisible = visible;
    }

    public boolean isStackVisible() {
        return isVisible;
    }
}