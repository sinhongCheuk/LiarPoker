package org.example.dahuapai.gui;

import java.io.*;
import javax.swing.SwingUtilities;

/**
 * UI 桥接：将 System.in/out 重定向至 GUI 组件，并确保所有输出均实时刷新到界面
 */
public class UIBridge {
    private static PipedOutputStream uiInputWriter;

    public static void initialize(GameWindow window) {
        try {
            // 输入管道：UI -> 控制台逻辑
            PipedInputStream pipedIn = new PipedInputStream();
            uiInputWriter = new PipedOutputStream(pipedIn);
            System.setIn(pipedIn);

            // 输出管道：控制台逻辑 -> UI
            PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedOutReader = new PipedInputStream(pipedOut);
            // 自定义 PrintStream：在每次 write 时都 flush，确保 print() 无需换行也能立即显示
            PrintStream ps = new PrintStream(pipedOut, true, "UTF-8") {
                @Override
                public void write(int b) {
                    super.write(b);
                    super.flush();
                }

                @Override
                public void write(byte[] buf, int off, int len) {
                    super.write(buf, off, len);
                    super.flush();
                }
            };
            System.setOut(ps);
            System.setErr(ps);

            // 从控制台输出管道持续读取并追加到 GUI
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(pipedOutReader, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        window.appendText(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "UIBridge-OutputReader").start();

            // 将 GUI 输入通过管道写入控制台逻辑
            window.setInputConsumer(input -> {
                try {
                    uiInputWriter.write((input + "\n").getBytes("UTF-8"));
                    uiInputWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}