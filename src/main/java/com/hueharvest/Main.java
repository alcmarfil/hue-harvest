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
            
            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            
            // sidebar components
            JPanel sidePanel = new JPanel();
            sidePanel.setPreferredSize(new Dimension(250, 0));
            sidePanel.setLayout(new BorderLayout());
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, Color.LIGHT_GRAY));

            // timer and status
            JPanel statusPanel = new JPanel(new GridLayout(2, 1));
            statusPanel.setBorder(BorderFactory.createTitledBorder("Match Status"));
            JLabel timerLabel = new JLabel("Time: 90s", SwingConstants.CENTER);
            JLabel burstLabel = new JLabel("Burst: Ready", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Arial", Font.BOLD, 22));
            statusPanel.add(timerLabel);
            statusPanel.add(burstLabel);

            // leaderboard
            JPanel leaderboardPanel = new JPanel(new GridLayout(4, 1));
            leaderboardPanel.setBorder(BorderFactory.createTitledBorder("Leaderboard"));
            JLabel[] playerScores = new JLabel[4];
            for (int i = 0; i < 4; i++) {
                playerScores[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.LEFT);
                playerScores[i].setOpaque(true);
                playerScores[i].setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                leaderboardPanel.add(playerScores[i]);
            }
            
            // give player (Player 1) a distinct color
            playerScores[0].setForeground(new Color(219, 112, 147)); 

            // chat Area
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
                    gamePanel.requestFocusInWindow(); // give focus back to game
                }
            });
            chatPanel.add(chatScroll, BorderLayout.CENTER);
            chatPanel.add(chatInput, BorderLayout.SOUTH);

            // assemble sidebar
            JPanel topSide = new JPanel(new BorderLayout());
            topSide.add(statusPanel, BorderLayout.NORTH);
            topSide.add(leaderboardPanel, BorderLayout.CENTER);
            
            sidePanel.add(topSide, BorderLayout.NORTH);
            sidePanel.add(chatPanel, BorderLayout.CENTER);

            frame.setLayout(new BorderLayout());
            frame.add(gamePanel, BorderLayout.CENTER);
            frame.add(sidePanel, BorderLayout.EAST);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // ui refresh timer
            Timer uiTimer = new Timer(50, e -> {
                // update specific player scores (Milestone 2 will update all)
                for (int i = 0; i < 4; i++) {
                    int count = gameState.getTileCount(i + 1);
                    playerScores[i].setText("Player " + (i + 1) + ": " + count);
                }
                
                long cd = gamePanel.getCooldownRemaining();
                if (cd > 0) {
                    burstLabel.setText("Burst: " + cd + "s");
                    burstLabel.setForeground(Color.RED);
                } else {
                    burstLabel.setText("Burst: READY");
                    burstLabel.setForeground(new Color(34, 139, 34));
                }
            });
            uiTimer.start();

            // game timer logic
            Timer gameTimer = new Timer(1000, e -> {
                gameState.tick();
                timerLabel.setText("Time: " + gameState.getTimeLeft() + "s");
                
                if (gameState.getTimeLeft() <= 0) {
                    uiTimer.stop();
                    ((Timer)e.getSource()).stop();
                    
                    // final scoring & placement
                    int myScore = gameState.getTileCount(1);
                    int rank = 1;
                    for (int i = 2; i <= 4; i++) {
                        if (gameState.getTileCount(i) > myScore) {
                            rank++;
                        }
                    }

                    String rankSuffix = switch (rank) {
                        case 1 -> "st";
                        case 2 -> "nd";
                        case 3 -> "rd";
                        default -> "th";
                    };

                    String message = String.format(
                        "MATCH EXPIRED!\n\n" +
                        "Your Score: %d tiles\n" +
                        "Placement: %d%s Place\n\n" +
                        "Would you like to play again?", 
                        myScore, rank, rankSuffix
                    );

                    int choice = JOptionPane.showOptionDialog(
                        frame, message, "HuHarvest Result", 
                        JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, 
                        null, new String[]{"Play Again", "Exit"}, "Play Again"
                    );

                    if (choice == JOptionPane.YES_OPTION) {
                        gameState.reset();
                        gamePanel.reset();
                        uiTimer.restart();
                        ((Timer)e.getSource()).restart();
                    } else {
                        System.exit(0);
                    }
                }
            });
            gameTimer.start();
        });
    }
}
