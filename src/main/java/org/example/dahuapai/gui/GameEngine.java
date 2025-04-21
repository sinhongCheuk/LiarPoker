package org.example.dahuapai.gui;

import org.example.dahuapai.Main;

/**
 * 游戏引擎：在独立线程中调用原有控制台版 Main.main
 */
public class GameEngine implements Runnable {
    private final String[] args;

    public GameEngine(String[] args) {
        this.args = args;
    }

    @Override
    public void run() {
        Main.main(args);
    }
}