package org.example.dahuapai;

import java.util.*;

/**
 * 大话牌 - 控制主流程主类
 * 本类负责整个游戏的初始化、发牌、回合循环、质疑机制与胜负判定。
 */
public class Main {
    public static void main(String[] args) {
        // 创建扫描器用于读取控制台输入
        Scanner scanner = new Scanner(System.in);
        // 创建游戏状态对象，保存玩家、牌堆、桌面牌等信息
        GameState gameState = new GameState();

        // 1. 读取玩家人数和名字
        System.out.print("请输入玩家人数（2～4人）：");
        int numPlayers = Integer.parseInt(scanner.nextLine());  // 将输入的字符串解析为整数
        for (int i = 1; i <= numPlayers; i++) {
            System.out.print("请输入玩家" + i + "的名字：");
            String name = scanner.nextLine();                  // 读取玩家姓名
            gameState.players.add(new Player(name));          // 将玩家添加到游戏状态中
        }

        // 2. 初始化牌堆并发牌
        initializeDeck(gameState);    // 构造并打乱一副牌，保存在 gameState.deck
        dealCards(gameState);         // 将牌均匀分发给每位玩家

        // 3. 主循环：当胜出玩家数小于总玩家数-1 时持续进行回合
        while (gameState.winners.size() < gameState.players.size() - 1) {
            playOneRound(gameState, scanner);  // 进行单个回合（声明→出牌→质疑→过牌等）
        }

        // 4. 所有玩家胜出后，打印最终排名
        System.out.println("\n游戏结束！胜利排名如下：");
        for (int i = 0; i < gameState.winners.size(); i++) {
            // 按胜出顺序输出玩家名
            System.out.println((i + 1) + ". " + gameState.winners.get(i).getName());
        }
        for (Player p : gameState.players) {
            // 剩余未胜出玩家为最后一名
            if (!p.hasWon()) {
                System.out.println((gameState.winners.size() + 1) + ". " + p.getName() + "（最后一名）");
            }
        }
    }

    /**
     * 初始化扑克牌：生成 54 张（含大小王）并打乱顺序
     */
    public static void initializeDeck(GameState gameState) {
        String[] suits = {"♠", "♥", "♦", "♣"};     // 四种花色
        String[] ranks = {"2", "3", "4"};             // 本示例仅用 2~4
        // 生成每种花色和点数的牌
        for (String suit : suits) {
            for (String rank : ranks) {
                gameState.deck.add(new Card(suit, rank));
            }
        }
        // 添加两张癞子牌（JOKER）
        gameState.deck.add(new Card("JOKER", "JOKER"));
        gameState.deck.add(new Card("JOKER", "JOKER"));
        Collections.shuffle(gameState.deck);  // 洗牌
    }

    /**
     * 将牌均匀分发给每位玩家
     */
    public static void dealCards(GameState gameState) {
        int totalCards = gameState.deck.size();            // 扑克牌总数
        int numPlayers = gameState.players.size();          // 玩家数量
        for (int i = 0; i < totalCards; i++) {
            // 使用取模分发：第 i 张牌给第 (i % numPlayers) 位玩家
            gameState.players.get(i % numPlayers).getHand().add(gameState.deck.get(i));
        }
    }

    /**
     * 执行一回合的完整流程：
     * - 跳过已胜出玩家
     * - 首位玩家声明点数
     * - 依次出牌或过牌
     * - 支持质疑与延迟胜出判定
     */
    static void playOneRound(GameState gameState, Scanner scanner) {
        // 跳过已胜出玩家，找到第一个可出牌的玩家
        while (gameState.currentPlayerIndex < gameState.players.size()
                && gameState.players.get(gameState.currentPlayerIndex).hasWon()) {
            gameState.currentPlayerIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size();
        }

        // 声明者：当前玩家
        Player starter = gameState.players.get(gameState.currentPlayerIndex);
        System.out.println("\n=== 新一轮开始，由 " + starter.getName() + " 开始声明 ===");

        // 显示手牌并询问声明点数
        showHand(starter);
        String declaredRank;
        while (true) {
            System.out.print(starter.getName() + "，请输入本轮声明点数（例如：8）：");
            declaredRank = scanner.nextLine().trim().toUpperCase();  // 将输入转为大写
            if (isValidRank(declaredRank)) break;                     // 验证合法性
            System.out.println("非法输入，请重新输入！");
        }

        // 设置回合初始状态
        gameState.currentDeclaredRank = declaredRank;  // 当前声明的点数
        gameState.roundOngoing = true;                 // 标记回合正在进行
        gameState.tablePile.clear();                   // 清空桌面牌
        gameState.lastPlayedCards.clear();             // 清空上次出牌记录
        gameState.passedPlayers.clear();               // 清空过牌记录

        int playerCount = gameState.players.size();    // 玩家总数
        int turnsTaken = 0;                            // 已出牌次数，用于判断首发

        // 回合循环：直到回合被质疑中断或触发全过结束
        while (true) {
            Player currentPlayer = gameState.players.get(gameState.currentPlayerIndex);
            if (!currentPlayer.hasWon()) {
                // 执行该玩家的出牌或过牌操作
                processPlayerTurn(gameState, scanner, currentPlayer, turnsTaken == 0);
                if (!gameState.roundOngoing) return;  // 若被质疑打断，回合结束
                turnsTaken++;
            }

            // 判断是否除了最近出牌者外其余玩家均已过牌
            long remainingActive = gameState.players.stream()
                    .filter(p -> !p.hasWon() && p != gameState.lastPlayedPlayer)
                    .filter(p -> !gameState.passedPlayers.contains(p))
                    .count();
            if (remainingActive == 0) {
                // 全部过牌 → 桌面牌入弃牌堆，下次由 lastPlayedPlayer 重新声明
                System.out.println("所有其他玩家都选择过牌，桌面牌进入弃牌堆，"
                        + gameState.lastPlayedPlayer.getName() + " 重新声明新一轮。");
                gameState.discardPile.addAll(gameState.tablePile);
                gameState.tablePile.clear();
                gameState.currentDeclaredRank = null;
                gameState.currentPlayerIndex = gameState.players.indexOf(gameState.lastPlayedPlayer);
                return;
            }

            // 轮到下一个玩家
            do {
                gameState.currentPlayerIndex = (gameState.currentPlayerIndex + 1) % playerCount;
            } while (gameState.players.get(gameState.currentPlayerIndex).hasWon());
        }
    }

    /**
     * 处理单个玩家的出牌或过牌与质疑流程
     * @param isFirst 表示本回合是否该玩家首次出牌（首发必出）
     */
    static void processPlayerTurn(GameState gameState, Scanner scanner, Player player, boolean isFirst) {
        if (player.hasWon()) return;  // 若已胜出则跳过

        System.out.println("\n轮到：" + player.getName());  // 提示玩家轮到
        showHand(player);                                    // 显示其手牌

        // 判断玩家是否要出牌或过牌
        boolean choosePlay = isFirst || askYesOrNo(scanner, "是否要出牌？（y=出牌 / n=过）：");
        if (!choosePlay) {
            System.out.println(player.getName() + " 选择过牌！");
            gameState.passedPlayers.add(player);  // 记录过牌
            return;
        }

        // 玩家选择具体要出的牌（1~4张）
        List<Card> chosen = chooseCards(scanner, player);
        gameState.tablePile.addAll(chosen);   // 将所选牌加入桌面（盖住）
        player.removeCards(chosen);           // 从手牌中移除
        gameState.lastPlayedPlayer = player;  // 记录最近出牌者
        gameState.lastPlayedCards = new ArrayList<>(chosen);

        // 质疑流程：轮询其他玩家
        boolean challenged = false;
        for (Player challenger : gameState.players) {
            if (challenger == player || challenger.hasWon()) continue;
            if (askYesOrNo(scanner, challenger.getName() + "，是否质疑？（y/n）：")) {
                resolveChallenge(gameState, player, challenger);
                challenged = true;
                break;
            }
        }

        // 若无人质疑且玩家手牌出完，则延迟胜出判定
        if (!challenged && player.getHand().isEmpty() && !player.hasWon()) {
            gameState.winners.add(player);
            player.declareWin();
            System.out.println(player.getName() + " 胜出！退出游戏。");
        }
    }

    /**
     * 质疑处理：仅检查最近一次出牌的那几张牌
     */
    static void resolveChallenge(GameState gameState, Player declarer, Player challenger) {
        System.out.println("\n" + challenger.getName() + " 对 " + declarer.getName() + " 发起了质疑！");

        // 验证出牌是否全部符合声明（JOKER 永远视为合法）
        boolean declaredIsTrue = true;
        for (Card c : gameState.lastPlayedCards) {
            if (c.isJoker()) continue;
            if (!c.getRank().equalsIgnoreCase(gameState.currentDeclaredRank)) {
                declaredIsTrue = false;
                break;
            }
        }

        if (!declaredIsTrue) {
            // 质疑成功：撒谎者收回桌面所有牌，下一个出牌者为质疑者
            System.out.println("质疑成功！桌面牌归 " + declarer.getName());
            declarer.addCards(gameState.tablePile);
            gameState.currentPlayerIndex = gameState.players.indexOf(challenger);
        } else {
            // 质疑失败：质疑者收回桌面所有牌
            System.out.println("质疑失败！桌面牌归 " + challenger.getName());
            challenger.addCards(gameState.tablePile);
            // 若此时被质疑者出完，判定胜出
            if (declarer.getHand().isEmpty() && !declarer.hasWon()) {
                gameState.winners.add(declarer);
                declarer.declareWin();
                System.out.println(declarer.getName() + " 胜出！退出游戏。");
            }
        }

        gameState.tablePile.clear();   // 清空桌面牌
        gameState.roundOngoing = false; // 标记回合中断，进入新一轮
    }

    /**
     * 让玩家选择要出的牌，返回 Card 列表
     */
    static List<Card> chooseCards(Scanner scanner, Player player) {
        List<Card> selected = new ArrayList<>();
        while (true) {
            System.out.print("请输入要打出的牌的索引（空格分隔，例如 0 2 3）：");
            String[] parts = scanner.nextLine().trim().split("\\s+");
            try {
                selected.clear();
                for (String p : parts) {
                    int idx = Integer.parseInt(p);     // 转为索引
                    selected.add(player.getHand().get(idx));
                }
                // 验证张数 1~4
                if (selected.size() >= 1 && selected.size() <= 4) break;
            } catch (Exception e) {
                // 捕获索引越界或格式错误
            }
            System.out.println("输入有误，请重新选择。");
        }
        return selected;
    }

    /**
     * 显示玩家手牌到控制台
     */
    static void showHand(Player player) {
        System.out.print(player.getName() + " 的手牌: ");
        for (int i = 0; i < player.getHand().size(); i++) {
            System.out.print("(" + i + ")" + player.getHand().get(i) + " ");
        }
        System.out.println();
    }

    /**
     * 简单的 y/n 提示，返回 true 表示 y
     */
    static boolean askYesOrNo(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String s = scanner.nextLine().trim().toLowerCase();
        return s.equals("y");
    }

    /**
     * 验证声明点数是否合法
     */
    public static boolean isValidRank(String rank) {
        return List.of("2", "3", "4").contains(rank);
    }
}
