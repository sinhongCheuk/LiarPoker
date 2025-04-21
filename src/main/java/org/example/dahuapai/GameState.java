
package org.example.dahuapai;

import java.util.*;

public class GameState {
    public List<Player> players = new ArrayList<>();
    public List<Card> deck = new ArrayList<>();
    public List<Card> tablePile = new ArrayList<>();
    public List<Card> discardPile = new ArrayList<>();
    public List<Player> winners = new ArrayList<>();

    public String currentDeclaredRank = null; // 当前声明的点数
    public int currentPlayerIndex = 0; // 当前轮到的玩家索引
    public Player lastPlayedPlayer = null; // 最近一次出牌者
    public List<Card> lastPlayedCards = new ArrayList<>(); // 最近一次出牌的牌

    public boolean roundOngoing = true; // 当前回合是否继续
    public Set<Player> passedPlayers = new HashSet<>(); // 当前回合已过牌的玩家
}

