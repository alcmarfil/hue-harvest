package com.hueharvest;

import com.hueharvest.client.GamePanel;
import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Main {
    private static final String CARD_TITLE = "TITLE";
    private static final String CARD_GAME = "GAME";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hue Harvest - Milestone 1");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // CardLayout initialization for overlapping management
            CardLayout cardLayout = new CardLayout();
            JPanel mainContainer = new JPanel(cardLayout);

            // ---------------------------------------------------------
            // CARD 1: TITLE SCREEN PANEL (GIF Covers everything including sidebar space)
            // ---------------------------------------------------------
            JPanel titlePanel = new JPanel() {
                private Image titleGif = null;
                {
                    java.net.URL gifUrl = getClass().getResource("/assets/title_screen.gif");
                    if (gifUrl != null) {
                        titleGif = Toolkit.getDefaultToolkit().createImage(gifUrl);
                    }
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (titleGif != null) {
                        g.drawImage(titleGif, 0, 0, getWidth(), getHeight(), this);
                    } else {
                        g.setColor(Color.BLACK);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Poppins", Font.BOLD, 24));
                        g.drawString("HUE HARVEST - PRESS ANY KEY TO START", getWidth() / 2 - 240, getHeight() / 2);
                    }
                }
            };
            
            // Total width must match: GamePanel width + Sidebar width 
            int gamePanelWidth = 640; 
            int sidebarWidth = 250;
            int totalHeight = 800; 
            
            titlePanel.setPreferredSize(new Dimension(gamePanelWidth + sidebarWidth, totalHeight));
            titlePanel.setFocusable(true);

            // ---------------------------------------------------------
            // CARD 2: ACTUAL GAME SCREEN
            // ---------------------------------------------------------
            JPanel gameContainer = new JPanel(new BorderLayout());

            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            
            // Sidebar implementation 
            JPanel sidePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    java.awt.image.BufferedImage bg = com.hueharvest.client.AssetManager.getImage("sidebar_bg.png");
                    if (bg != null) {
                        g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setPaint(new java.awt.GradientPaint(0, 0, new Color(248, 246, 240), 0, getHeight(), new Color(230, 235, 230)));
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
            };
            sidePanel.setPreferredSize(new Dimension(sidebarWidth, 0));
            sidePanel.setLayout(null);
            sidePanel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(180, 190, 180)));
            sidePanel.setFocusable(false);

            // 1. Timer Label
            JLabel timerLabel = new JLabel(GameState.INITIAL_TIME + "s", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Poppins", Font.BOLD, 18));
            timerLabel.setForeground(new Color(101, 67, 33)); 
            timerLabel.setBounds(18, 68, 90, 25); 
            sidePanel.add(timerLabel);

            // Load check icon
            java.awt.image.BufferedImage checkImg = com.hueharvest.client.AssetManager.getImage("check icon.png");
            final ImageIcon checkIcon = (checkImg != null) ? new ImageIcon(checkImg) : null;

            // 2. Burst Status Icon/Text
            JLabel burstStatusIcon = new JLabel();
            burstStatusIcon.setHorizontalAlignment(SwingConstants.CENTER);
            burstStatusIcon.setVerticalAlignment(SwingConstants.CENTER);
            burstStatusIcon.setFont(new Font("Poppins", Font.BOLD, 12));
            burstStatusIcon.setBounds(158, 35, 70, 50); 
            sidePanel.add(burstStatusIcon);

            // 3. Leaderboard Player Scores
            JLabel[] playerScores = new JLabel[4];
            for (int i = 0; i < 4; i++) {
                playerScores[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.LEFT);
                playerScores[i].setOpaque(false);
                playerScores[i].setFont(new Font("Poppins", Font.BOLD, 15));
                playerScores[i].setBounds(28, 163 + (i * 35), 194, 24); 
                sidePanel.add(playerScores[i]);
            }
            playerScores[0].setForeground(new Color(219, 112, 147)); 
            playerScores[1].setForeground(new Color(74, 55, 40));    
            playerScores[2].setForeground(new Color(74, 55, 40));
            playerScores[3].setForeground(new Color(74, 55, 40));

            // 5. Chat Text Area
            JTextArea chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setOpaque(false);
            chatArea.setFont(new Font("Poppins", Font.PLAIN, 14));
            chatArea.setForeground(new Color(50, 35, 15)); 
            chatArea.setFocusable(false);
            chatArea.setBounds(14, 360, 226, 370); 
            sidePanel.add(chatArea);

            final java.util.LinkedList<String> chatMessages = new java.util.LinkedList<>();
            final int MAX_CHAT_LINES = 18;

            // 6. Chat Input Text Field
            JTextField chatInput = new JTextField();
            chatInput.setFont(new Font("Poppins", Font.PLAIN, 14));
            chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 195, 180), 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));
            chatInput.setBounds(15, 740, 220, 40); 
            chatInput.addActionListener(e -> {
                String text = chatInput.getText().trim();
                if (!text.isEmpty()) {
                    gamePanel.sendPacket(new com.hueharvest.shared.NetworkPacket(
                        com.hueharvest.shared.NetworkPacket.Type.CHAT, -1, text)); 
                    chatInput.setText("");
                    gamePanel.requestFocusInWindow();
                }
            });
            sidePanel.add(chatInput);

            gamePanel.setMessageListener(msg -> {
                SwingUtilities.invokeLater(() -> {
                    chatMessages.addLast(msg);
                    if (chatMessages.size() > MAX_CHAT_LINES) chatMessages.removeFirst();
                    chatArea.setText(String.join("\n", chatMessages));
                });
            });

            // include panel and sidebar in Game Container card
            gameContainer.add(gamePanel, BorderLayout.CENTER);
            gameContainer.add(sidePanel, BorderLayout.EAST);

            // inject two screens in our CardLayout manager
            mainContainer.add(titlePanel, CARD_TITLE);
            mainContainer.add(gameContainer, CARD_GAME);

            // assembly in Main Frame
            frame.setLayout(new BorderLayout());
            frame.add(mainContainer, BorderLayout.CENTER);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // let Title Panel catch keyboard focus 
            titlePanel.requestFocusInWindow();

            // ---------------------------------------------------------
            // KEY LISTENERS & TRANSITION LOGIC
            // ---------------------------------------------------------
            titlePanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    titlePanel.removeKeyListener(this);

                    //show Mode Choice Dialog
                    String[] options = {"Host Game", "Join Game"};
                    int choice = JOptionPane.showOptionDialog(frame, "Welcome to Hue Harvest Online!", 
                        "Game Mode", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                    if (choice == -1) System.exit(0);

                    String serverIp = "localhost";

                    if (choice == 0) { // Host Setup
                        new Thread(() -> {
                            try {
                                com.hueharvest.server.GameServer.main(new String[]{});
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                        try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    } else if (choice == 1) { // Join Setup
                        String input = JOptionPane.showInputDialog(frame, "Enter Room Code or Server IP:", "");
                        if (input == null || input.trim().isEmpty()) System.exit(0);
                        serverIp = com.hueharvest.shared.NetworkUtils.roomCodeToIp(input);
                    }

                    // connect to network after picking any user type
                    gamePanel.connect(serverIp);

                    // switch view from Title Panel to Game UI Setup!
                    cardLayout.show(mainContainer, CARD_GAME);
                    gamePanel.requestFocusInWindow();
                }
            });

            // UI REFRESH TIMER
            final Timer[] timers = new Timer[1];
            timers[0] = new Timer(200, e -> {
                GameState remoteState = gamePanel.getRemoteGameState();
                if (remoteState == null) return;

                for (int i = 0; i < 4; i++) {
                    playerScores[i].setText("Player " + (i + 1) + ": " + remoteState.getTileCount(i + 1));
                }
                
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