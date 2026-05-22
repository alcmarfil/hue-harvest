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
    private static final String CARD_ABOUT = "ABOUT";

    // Menu Navigation State
    private static int currentMenuIndex = 0;
    private static final String[] MENU_OPTIONS = {"Enter Lobby", "How to Play", "About"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            GameState gameState = new GameState();
            GamePanel gamePanel = new GamePanel(gameState);

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
            // CARD 3: LOBBY SELECTION SCREEN
            // ---------------------------------------------------------
            final int[] lobbyMenuIndex = {0}; // 0 = Host, 1 = Join
            
            JPanel lobbyMenuPanel = new JPanel(null) {
                // declare needed image variables as fields of the panel
                private java.awt.image.BufferedImage bgImage = null;
                private java.awt.image.BufferedImage modalLobbyBg = null;
                private java.awt.image.BufferedImage hostBtn = null;
                private java.awt.image.BufferedImage joinBtn = null;
                private java.awt.image.BufferedImage selectorBtn = null;

                {
                    // load asset using AssetManager 
                    bgImage = com.hueharvest.client.AssetManager.getImage("overall_bg.png");
                    modalLobbyBg = com.hueharvest.client.AssetManager.getImage("modal_lobby.png");
                    hostBtn = com.hueharvest.client.AssetManager.getImage("host_btn.png"); 
                    joinBtn = com.hueharvest.client.AssetManager.getImage("join_btn.png"); 
                    selectorBtn = com.hueharvest.client.AssetManager.getImage("selector2.png");
                    
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

                    // render main overall background
                    if (bgImage != null) {
                        g2d.drawImage(bgImage, 0, 0, panelWidth, panelHeight, null);
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, panelWidth, panelHeight);
                    }

                    // render "modal_lobby.png" centered
                    int modalX = 0, modalY = 0;
                    int modalW = 0, modalH = 0;
                    if (modalLobbyBg != null) {
                        modalW = modalLobbyBg.getWidth();
                        modalH = modalLobbyBg.getHeight();
                        modalX = (panelWidth - modalW) / 2;
                        modalY = (panelHeight - modalH) / 2;
                        g2d.drawImage(modalLobbyBg, modalX, modalY, null);
                    }

                    // row layout for the  Host at Join buttons 
                    java.awt.image.BufferedImage[] lobbyButtons = {hostBtn, joinBtn};
                    int buttonGap = 40; 
                    int totalRowWidth = 0;

                    for (java.awt.image.BufferedImage btn : lobbyButtons) {
                        if (btn != null) totalRowWidth += btn.getWidth();
                    }
                    totalRowWidth += buttonGap * (lobbyButtons.length - 1);

                    int startRowX = modalX + (modalW - totalRowWidth) / 2;
                    // fallback if no modal image
                    int rowY = (modalLobbyBg != null) ? (modalY + modalH - 100) : (panelHeight - 150); 

                    // paint buttons at custom selection cursor
                    int currentBtnX = startRowX;
                    for (int i = 0; i < lobbyButtons.length; i++) {
                        java.awt.image.BufferedImage currentBtn = lobbyButtons[i];
                        if (currentBtn == null) continue;

                        int btnW = currentBtn.getWidth();
                        int btnH = currentBtn.getHeight();

                        g2d.drawImage(currentBtn, currentBtnX, rowY, null);

                        // Selector Cursor logic 
                        if (i == lobbyMenuIndex[0] && selectorBtn != null) {
                            // center selector in the middle of button image (overlay)
                            int selW = selectorBtn.getWidth();
                            int selH = selectorBtn.getHeight();
                            
                            int selectorX = currentBtnX + (btnW - selW) / 2;
                            int selectorY = rowY + (btnH - selH) / 2;
                            
                            g2d.drawImage(selectorBtn, selectorX, selectorY, null);
                        }

                        currentBtnX += btnW + buttonGap;
                    }
                    g2d.dispose();
                }
            };

            lobbyMenuPanel.setFocusable(true);
            lobbyMenuPanel.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    
                    // navigate between Host (0) at Join (1) using Left/Right or A/D
                    if (keyCode == java.awt.event.KeyEvent.VK_LEFT || keyCode == java.awt.event.KeyEvent.VK_A) {
                        lobbyMenuIndex[0] = 0;
                        lobbyMenuPanel.repaint();
                    } else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT || keyCode == java.awt.event.KeyEvent.VK_D) {
                        lobbyMenuIndex[0] = 1;
                        lobbyMenuPanel.repaint();
                    } 
                    // go back to Main Menu when enter ESC
                    else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                        java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                        cl.show(mainContainer, CARD_MENU);
                        menuPanel.requestFocusInWindow();
                    }
                    // trigger action when enter Enter o Space
                    else if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_SPACE) {
                        lobbyMenuPanel.requestFocusInWindow();
                        java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                        
                        if (lobbyMenuIndex[0] == 0) {
                            // start server on background separately
                            new Thread(() -> {
                                com.hueharvest.server.GameServer.main(new String[]{});
                            }).start();

                            // trigger instantly the transition without waiting in main()
                            transitToGame("localhost", cl, mainContainer, gamePanel);

                        } else {
                            // --- JOIN ROUTINE ---
                            cl.show(mainContainer, "JoinCodeCard");
                        }
                    }
                }
            });

            // Automatic focus controller for Card 3 screen
            lobbyMenuPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    lobbyMenuPanel.requestFocusInWindow();
                }
            });

            // ---------------------------------------------------------
            // CARD 4: JOIN LOBBY INPUT CODE SCREEN
            // ---------------------------------------------------------
            final int[] joinBtnIndex = {0}; // 0 = Enter Code, 1 = Cancel
            
            JPanel joinCodePanel = new JPanel(null) {
                // declare needed image variables as fields of the panel
                private java.awt.image.BufferedImage bgImage = null;
                private java.awt.image.BufferedImage inputCodeModal = null;
                private java.awt.image.BufferedImage enterCodeBtn = null;
                private java.awt.image.BufferedImage cancelBtn = null;
                private java.awt.image.BufferedImage selectorBtn = null;

                {
                    // load asset using AssetManager 
                    bgImage = com.hueharvest.client.AssetManager.getImage("overall_bg.png");
                    inputCodeModal = com.hueharvest.client.AssetManager.getImage("input_code_modal.png");
                    enterCodeBtn = com.hueharvest.client.AssetManager.getImage("enter_code_btn.png"); 
                    cancelBtn = com.hueharvest.client.AssetManager.getImage("cancel_btn.png");         
                    selectorBtn = com.hueharvest.client.AssetManager.getImage("selector2.png");
                    
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

                    // render background 
                    if (bgImage != null) {
                        g2d.drawImage(bgImage, 0, 0, panelWidth, panelHeight, null);
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, panelWidth, panelHeight);
                    }

                    // render "input_code_modal.png" centered
                    int modalX = 0, modalY = 0;
                    int modalW = 0, modalH = 0;
                    if (inputCodeModal != null) {
                        modalW = inputCodeModal.getWidth();
                        modalH = inputCodeModal.getHeight();
                        modalX = (panelWidth - modalW) / 2;
                        modalY = (panelHeight - modalH) / 2;
                        g2d.drawImage(inputCodeModal, modalX, modalY, null);
                    }

                    // row Layout for Enter and Cancel buttons 
                    java.awt.image.BufferedImage[] actionBtns = {enterCodeBtn, cancelBtn};
                    int buttonGap = 40;
                    int totalRowWidth = 0;

                    for (java.awt.image.BufferedImage btn : actionBtns) {
                        if (btn != null) totalRowWidth += btn.getWidth();
                    }
                    totalRowWidth += buttonGap * (actionBtns.length - 1);

                    int startRowX = modalX + (modalW - totalRowWidth) / 2;
                    int rowY = (inputCodeModal != null) ? (modalY + modalH - 95) : (panelHeight - 120); 

                    // render buttons and selector cursor 
                    int currentBtnX = startRowX;
                    Component[] comps = getComponents();
                    boolean fieldHasFocus = false;
                    for (Component c : comps) {
                        if (c instanceof JTextField && c.hasFocus()) {
                            fieldHasFocus = true;
                            break;
                        }
                    }

                    for (int i = 0; i < actionBtns.length; i++) {
                        java.awt.image.BufferedImage currentBtn = actionBtns[i];
                        if (currentBtn == null) continue;

                        int btnW = currentBtn.getWidth();
                        int btnH = currentBtn.getHeight();

                        g2d.drawImage(currentBtn, currentBtnX, rowY, null);

                        // Selector Cursor logic 
                        if (i == joinBtnIndex[0] && !fieldHasFocus && selectorBtn != null) {
                            int selW = selectorBtn.getWidth();
                            int selH = selectorBtn.getHeight();
                            
                            int selectorX = currentBtnX + (btnW - selW) / 2;
                            int selectorY = rowY + (btnH - selH) / 2;
                            
                            g2d.drawImage(selectorBtn, selectorX, selectorY, null);
                        }

                        currentBtnX += btnW + buttonGap;
                    }
                    g2d.dispose();
                }
            };
            joinCodePanel.setOpaque(false);
            joinCodePanel.setPreferredSize(new Dimension(totalWidth, totalHeight));

            // NATIVE SWING TEXT FIELD SETUP
            JTextField codeField = new JTextField();
            codeField.setFont(new Font("Monospaced", Font.BOLD, 28));
            Color customGreen = Color.decode("#b4c254");
            codeField.setForeground(customGreen); 
            codeField.setCaretColor(customGreen); 
            codeField.setOpaque(false); 
            codeField.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 0), 2)); 
            codeField.setHorizontalAlignment(JTextField.CENTER);

            // Spatial alignment math using temporary fallback 
            int tempModalW = 500;
            int tempModalH = 350;
            java.awt.image.BufferedImage testImg = com.hueharvest.client.AssetManager.getImage("input_code_modal.png");
            if (testImg != null) {
                tempModalW = testImg.getWidth();
                tempModalH = testImg.getHeight();
            }
            
            int mX = (totalWidth - tempModalW) / 2;
            int mY = (totalHeight - tempModalH) / 2;

            int fieldY = mY + (int)(tempModalH * 0.65) - 45; 
            int fieldX = mX + (tempModalW - 320) / 2 + (int)(tempModalW * 0.08);

            codeField.setBounds(fieldX, fieldY, 320, 50);
            joinCodePanel.add(codeField);
            codeField.setOpaque(false);
            codeField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));

            // FLOW CONTROL AT KEY NAVIGATION FOR INPUT LAYER
            codeField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
                        joinCodePanel.requestFocusInWindow();
                        joinCodePanel.repaint();
                    } else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                        codeField.setText("");
                        java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                        cl.show(mainContainer, "LobbyMenuCard");
                        if (lobbyMenuPanel != null) lobbyMenuPanel.requestFocusInWindow();
                    }
                    else if (keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                        String finalRoomCode = codeField.getText().trim();
                        if (!finalRoomCode.isEmpty()) {
                            System.out.println("[Socket] Connecting to client network room: " + finalRoomCode);
                            String serverIp = com.hueharvest.shared.NetworkUtils.roomCodeToIp(finalRoomCode);
                            gamePanel.connect(serverIp);
                            
                            java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                            cl.show(mainContainer, CARD_GAME);
                            gamePanel.requestFocusInWindow();
                        }
                    }
                }
            });

            // Listener for actual panel (when focus is on the buttons)
            joinCodePanel.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    
                    if (keyCode == java.awt.event.KeyEvent.VK_LEFT || keyCode == java.awt.event.KeyEvent.VK_A) {
                        joinBtnIndex[0] = 0; // Enter Code Button Focus
                        joinCodePanel.repaint();
                    } 
                    else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT || keyCode == java.awt.event.KeyEvent.VK_D) {
                        joinBtnIndex[0] = 1; // Cancel Button Focus
                        joinCodePanel.repaint();
                    } 
                    else if (keyCode == java.awt.event.KeyEvent.VK_UP || keyCode == java.awt.event.KeyEvent.VK_W) {
                        codeField.requestFocusInWindow();
                        joinCodePanel.repaint();
                    }
                    else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                        codeField.setText("");
                        java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                        cl.show(mainContainer, "LobbyMenuCard");
                        if (lobbyMenuPanel != null) lobbyMenuPanel.requestFocusInWindow();
                    }
                    else if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_SPACE) {
                        java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                        if (joinBtnIndex[0] == 0) {
                            // ENTER CODE CLICKED
                            String finalRoomCode = codeField.getText().trim();
                            if (!finalRoomCode.isEmpty()) {
                                System.out.println("[Socket] Connecting to client network room matching string: " + finalRoomCode);
                                String serverIp = com.hueharvest.shared.NetworkUtils.roomCodeToIp(finalRoomCode);
                                gamePanel.connect(serverIp);
                                cl.show(mainContainer, CARD_GAME);
                                gamePanel.requestFocusInWindow();
                            }
                        } else {
                            // CANCEL CLICKED
                            codeField.setText("");
                            cl.show(mainContainer, "LobbyMenuCard");
                            if (lobbyMenuPanel != null) lobbyMenuPanel.requestFocusInWindow();
                        }
                    }
                }
            });

            // Automatic focus handle when going back to this screen
            joinCodePanel.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    joinBtnIndex[0] = 0; 
                    codeField.setText("");
                    codeField.requestFocusInWindow();
                }
            });

            // ---------------------------------------------------------
            // CARD 5: ACTUAL GAME SCREEN CONTAINER & SIDEBAR
            // ---------------------------------------------------------
            JPanel gameContainer = new JPanel(new BorderLayout());
            
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
            burstStatusIcon.setBounds(163, 35, 70, 50); // X, Y, Width, Height
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

            gamePanel.setOnQuitCallback(() -> {
                cardLayout.show(mainContainer, CARD_MENU);
                menuPanel.requestFocusInWindow();
            });

            // ---------------------------------------------------------
            // CARD 6: ABOUT THE GAME OVERLAYS SCREEN
            // ---------------------------------------------------------
            JPanel aboutPanel = new JPanel(null) {
                private java.awt.image.BufferedImage bgImage = null;
                private java.awt.image.BufferedImage about1 = null;
                private java.awt.image.BufferedImage about2 = null;
                private java.awt.image.BufferedImage selectorBtn = null;
                private java.awt.image.BufferedImage nextBtnImg = null;
                private java.awt.image.BufferedImage backBtnImg = null;
                private int pageIndex = 0; // 0 = Page 1, 1 = Page 2
                private int selectedButtonIndex = 0; // 0 = NEXT/PREV, 1 = BACK

                {
                    bgImage = com.hueharvest.client.AssetManager.getImage("overall_bg.png");
                    about1 = com.hueharvest.client.AssetManager.getImage("aboutTheGame.png");
                    about2 = com.hueharvest.client.AssetManager.getImage("aboutTheGame (2).png");
                    selectorBtn = com.hueharvest.client.AssetManager.getImage("selector2.png");
                    nextBtnImg = com.hueharvest.client.AssetManager.getImage("next_btn.png");
                    backBtnImg = com.hueharvest.client.AssetManager.getImage("back_btn.png");
                    setDoubleBuffered(true);

                    setFocusable(true);
                    addKeyListener(new java.awt.event.KeyAdapter() {
                        @Override
                        public void keyPressed(java.awt.event.KeyEvent e) {
                            int keyCode = e.getKeyCode();
                            if (keyCode == java.awt.event.KeyEvent.VK_LEFT || keyCode == java.awt.event.KeyEvent.VK_A) {
                                selectedButtonIndex = 0;
                                repaint();
                            } else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT || keyCode == java.awt.event.KeyEvent.VK_D) {
                                selectedButtonIndex = 1;
                                repaint();
                            } else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                                java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                                cl.show(mainContainer, CARD_MENU);
                                menuPanel.requestFocusInWindow();
                            } else if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_SPACE) {
                                if (selectedButtonIndex == 0) {
                                    pageIndex = (pageIndex == 0) ? 1 : 0;
                                    repaint();
                                } else {
                                    java.awt.CardLayout cl = (java.awt.CardLayout) mainContainer.getLayout();
                                    cl.show(mainContainer, CARD_MENU);
                                    menuPanel.requestFocusInWindow();
                                }
                            }
                        }
                    });

                    addComponentListener(new java.awt.event.ComponentAdapter() {
                        @Override
                        public void componentShown(java.awt.event.ComponentEvent e) {
                            pageIndex = 0;
                            selectedButtonIndex = 0;
                            requestFocusInWindow();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int panelWidth = getWidth();
                    int panelHeight = getHeight();

                    // Render background
                    if (bgImage != null) {
                        g2d.drawImage(bgImage, 0, 0, panelWidth, panelHeight, null);
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, panelWidth, panelHeight);
                    }

                    // Render modal page image
                    java.awt.image.BufferedImage activeModal = (pageIndex == 0) ? about1 : about2;
                    if (activeModal != null) {
                        g2d.drawImage(activeModal, 0, 0, panelWidth, panelHeight, null);
                    } else {
                        // Fallback if image not loaded
                        int modalW = 640;
                        int modalH = 480;
                        int modalX = (panelWidth - modalW) / 2;
                        int modalY = (panelHeight - modalH) / 2 - 40;
                        g2d.setColor(new Color(230, 215, 195));
                        g2d.fillRoundRect(modalX, modalY, modalW, modalH, 20, 20);
                    }

                    // Render NEXT button image overlay
                    if (nextBtnImg != null) {
                        g2d.drawImage(nextBtnImg, 0, 0, panelWidth, panelHeight, null);
                    }

                    // Render BACK button image overlay
                    if (backBtnImg != null) {
                        g2d.drawImage(backBtnImg, 0, 0, panelWidth, panelHeight, null);
                    }

                    // Map 1000x750 design coordinates to scaled panelWidth x panelHeight for selector overlay
                    double scaleX = (double) panelWidth / 1000.0;
                    double scaleY = (double) panelHeight / 750.0;

                    int leftBtnX = (int) (220 * scaleX);
                    int rightBtnX = (int) (522 * scaleX);
                    int btnY = (int) (600 * scaleY);
                    int btnW = (int) (250 * scaleX);
                    int btnH = (int) (75 * scaleY);

                    // Draw selector overlay if selected
                    int activeX = (selectedButtonIndex == 0) ? leftBtnX : rightBtnX;
                    if (selectorBtn != null) {
                        int selW = selectorBtn.getWidth();
                        int selH = selectorBtn.getHeight();
                        int selectorX = activeX + (btnW - selW) / 2;
                        int selectorY = btnY + (btnH - selH) / 2;
                        g2d.drawImage(selectorBtn, selectorX, selectorY, null);
                    }

                    g2d.dispose();
                }
            };

            gameContainer.add(gamePanel, BorderLayout.CENTER);
            gameContainer.add(sidePanel, BorderLayout.EAST);

            // Assemble into Card Stack
            mainContainer.add(titlePanel, CARD_TITLE);
            mainContainer.add(menuPanel, CARD_MENU);
            mainContainer.add(lobbyMenuPanel, "LobbyMenuCard"); 
            mainContainer.add(joinCodePanel, "JoinCodeCard");   
            mainContainer.add(gameContainer, CARD_GAME);
            mainContainer.add(aboutPanel, CARD_ABOUT);

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
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        System.exit(0); // exit the game
                    } else {
                        titlePanel.removeKeyListener(this);
                        cardLayout.show(mainContainer, CARD_MENU);
                        menuPanel.requestFocusInWindow();
                    }
                }
            });

            titlePanel.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    titlePanel.requestFocusInWindow();
                    // add again the listener because we remove it from transition
                    titlePanel.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                System.exit(0);
                            } else {
                                titlePanel.removeKeyListener(this);
                                cardLayout.show(mainContainer, CARD_MENU);
                                menuPanel.requestFocusInWindow();
                            }
                        }
                    });
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
                    else if (keyCode == KeyEvent.VK_ESCAPE) {
                        cardLayout.show(mainContainer, CARD_TITLE);
                        titlePanel.requestFocusInWindow();
                    }
                }
            });

            // GAME CONTROL STATE UPDATER TIMER
            new Timer(200, e -> {
                GameState remoteState = gamePanel.getRemoteGameState();
                if (remoteState == null) return;

                if (remoteState.getStatus() == GameState.Status.LOBBY) {
                    for (int i = 0; i < 4; i++) {
                        playerScores[i].setText("Player " + (i + 1) + ": 0");
                    }
                    timerLabel.setText(GameState.INITIAL_TIME + "s");
                    burstStatusIcon.setIcon(null);
                    burstStatusIcon.setText("READY");
                    return;
                }

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

    private static void transitToGame(String ip, CardLayout cl, JPanel container, GamePanel gp) {
        SwingUtilities.invokeLater(() -> {
            gp.connect(ip);
            cl.show(container, CARD_GAME);
            gp.requestFocusInWindow();
        });
    }

    // ---------------------------------------------------------
    // MENU FUNCTION ACTIVATION ROUTINE
    // ---------------------------------------------------------
    private static void executeMenuOption(int index, JFrame frame, CardLayout cl, JPanel container, GamePanel gamePanel) {
        switch (index) {
            case 0: // 1. Enter Lobby Option Selected
                // Instead of JOptionPane, i-show na natin ang ginawa mong custom Lobby Selection Screen (CARD 3)
                cl.show(container, "LobbyMenuCard");
                
                // Hanapin natin yung lobbyMenuPanel sa loob ng container para ma-set ang keyboard focus sa kanya
                for (Component c : container.getComponents()) {
                    if (c instanceof JPanel && "LobbyMenuCard".equals(container.getLayout().toString())) {
                        // Safe fallback kung hindi direktang makuha, pero mas mainam na tawagan natin directly via layout transition
                    }
                }
                
                // At dahil dynamic ang focus handling mo, siguraduhing mag-request ng focus sa window
                // Tip: Mas maganda kung i-add mo rin ito sa componentShown listener ng lobbyMenuPanel sa main setup mo.
                container.revalidate();
                container.repaint();
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
                cl.show(container, CARD_ABOUT);
                break;
        }
    }
}