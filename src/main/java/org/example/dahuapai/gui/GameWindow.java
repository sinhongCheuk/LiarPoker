package org.example.dahuapai.gui;

import javax.swing.*;
import java.awt.*;

/**
 * 主窗口：展示控制台输出，并接收用户输入
 */
public class GameWindow extends JFrame {
    private final JTextArea consoleArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private InputConsumer inputConsumer;

    public GameWindow() {
        setTitle("大话牌 - GUI版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(consoleArea);

        inputField.addActionListener(e -> submitInput());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(inputField, BorderLayout.SOUTH);
    }

    /**
     * 将文本追加到控制台区域
     */
    public void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text);
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private void submitInput() {
        String input = inputField.getText();
        if (inputConsumer != null && input != null) {
            inputField.setText("");
            inputConsumer.consume(input);
        }
    }

    /**
     * 设置输入消费者，当用户在 UI 输入时回调
     */
    public void setInputConsumer(InputConsumer consumer) {
        this.inputConsumer = consumer;
    }

    /**
     * 功能接口：消费用户输入
     */
    public interface InputConsumer {
        void consume(String input);
    }
}
