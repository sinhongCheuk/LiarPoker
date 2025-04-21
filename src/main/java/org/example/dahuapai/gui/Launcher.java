package org.example.dahuapai.gui;

import org.example.dahuapai.Main;
import javax.swing.SwingUtilities;

/**
 * 启动类：初始化 GUI 并启动游戏引擎
 */
public class Launcher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            UIBridge.initialize(window);
            window.setVisible(true);
            // 在新线程中运行原有控制台逻辑
            new Thread(new GameEngine(args), "GameEngine").start();
        });
    }
}