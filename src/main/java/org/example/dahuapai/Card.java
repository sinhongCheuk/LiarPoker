
package org.example.dahuapai;

import java.util.*;
/**
 * 表示一张扑克牌。
 * 每张牌包含点数（rank）和花色（suit）。
 * 特殊点数 "JOKER" 表示大小王，其没有花色。
 */
public class Card {
    private final String suit; // 花色：♠ ♥ ♦ ♣ 或 JOKER
    private final String rank; // 点数：2~10, J, Q, K, A, JOKER

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public boolean isJoker() {
        return rank.equalsIgnoreCase("JOKER");
    }

    @Override
    public String toString() {
        return isJoker() ? "JOKER" : suit + rank;
    }
}
