
package org.example.dahuapai;

import java.util.*;

/**
 * 大话牌 - 控制主流程
 * 包含：
 * - 玩家声明、出牌、质疑、过牌机制
 * - 延迟胜出判断
 * - 轮次控制与胜出者顺序记录
 */

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GameState gameState = new GameState();

        System.out.print("请输入玩家人数（2～4人）：");
        int numPlayers = Integer.parseInt(scanner.nextLine());
        for (int i = 1; i <= numPlayers; i++) {
            System.out.print("请输入玩家" + i + "的名字：");
            String name = scanner.nextLine();
            gameState.players.add(new Player(name));
        }

        initializeDeck(gameState);
        dealCards(gameState);

        while (gameState.winners.size() < gameState.players.size() - 1) {
            playOneRound(gameState, scanner);
        }

        System.out.println("\n游戏结束！胜利排名如下：");
        for (int i = 0; i < gameState.winners.size(); i++) {
            System.out.println((i + 1) + ". " + gameState.winners.get(i).getName());
        }
        for (Player p : gameState.players) {
            if (!p.hasWon()) {
                System.out.println((gameState.winners.size() + 1) + ". " + p.getName() + "（最后一名）");
            }
        }
    }

    public static void initializeDeck(GameState gameState) {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"2", "3", "4"};
        for (String suit : suits) {
            for (String rank : ranks) {
                gameState.deck.add(new Card(suit, rank));
            }
        }
        gameState.deck.add(new Card("JOKER", "JOKER"));
        gameState.deck.add(new Card("JOKER", "JOKER"));
        Collections.shuffle(gameState.deck);
    }

    public static void dealCards(GameState gameState) {
        int totalCards = gameState.deck.size();
        int numPlayers = gameState.players.size();
        for (int i = 0; i < totalCards; i++) {
            gameState.players.get(i % numPlayers).getHand().add(gameState.deck.get(i));
        }
    }

    static void playOneRound(GameState gameState, Scanner scanner) {
        while (gameState.currentPlayerIndex < gameState.players.size()
                && gameState.players.get(gameState.currentPlayerIndex).hasWon()) {
            gameState.currentPlayerIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size();
        }

        Player starter = gameState.players.get(gameState.currentPlayerIndex);
        System.out.println("\n=== 新一轮开始，由 " + starter.getName() + " 开始声明 ===");

        showHand(starter);
        String declaredRank;
        while (true) {
            System.out.print(starter.getName() + "，请输入本轮声明点数（例如：8）：");
            declaredRank = scanner.nextLine().trim().toUpperCase();
            if (isValidRank(declaredRank)) break;
            System.out.println("非法输入，请重新输入！");
        }

        gameState.currentDeclaredRank = declaredRank;
        gameState.roundOngoing = true;
        gameState.tablePile.clear();
        gameState.lastPlayedCards.clear();
        gameState.passedPlayers.clear();

        int playerCount = gameState.players.size();
        int turnsTaken = 0;

        while (true) {
            Player currentPlayer = gameState.players.get(gameState.currentPlayerIndex);
            if (!currentPlayer.hasWon()) {
                processPlayerTurn(gameState, scanner, currentPlayer, turnsTaken == 0);
                if (!gameState.roundOngoing) return;
                turnsTaken++;
            }

            long remainingActive = gameState.players.stream()
                    .filter(p -> !p.hasWon() && p != gameState.lastPlayedPlayer)
                    .filter(p -> !gameState.passedPlayers.contains(p))
                    .count();

            if (remainingActive == 0) {
                System.out.println("所有其他玩家都选择过牌，桌面牌进入弃牌堆，" + gameState.lastPlayedPlayer.getName() + " 重新声明新一轮。");
                gameState.discardPile.addAll(gameState.tablePile);
                gameState.tablePile.clear();
                gameState.currentDeclaredRank = null;
                gameState.currentPlayerIndex = gameState.players.indexOf(gameState.lastPlayedPlayer);
                return;
            }

            do {
                gameState.currentPlayerIndex = (gameState.currentPlayerIndex + 1) % playerCount;
            } while (gameState.players.get(gameState.currentPlayerIndex).hasWon());
        }
    }

    static void processPlayerTurn(GameState gameState, Scanner scanner, Player player, boolean isFirst) {
        if (player.hasWon()) return;

        System.out.println("\n轮到：" + player.getName());
        showHand(player);

        boolean choosePlay = isFirst || askYesOrNo(scanner, "是否要出牌？（y=出牌 / n=过）：");
        if (!choosePlay) {
            System.out.println(player.getName() + " 选择过牌！");
            gameState.passedPlayers.add(player);
            return;
        }

        List<Card> chosen = chooseCards(scanner, player);
        gameState.tablePile.addAll(chosen);
        player.removeCards(chosen);
        gameState.lastPlayedPlayer = player;
        gameState.lastPlayedCards = new ArrayList<>(chosen);

        boolean challenged = false;
        for (Player challenger : gameState.players) {
            if (challenger == player || challenger.hasWon()) continue;
            if (askYesOrNo(scanner, challenger.getName() + "，是否质疑？（y/n）：")) {
                resolveChallenge(gameState, player, challenger);
                challenged = true;
                break;
            }
        }

        if (!challenged && player.getHand().isEmpty() && !player.hasWon()) {
            gameState.winners.add(player);
            player.declareWin();
            System.out.println(player.getName() + " 🎉 胜出！退出游戏。");
        }
    }

    static void resolveChallenge(GameState gameState, Player declarer, Player challenger) {
        System.out.println("\n" + challenger.getName() + " 对 " + declarer.getName() + " 发起了质疑！");

        boolean declaredIsTrue = true;
        for (Card c : gameState.lastPlayedCards) {
            if (c.isJoker()) continue;
            if (!c.getRank().equalsIgnoreCase(gameState.currentDeclaredRank)) {
                declaredIsTrue = false;
                break;
            }
        }

        boolean challengeSuccessful = !declaredIsTrue;

        if (challengeSuccessful) {
            System.out.println("质疑成功！桌面牌归 " + declarer.getName());
            declarer.addCards(gameState.tablePile);
            gameState.currentPlayerIndex = gameState.players.indexOf(challenger);
        } else {
            System.out.println("质疑失败！桌面牌归 " + challenger.getName());
            challenger.addCards(gameState.tablePile);
            if (declarer.getHand().isEmpty() && !declarer.hasWon()) {
                gameState.winners.add(declarer);
                declarer.declareWin();
                System.out.println(declarer.getName() + " 🎉 胜出！退出游戏。");
            }
        }

        gameState.tablePile.clear();
        gameState.roundOngoing = false;
    }

    static List<Card> chooseCards(Scanner scanner, Player player) {
        List<Card> selected = new ArrayList<>();
        while (true) {
            System.out.print("请输入要打出的牌的索引（空格分隔，例如 0 2 3）：");
            String[] parts = scanner.nextLine().trim().split("\\s+");
            try {
                for (String p : parts) {
                    int index = Integer.parseInt(p);
                    selected.add(player.getHand().get(index));
                }
                if (selected.size() >= 1 && selected.size() <= 4) break;
            } catch (Exception e) {
                System.out.println("输入有误，请重新选择。");
                selected.clear();
            }
        }
        return selected;
    }

    static void showHand(Player player) {
        List<Card> hand = player.getHand();
        System.out.print(player.getName() + " 的手牌: ");
        for (int i = 0; i < hand.size(); i++) {
            System.out.print("(" + i + ")" + hand.get(i) + " ");
        }
        System.out.println();
    }

    static boolean askYesOrNo(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String s = scanner.nextLine().trim().toLowerCase();
        return s.equals("y");
    }

    public static boolean isValidRank(String rank) {
        return List.of("2", "3", "4").contains(rank);
    }
}

