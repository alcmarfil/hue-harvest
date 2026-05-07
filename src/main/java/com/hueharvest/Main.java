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
            
            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            
            // --- SIDEBAR COMPONENTS ---
            JPanel sidePanel = new JPanel();
            sidePanel.setPreferredSize(new Dimension(250, 0));
            sidePanel.setLayout(new BorderLayout());
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));

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
            JScrollPane chatScroll = new JScrollPane(chatArea);
            
            JTextField chatInput = new JTextField();
            chatInput.addActionListener(e -> {
                if (!chatInput.getText().trim().isEmpty()) {
                    chatArea.append("You: " + chatInput.getText() + "\n");
                    chatInput.setText("");
                    gamePanel.requestFocusInWindow();
                }
            });
            chatPanel.add(chatScroll, BorderLayout.CENTER);
            chatPanel.add(chatInput, BorderLayout.SOUTH);

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

            // --- GAME LOGIC TIMER ---
            timers[1] = new Timer(1000, e -> {
                gameState.tick();
                timerLabel.setText("Time: " + gameState.getTimeLeft() + "s");

                int player1Count = gameState.getTileCount(1);
                boolean goalReached = player1Count >= GameState.GOAL_TILES;
                boolean timeExpired = gameState.getTimeLeft() <= 0;

                if (goalReached || timeExpired) {
                    timers[0].stop();
                    timers[1].stop();

                    if (goalReached) {
                        // SCENARIO A: VICTORY
                        int rank = 1; 
                        for (int i = 2; i <= 4; i++) {
                            if (gameState.getTileCount(i) > player1Count) rank++;
                        }
                        gamePanel.setGameOver(true, "Rank: " + rank, player1Count);
                    } else {
                        // SCENARIO B: TIME EXPIRED (GOAL NOT MET)
                        gamePanel.setGameOver(false, "You did not meet the goal of 200 crops.", player1Count);
                    }
                }
            });

            // --- UI REFRESH TIMER ---
            timers[0] = new Timer(50, e -> {
                for (int i = 0; i < 4; i++) {
                    playerScores[i].setText("Player " + (i + 1) + ": " + gameState.getTileCount(i + 1));
                }
                int p1 = gameState.getTileCount(1);
                goalLabel.setText("Goal: " + p1 + " / " + GameState.GOAL_TILES);
                goalLabel.setForeground(p1 >= GameState.GOAL_TILES ? new Color(34, 139, 34) : Color.BLACK);
                
                long cd = gamePanel.getCooldownRemaining();
                burstLabel.setText("Burst: " + (cd > 0 ? cd + "s" : "READY"));
                burstLabel.setForeground(cd > 0 ? Color.RED : new Color(34, 139, 34));
            });

            // pass restart logic to game panel
            gamePanel.setRestartAction(() -> {
                gameState.reset(); // 
                timers[0].start();
                timers[1].start();
                timerLabel.setText("Time: " + GameState.INITIAL_TIME + "s");
            });

            // To stop the game instantly
            gamePanel.setWinListener(() -> {
                int p1Count = gameState.getTileCount(1);
                
                // force the leaderboard and goal label to show the actual final count
                playerScores[0].setText("Player 1: " + p1Count);
                goalLabel.setText("Goal: " + p1Count + " / " + GameState.GOAL_TILES);
                goalLabel.setForeground(new Color(34, 139, 34));

                // stop the timers
                timers[0].stop(); // UI Timer
                timers[1].stop(); // Logic Timer
                
                // calculate rank and show modal
                int rank = 1; 
                for (int i = 2; i <= 4; i++) {
                    if (gameState.getTileCount(i) > p1Count) rank++;
                }
                gamePanel.setGameOver(true, "Rank: " + rank, p1Count);
            });

            timers[0].start();
            timers[1].start();
        });
    }
}