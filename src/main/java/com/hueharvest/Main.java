package com.hueharvest;

import com.hueharvest.client.GamePanel;
import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hue Harvest - Milestone 1");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            
            String[] options = {"Host Game", "Join Game"};
            int choice = JOptionPane.showOptionDialog(frame, "Welcome to Hue Harvest Online!", 
                "Game Mode", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == -1) System.exit(0);

            if (choice == 0) { // Host
                new Thread(() -> {
                    try {
                        com.hueharvest.server.GameServer.main(new String[]{});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                // Short delay to let server start
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            }

            String serverIp = "localhost";
            if (choice == 1) { // Join
                serverIp = JOptionPane.showInputDialog(frame, "Enter Server IP / Room Code:", "localhost");
                if (serverIp == null || serverIp.isEmpty()) System.exit(0);
            }

            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            gamePanel.connect(serverIp);
            
            // To be initialized after chat components
            java.util.function.Consumer<String> chatSync = null;
            JPanel sidePanel = new JPanel();
            sidePanel.setPreferredSize(new Dimension(250, 0));
            sidePanel.setLayout(new BorderLayout());
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));
            sidePanel.setFocusable(false);

            // Match Status (Timer, Goal, Burst)
            JPanel statusPanel = new JPanel(new GridLayout(3, 1));
            statusPanel.setBorder(BorderFactory.createTitledBorder("Match Status"));
            JLabel timerLabel = new JLabel("Time: " + GameState.INITIAL_TIME + "s", SwingConstants.CENTER);
            JLabel goalLabel = new JLabel("Goal: 0 / " + GameState.GOAL_TILES, SwingConstants.CENTER);
            JLabel burstLabel = new JLabel("Burst: Ready", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Arial", Font.BOLD, 22));
            statusPanel.add(timerLabel);
            statusPanel.add(goalLabel);
            statusPanel.add(burstLabel);

            // Leaderboard
            JPanel leaderboardPanel = new JPanel(new GridLayout(4, 1));
            leaderboardPanel.setBorder(BorderFactory.createTitledBorder("Leaderboard"));
            JLabel[] playerScores = new JLabel[4];
            for (int i = 0; i < 4; i++) {
                playerScores[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.LEFT);
                playerScores[i].setOpaque(true);
                playerScores[i].setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                leaderboardPanel.add(playerScores[i]);
            }
            playerScores[0].setForeground(new Color(219, 112, 147)); // Player 1 distinct color

            // Chat Area
            JPanel chatPanel = new JPanel(new BorderLayout());
            chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
            JTextArea chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setFocusable(false); // Prevent focus theft
            JScrollPane chatScroll = new JScrollPane(chatArea);
            
            JTextField chatInput = new JTextField();
            chatInput.addActionListener(e -> {
                String text = chatInput.getText().trim();
                if (!text.isEmpty()) {
                    gamePanel.sendPacket(new com.hueharvest.shared.NetworkPacket(
                        com.hueharvest.shared.NetworkPacket.Type.CHAT, -1, text)); // ID -1, server will fix
                    chatInput.setText("");
                    gamePanel.requestFocusInWindow();
                }
            });
            chatPanel.add(chatScroll, BorderLayout.CENTER);
            chatPanel.add(chatInput, BorderLayout.SOUTH);

            // Connect networking to UI
            gamePanel.setMessageListener(msg -> {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(msg + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
            });

            // Assemble Sidebar
            JPanel topSide = new JPanel(new BorderLayout());
            topSide.add(statusPanel, BorderLayout.NORTH);
            topSide.add(leaderboardPanel, BorderLayout.CENTER);
            
            sidePanel.add(topSide, BorderLayout.NORTH);
            sidePanel.add(chatPanel, BorderLayout.CENTER);

            // Main Frame Assembly
            frame.setLayout(new BorderLayout());
            frame.add(gamePanel, BorderLayout.CENTER);
            frame.add(sidePanel, BorderLayout.EAST);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            final Timer[] timers = new Timer[2]; 

            // Logic timer removed as server handles it

            // UI REFRESH TIMER (Online mode - Very slow refresh for labels to save CPU for typing)
            timers[0] = new Timer(200, e -> {
                GameState remoteState = gamePanel.getRemoteGameState();
                if (remoteState == null) return;

                // Leaderboard tracking
                for (int i = 0; i < 4; i++) {
                    playerScores[i].setText("Player " + (i + 1) + ": " + remoteState.getTileCount(i + 1));
                }

                // Goal tracking: Use current player's score for the main progress
                int myScore = remoteState.getTileCount(gamePanel.getMyPlayerId());
                goalLabel.setText("Goal: " + myScore + " / " + GameState.GOAL_TILES);
                goalLabel.setForeground(myScore >= GameState.GOAL_TILES ? new Color(34, 139, 34) : Color.BLACK);
                
                // Show burst status
                boolean ready = gamePanel.isBurstReady();
                burstLabel.setText("Burst: " + (ready ? "READY" : "CHARGING"));
                burstLabel.setForeground(ready ? new Color(34, 139, 34) : Color.RED);
                
                timerLabel.setText("Time: " + remoteState.getTimeLeft() + "s");
            });
            timers[0].start();

        });
    }
}