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
                String input = JOptionPane.showInputDialog(frame, "Enter Room Code or Server IP:", "");
                if (input == null || input.trim().isEmpty()) System.exit(0);
                serverIp = com.hueharvest.shared.NetworkUtils.roomCodeToIp(input);
            }

            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            gamePanel.connect(serverIp);
            
            // To be initialized after chat components
            java.util.function.Consumer<String> chatSync = null;
            JPanel sidePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    java.awt.image.BufferedImage bg = com.hueharvest.client.AssetManager.getImage("sidebar_bg.png");
                    if (bg != null) {
                        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        // Soft fall-back premium gradient
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setPaint(new java.awt.GradientPaint(0, 0, new Color(248, 246, 240), 0, getHeight(), new Color(230, 235, 230)));
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
            };
            sidePanel.setPreferredSize(new Dimension(250, 0));
            sidePanel.setLayout(null);
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(180, 190, 180)));
            sidePanel.setFocusable(false);

            // 1. Timer Label (Centered inside the wooden hanger sign on the left)
            JLabel timerLabel = new JLabel(GameState.INITIAL_TIME + "s", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Poppins", Font.BOLD, 18));
            timerLabel.setForeground(new Color(101, 67, 33)); // Wood brown color
            timerLabel.setBounds(18, 68, 90, 25); // X, Y, Width, Height (moved down a little)
            sidePanel.add(timerLabel);

            // Load check icon
            java.awt.image.BufferedImage checkImg = com.hueharvest.client.AssetManager.getImage("check icon.png");
            final ImageIcon checkIcon = (checkImg != null) ? new ImageIcon(checkImg) : null;

            // 2. Burst Status Icon/Text (Centered inside the "BURST" box on the right)
            JLabel burstStatusIcon = new JLabel();
            burstStatusIcon.setHorizontalAlignment(SwingConstants.CENTER);
            burstStatusIcon.setVerticalAlignment(SwingConstants.CENTER);
            burstStatusIcon.setFont(new Font("Poppins", Font.BOLD, 12));
            burstStatusIcon.setBounds(158, 35, 70, 50); // X, Y, Width, Height 
            sidePanel.add(burstStatusIcon);

            // 3. Leaderboard Player Scores
            JLabel[] playerScores = new JLabel[4];
            String[] playerColors = {"#DB7093", "#4A3728", "#4A3728", "#4A3728"}; // P1 pink, others dark brown
            for (int i = 0; i < 4; i++) {
                playerScores[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.LEFT);
                playerScores[i].setOpaque(false);
                playerScores[i].setFont(new Font("Poppins", Font.BOLD, 15));
                playerScores[i].setBounds(28, 163 + (i * 35), 194, 24); // 32px line spacing
                sidePanel.add(playerScores[i]);
            }
            playerScores[0].setForeground(new Color(219, 112, 147)); // Player 1 pink
            playerScores[1].setForeground(new Color(74, 55, 40));    // Players 2-4 dark brown
            playerScores[2].setForeground(new Color(74, 55, 40));
            playerScores[3].setForeground(new Color(74, 55, 40));

            // (Goal label removed from sidebar — shown in-game only)

            // 5. Chat Text Area — no JScrollPane to avoid viewport scroll causing game panel repaint glitches
            JTextArea chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setOpaque(false);
            chatArea.setFont(new Font("Poppins", Font.PLAIN, 14));
            chatArea.setForeground(new Color(50, 35, 15)); // Dark readable brown
            chatArea.setFocusable(false);
            chatArea.setBounds(14, 360, 226, 370); // X, Y, Width, Height — added directly, no scroll pane
            sidePanel.add(chatArea);

            // Rolling message buffer — keeps only the latest 18 messages to avoid any scroll/repaint
            final java.util.LinkedList<String> chatMessages = new java.util.LinkedList<>();
            final int MAX_CHAT_LINES = 18;

            // 6. Chat Input Text Field (At the very bottom of the house graphic)
            JTextField chatInput = new JTextField();
            chatInput.setFont(new Font("Poppins", Font.PLAIN, 14));
            chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 195, 180), 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));
            chatInput.setBounds(15, 740, 220, 40); // X, Y, Width, Height
            chatInput.addActionListener(e -> {
                String text = chatInput.getText().trim();
                if (!text.isEmpty()) {
                    gamePanel.sendPacket(new com.hueharvest.shared.NetworkPacket(
                        com.hueharvest.shared.NetworkPacket.Type.CHAT, -1, text)); // ID -1, server will fix
                    chatInput.setText("");
                    gamePanel.requestFocusInWindow();
                }
            });
            sidePanel.add(chatInput);

            // Connect networking to UI — rolling buffer, no scroll needed
            gamePanel.setMessageListener(msg -> {
                SwingUtilities.invokeLater(() -> {
                    chatMessages.addLast(msg);
                    if (chatMessages.size() > MAX_CHAT_LINES) chatMessages.removeFirst();
                    chatArea.setText(String.join("\n", chatMessages));
                });
            });

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

                // Goal tracking (sidebar label removed, tracked in-game only)
                
                // Show burst status (checkmark image when ready)
                boolean ready = gamePanel.isBurstReady();
                if (ready) {
                    if (checkIcon != null) {
                        burstStatusIcon.setIcon(checkIcon);
                        burstStatusIcon.setText("");
                    } else {
                        burstStatusIcon.setIcon(null);
                        burstStatusIcon.setText("READY");
                        burstStatusIcon.setFont(new Font("Poppins", Font.BOLD, 12));
                        burstStatusIcon.setForeground(new Color(34, 139, 34));
                    }
                } else {
                    burstStatusIcon.setIcon(null);
                    burstStatusIcon.setText("Charging");
                    burstStatusIcon.setFont(new Font("Poppins", Font.BOLD, 11));
                    burstStatusIcon.setForeground(Color.RED);
                }
                
                timerLabel.setText(remoteState.getTimeLeft() + "s");
            });
            timers[0].start();

        });
    }
}