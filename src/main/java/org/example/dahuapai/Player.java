
package org.example.dahuapai;

import java.util.*;

public class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();
    private boolean hasWon = false;

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public boolean hasWon() {
        return hasWon;
    }

    public void declareWin() {
        hasWon = true;
    }

    public void addCards(List<Card> cards) {
        hand.addAll(cards);
    }

    public void removeCards(List<Card> cards) {
        hand.removeAll(cards);
    }
}

