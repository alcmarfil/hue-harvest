package com.hueharvest;

import com.hueharvest.client.GamePanel;
import com.hueharvest.shared.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Main {
    // Card Layout State Identifier Keys
    private static final String CARD_TITLE = "TITLE";
    private static final String CARD_MENU = "MENU";
    private static final String CARD_GAME = "GAME";

    // Menu Navigation State
    private static int currentMenuIndex = 0;
    private static final String[] MENU_OPTIONS = {"Enter Lobby", "How to Play", "About"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hue Harvest");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // CardLayout initialization for multi-screen management
            CardLayout cardLayout = new CardLayout();
            JPanel mainContainer = new JPanel(cardLayout);

            // Screen Dimension Calculations
            int gamePanelWidth = 640; 
            int sidebarWidth = 250;
            int totalWidth = gamePanelWidth + sidebarWidth;
            int totalHeight = 800; 

            // ---------------------------------------------------------
            // CARD 1: TITLE SCREEN PANEL (Snappy Canva Reveal)
            // ---------------------------------------------------------
            JPanel titlePanel = new JPanel(null) {
                private java.awt.image.BufferedImage bgImage = null;
                private java.awt.image.BufferedImage textImage = null;
                private double progress = 0.0;          
                private final double TIME_STEP = 0.035; 
                private Timer revealTimer = null;

                {
                    bgImage = com.hueharvest.client.AssetManager.getImage("title_screen_bg.png");
                    textImage = com.hueharvest.client.AssetManager.getImage("title_text.png");

                    revealTimer = new Timer(16, event -> {
                        progress += TIME_STEP;
                        if (progress >= 1.0) {
                            progress = 1.0;     
                            revealTimer.stop(); 
                        }
                        repaint();
                    });
                    revealTimer.start();
                    setDoubleBuffered(true);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    if (bgImage != null) {
                        g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }

                    if (textImage != null) {
                        double easedProgress = Math.sin(progress * (Math.PI / 2.0));
                        float alpha = (float) Math.min(1.0, progress * 2.0);
                        double currentScale = 0.92 + (0.08 * easedProgress);

                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                        int currentWidth = (int) (textImage.getWidth() * currentScale);
                        int currentHeight = (int) (textImage.getHeight() * currentScale);
                        int x = (getWidth() - currentWidth) / 2;
                        int y = (getHeight() - currentHeight) / 2;

                        g2d.drawImage(textImage, x, y, currentWidth, currentHeight, null);
                    }
                    g2d.dispose();
                }
            };
            titlePanel.setBackground(Color.BLACK);
            titlePanel.setPreferredSize(new Dimension(totalWidth, totalHeight));
            titlePanel.setFocusable(true);

            // ---------------------------------------------------------
            // CARD 2: MAIN MENU SCREEN
            // ---------------------------------------------------------
            JPanel menuPanel = new JPanel(null) {
                private java.awt.image.BufferedImage bgImage = null;
                private java.awt.image.BufferedImage lobbyBtn = null;
                private java.awt.image.BufferedImage h2pBtn = null;
                private java.awt.image.BufferedImage aboutBtn = null;
                private java.awt.image.BufferedImage selectorBtn = null; 

                {
                    bgImage = com.hueharvest.client.AssetManager.getImage("overall_bg.png");
                    lobbyBtn = com.hueharvest.client.AssetManager.getImage("lobby_btn.png");
                    h2pBtn = com.hueharvest.client.AssetManager.getImage("h2p_btn.png");
                    aboutBtn = com.hueharvest.client.AssetManager.getImage("about_btn.png");
                    selectorBtn = com.hueharvest.client.AssetManager.getImage("selector.png");
                    
                    setDoubleBuffered(true);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int panelWidth = getWidth();
                    int panelHeight = getHeight();

                    // 1. Render Background Image
                    if (bgImage != null) {
                        g2d.drawImage(bgImage, 0, 0, panelWidth, panelHeight, null);
                    } else {
                        g2d.setColor(new Color(20, 20, 20));
                        g2d.fillRect(0, 0, panelWidth, panelHeight);
                    }

                    java.awt.image.BufferedImage[] buttons = {lobbyBtn, h2pBtn, aboutBtn};
                    
                    int gap = 20; // Spacing between buttons
                    int totalMenuHeight = 0;

                    for (java.awt.image.BufferedImage btn : buttons) {
                        if (btn != null) {
                            totalMenuHeight += btn.getHeight();
                        }
                    }
                    totalMenuHeight += gap * (buttons.length - 1);

                    // vertical center alignment
                    int currentY = (panelHeight - totalMenuHeight) / 2;

                    // render Buttons & Custom Image Cursor
                    for (int i = 0; i < buttons.length; i++) {
                        java.awt.image.BufferedImage currentBtn = buttons[i];
                        if (currentBtn == null) continue;

                        int btnWidth = currentBtn.getWidth();
                        int btnHeight = currentBtn.getHeight();
                        int btnX = (panelWidth - btnWidth) / 2;

                        // draw main menu button
                        g2d.drawImage(currentBtn, btnX, currentY, null);

                        // if active index, render custom image selector beside it
                        if (i == currentMenuIndex && selectorBtn != null) {
                            int selWidth = selectorBtn.getWidth();
                            int selHeight = selectorBtn.getHeight();

                            // POSITIONING COMPUTATION
                            int selectorX = btnX - selWidth - 15;
                            int selectorY = currentY + (btnHeight - selHeight) / 2;
                            g2d.drawImage(selectorBtn, selectorX, selectorY, null);
                        }
                        currentY += btnHeight + gap;
                    }
                    g2d.dispose();
                }
            };
            menuPanel.setBackground(Color.BLACK);
            menuPanel.setPreferredSize(new Dimension(totalWidth, totalHeight));
            menuPanel.setFocusable(true);

            // ---------------------------------------------------------
            // CARD 3: ACTUAL GAME SCREEN CONTAINER & SIDEBAR
            // ---------------------------------------------------------
            JPanel gameContainer = new JPanel(new BorderLayout());
            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);
            
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

            // Sidebar UI Elements Initialization
            JLabel timerLabel = new JLabel(GameState.INITIAL_TIME + "s", SwingConstants.CENTER);
            timerLabel.setFont(new Font("Poppins", Font.BOLD, 18));
            timerLabel.setForeground(new Color(101, 67, 33)); 
            timerLabel.setBounds(18, 68, 90, 25); 
            sidePanel.add(timerLabel);

            java.awt.image.BufferedImage checkImg = com.hueharvest.client.AssetManager.getImage("check icon.png");
            final ImageIcon checkIcon = (checkImg != null) ? new ImageIcon(checkImg) : null;

            JLabel burstStatusIcon = new JLabel();
            burstStatusIcon.setBounds(158, 35, 70, 50); 
            sidePanel.add(burstStatusIcon);

            JLabel[] playerScores = new JLabel[4];
            for (int i = 0; i < 4; i++) {
                playerScores[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.LEFT);
                playerScores[i].setFont(new Font("Poppins", Font.BOLD, 15));
                playerScores[i].setBounds(28, 163 + (i * 35), 194, 24); 
                sidePanel.add(playerScores[i]);
            }
            playerScores[0].setForeground(new Color(219, 112, 147)); 
            playerScores[1].setForeground(new Color(74, 55, 40));    

            JTextArea chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setOpaque(false);
            chatArea.setFont(new Font("Poppins", Font.PLAIN, 14));
            chatArea.setBounds(14, 360, 226, 370); 
            sidePanel.add(chatArea);

            final java.util.LinkedList<String> chatMessages = new java.util.LinkedList<>();
            final int MAX_CHAT_LINES = 18;

            JTextField chatInput = new JTextField();
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

            gameContainer.add(gamePanel, BorderLayout.CENTER);
            gameContainer.add(sidePanel, BorderLayout.EAST);

            // Assemble into Card Stack
            mainContainer.add(titlePanel, CARD_TITLE);
            mainContainer.add(menuPanel, CARD_MENU);
            mainContainer.add(gameContainer, CARD_GAME);

            frame.setLayout(new BorderLayout());
            frame.add(mainContainer, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            titlePanel.requestFocusInWindow();

            // ---------------------------------------------------------
            // FLOW TRANSITION LOGIC
            // ---------------------------------------------------------
            
            // TRANSITION 1: From Title Screen to Menu Screen once any key is entered
            titlePanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    titlePanel.removeKeyListener(this);
                    cardLayout.show(mainContainer, CARD_MENU);
                    menuPanel.requestFocusInWindow(); 
                }
            });

            // TRANSITION 2: Keyboard Navigated Navigation System for Menu Options
            menuPanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();

                    // Navigation via Arrow keys or WASD controls
                    if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                        currentMenuIndex--;
                        if (currentMenuIndex < 0) currentMenuIndex = MENU_OPTIONS.length - 1;
                        menuPanel.repaint();
                    } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                        currentMenuIndex++;
                        if (currentMenuIndex >= MENU_OPTIONS.length) currentMenuIndex = 0;
                        menuPanel.repaint();
                    } 
                    // Activation Event via Enter or Space key
                    else if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                        executeMenuOption(currentMenuIndex, frame, cardLayout, mainContainer, gamePanel);
                    }
                }
            });

            // GAME CONTROL STATE UPDATER TIMER
            new Timer(200, e -> {
                GameState remoteState = gamePanel.getRemoteGameState();
                if (remoteState == null || remoteState.getStatus() == GameState.Status.LOBBY) return;

                for (int i = 0; i < 4; i++) {
                    playerScores[i].setText("Player " + (i + 1) + ": " + remoteState.getTileCount(i + 1));
                }
                
                if (gamePanel.isBurstReady()) {
                    if (checkIcon != null) {
                        burstStatusIcon.setIcon(checkIcon);
                        burstStatusIcon.setText("");
                    } else {
                        burstStatusIcon.setIcon(null);
                        burstStatusIcon.setText("READY");
                    }
                } else {
                    burstStatusIcon.setIcon(null);
                    burstStatusIcon.setText("Charging");
                }
                timerLabel.setText(remoteState.getTimeLeft() + "s");
            }).start();
        });
    }

    // ---------------------------------------------------------
    // MENU FUNCTION ACTIVATION ROUTINE
    // ---------------------------------------------------------
    private static void executeMenuOption(int index, JFrame frame, CardLayout cl, JPanel container, GamePanel gamePanel) {
        switch (index) {
            case 0: // 1. Enter Lobby Option Selected
                String[] options = {"Host Game", "Join Game"};
                int choice = JOptionPane.showOptionDialog(frame, "Welcome to Hue Harvest Online!", 
                    "Lobby Access", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == -1) return; // go back to menu if input window is closed

                String serverIp = "localhost";

                if (choice == 0) { // Host Routine
                    new Thread(() -> {
                        try {
                            com.hueharvest.server.GameServer.main(new String[]{});
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                    try { Thread.sleep(500); } catch (InterruptedException ex) {}
                } else { // Join Routine
                    String input = JOptionPane.showInputDialog(frame, "Enter Room Code or Server IP:", "");
                    if (input == null || input.trim().isEmpty()) return;
                    serverIp = com.hueharvest.shared.NetworkUtils.roomCodeToIp(input);
                }

                // Connect and transition into the game screen
                gamePanel.connect(serverIp);
                cl.show(container, CARD_GAME);
                gamePanel.requestFocusInWindow();
                break;

            case 1: // 2. How to Play Option Selected
                JOptionPane.showMessageDialog(frame, 
                    "1. Use Arrow Keys / WASD to move around the field.\n" +
                    "2. Stepping on tiles harvests and claims them to your color.\n" +
                    "3. Press 'Enter' when burst is ready to claim a 3x3 grid instantly!\n" +
                    "4. The player with the highest tile count when timer hits 0 wins!", 
                    "How to Play - Game Instructions", JOptionPane.INFORMATION_MESSAGE);
                break;

            case 2: // 3. About Option Selected
                JOptionPane.showMessageDialog(frame, 
                    "Hue Harvest Online\n" +
                    "A farm-themed color conquest game\n\n" +
                    "Developed as an academic development project for CMSC 137.\n" +
                    "All rights reserved 2026.", 
                    "About Hue Harvest", JOptionPane.INFORMATION_MESSAGE);
                break;
        }
    }
}