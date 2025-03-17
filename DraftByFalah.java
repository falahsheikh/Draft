import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class DraftByFalah extends JFrame {
    private JPanel mainPanel;
    private JPanel homePanel;
    private JTabbedPane homeTabbedPane;
    private JTabbedPane editorTabbedPane;
    private List<JTextPane> textPanes = new ArrayList<>();
    private Map<JTextPane, UUID> linkedDocuments = new HashMap<>();
    private Map<UUID, List<JTextPane>> documentGroups = new HashMap<>();
    private int windowCount = 1;
    private JLabel statusBar;
    private Map<JTextPane, File> filePaths = new HashMap<>();
    private String recentDocumentsPath;
    private Set<Integer> usedDocumentNumbers = new HashSet<>();

    private File currentDirectory;

    public DraftByFalah() {
        String recentDocumentsPath = promptForDirectory();
        this.recentDocumentsPath = recentDocumentsPath;

        this.currentDirectory = new File(System.getProperty("user.dir"));

        setTitle("Draft");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(236, 233, 216));
        add(mainPanel);

        createToggleHomePanelAction();

        editorTabbedPane = new JTabbedPane() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(236, 233, 216));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        editorTabbedPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        editorTabbedPane.setForeground(Color.BLACK);
        editorTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        editorTabbedPane.addChangeListener(e -> {
            JTextPane currentPane = getCurrentTextPane();
            if (currentPane != null) {
                updateStatusBar(currentPane);
            }
        });

        setTitle("Draft");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (homePanel.isVisible()) {
                    int panelHeight = 200;
                    homePanel.setPreferredSize(new Dimension(mainPanel.getWidth(), panelHeight));
                    homePanel.setMinimumSize(new Dimension(mainPanel.getWidth(), panelHeight));
                    homePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight));
                    homePanel.revalidate();
                    homePanel.repaint();
                }
            }
        });

        homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(new Color(236, 233, 216));
        homePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Documents",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12)));
        homePanel.setForeground(Color.BLACK);
        homePanel.setPreferredSize(new Dimension(getWidth(), 150));
        homePanel.setVisible(false);

        JMenuBar menuBar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(0, 0, 128));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        menuBar.setOpaque(true);
        menuBar.setBorder(BorderFactory.createEtchedBorder());

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.setForeground(Color.WHITE);
        JMenuItem newItem = new JMenuItem("New", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveAsItem = new JMenuItem("Save As...", KeyEvent.VK_S);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem closeItem = new JMenuItem("Close Window", KeyEvent.VK_C);
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_E);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));

        JMenuItem terminalItem = new JMenuItem("Terminal", KeyEvent.VK_T);
        terminalItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        terminalItem.addActionListener(e -> openTerminalWithTip());

        newItem.addActionListener(e -> createNewTextWindow());
        openItem.addActionListener(e -> openFile(null));
        saveItem.addActionListener(e -> saveFile(false));
        saveAsItem.addActionListener(e -> saveFile(true));
        closeItem.addActionListener(e -> closeCurrentWindow());
        exitItem.addActionListener(e -> exitApplication());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.setForeground(Color.WHITE);
        JMenuItem cutItem = new JMenuItem("Cut", KeyEvent.VK_C);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        JMenuItem copyItem = new JMenuItem("Copy", KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        JMenuItem pasteItem = new JMenuItem("Paste", KeyEvent.VK_P);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        JMenuItem undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        JMenuItem redoItem = new JMenuItem("Redo", KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        JMenuItem selectAllItem = new JMenuItem("Select All", KeyEvent.VK_S);
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        JMenuItem findItem = new JMenuItem("Find...", KeyEvent.VK_F);
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        JMenuItem replaceItem = new JMenuItem("Replace...", KeyEvent.VK_R);
        replaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));

        cutItem.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.cut();
        });
        copyItem.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.copy();
        });
        pasteItem.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.paste();
        });
        undoItem.addActionListener(e -> performUndoRedo(true));
        redoItem.addActionListener(e -> performUndoRedo(false));
        selectAllItem.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.selectAll();
        });
        findItem.addActionListener(e -> findText());
        replaceItem.addActionListener(e -> replaceText());

        addTerminalShortcut();


        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);
        editMenu.addSeparator();
        editMenu.add(findItem);
        editMenu.add(replaceItem);

        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        windowMenu.setForeground(Color.WHITE);

        JMenuItem duplicateItem = new JMenuItem("Duplicate Window", KeyEvent.VK_D);
        duplicateItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        JMenuItem renameTabItem = new JMenuItem("Rename Current Tab", KeyEvent.VK_R);
        renameTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        JMenuItem splitViewItem = new JMenuItem("Open in Split View", KeyEvent.VK_O);
        splitViewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));

        duplicateItem.addActionListener(e -> duplicateCurrentWindow());
        renameTabItem.addActionListener(e -> renameCurrentTab());
        splitViewItem.addActionListener(e -> openInSplitView());

        windowMenu.add(duplicateItem);
        windowMenu.add(renameTabItem);
        windowMenu.add(splitViewItem);
        windowMenu.add(terminalItem);

        addTerminalShortcut();



        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.setForeground(Color.WHITE);

        JMenuItem homeItem = new JMenuItem("Toggle Home Panel", KeyEvent.VK_T);
        homeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
        JMenuItem wordCountItem = new JMenuItem("Word Count", KeyEvent.VK_W);
        wordCountItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

        homeItem.addActionListener(e -> toggleHomePanel());
        wordCountItem.addActionListener(e -> showWordCount());

        viewMenu.add(homeItem);
        viewMenu.add(wordCountItem);

        JMenu formatMenu = new JMenu("Format");
        formatMenu.setMnemonic(KeyEvent.VK_O);
        formatMenu.setForeground(Color.WHITE);

        JMenuItem fontIncreaseItem = new JMenuItem("Zoom In");
        fontIncreaseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ActionEvent.CTRL_MASK));
        JMenuItem fontDecreaseItem = new JMenuItem("Zoom out");
        fontDecreaseItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));

        fontIncreaseItem.addActionListener(e -> changeFontSize(1));
        fontDecreaseItem.addActionListener(e -> changeFontSize(-1));

        formatMenu.addSeparator();
        formatMenu.add(fontIncreaseItem);
        formatMenu.add(fontDecreaseItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.setForeground(Color.WHITE);

        JMenuItem basicCommandsItem = new JMenuItem("Basic Commands");
        basicCommandsItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this, """
        Basic Commands:
        
        File Operations:
        Ctrl+N       - New Document
        Ctrl+O       - Open Document
        Ctrl+S       - Save Document
        Ctrl+Shift+S - Save As...
        Ctrl+W       - Close Window
        Ctrl+Q       - Exit
        
        Edit Operations:
        Ctrl+Z       - Undo
        Ctrl+Y       - Redo
        Ctrl+A       - Select All
        Ctrl+F       - Find Text
        Ctrl+H       - Replace Text
        
        Window Operations:
        Ctrl+D       - Duplicate Window
        Ctrl+R       - Rename Current Tab
        Ctrl+T       - Open in Split View
        Ctrl+Shift+T - Open Terminal
        
        View Operations:
        Ctrl+Shift+H - Toggle Home Panel
        Ctrl+Shift+W - Word Count
        
        Format Operations:
        Ctrl+Plus (+)  - Zoom In
        Ctrl+Minus (-) - Zoom Out
        """, "Draft Help", JOptionPane.INFORMATION_MESSAGE)
        );

        JMenuItem aboutItem = new JMenuItem("About Draft...");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this, """
        Draft v0.0.1
        
        Author: Falah Sheikh
                
        © 2025 TENSA Solutions
        All Rights Reserved.
        """, "About Draft", JOptionPane.INFORMATION_MESSAGE)
        );

        helpMenu.add(basicCommandsItem);
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(windowMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        JToolBar toolBar = new JToolBar() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(192, 192, 192));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(128, 128, 128));
                g2d.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createRaisedBevelBorder());
        toolBar.setBackground(new Color(192, 192, 192));

        JButton newButton = createToolbarButton("New", "New Document (Ctrl+N)");
        JButton openButton = createToolbarButton("Open", "Open Document (Ctrl+O)");
        JButton saveButton = createToolbarButton("Save", "Save Document (Ctrl+S)");
        JButton cutButton = createToolbarButton("Cut", "Cut Selection (Ctrl+X)");
        JButton copyButton = createToolbarButton("Copy", "Copy Selection (Ctrl+C)");
        JButton pasteButton = createToolbarButton("Paste", "Paste (Ctrl+V)");
        JButton undoButton = createToolbarButton("Undo", "Undo (Ctrl+Z)");
        JButton redoButton = createToolbarButton("Redo", "Redo (Ctrl+Y)");
        JButton findButton = createToolbarButton("Find", "Find Text (Ctrl+F)");
        JButton helpButton = createToolbarButton("Help", "Help Topics (F1)");

        newButton.addActionListener(e -> createNewTextWindow());
        openButton.addActionListener(e -> openFile(null));
        saveButton.addActionListener(e -> saveFile(false));

        cutButton.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.cut();
        });

        copyButton.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.copy();
        });

        pasteButton.addActionListener(e -> {
            JTextPane textPane = getCurrentTextPane();
            if (textPane != null) textPane.paste();
        });

        undoButton.addActionListener(e -> performUndoRedo(true));
        redoButton.addActionListener(e -> performUndoRedo(false));

        findButton.addActionListener(e -> findText());

        helpButton.addActionListener(e -> showHelp());

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.add(helpButton);

        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(editorTabbedPane, BorderLayout.CENTER);

        statusBar = new JLabel(" Ready") {
            private boolean isPressed = false;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(236, 233, 216));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                drawHomeButton(g2d, -10, -29, isPressed);
                g2d.rotate(Math.toRadians(90));
                g2d.setColor(new Color(0, 0, 128));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();
                g2d.drawString(getText(), (getHeight() - textWidth) / 2, -5);
            }

            private void drawHomeButton(Graphics2D g2d, int x, int y, boolean pressed) {
                int squareSize = 50;
                int borderThickness = 1;
                int bottomBorderThickness = 2;
                int pressOffset = pressed ? 1 : 0;
                g2d.setColor(new Color(0, 0, 128));
                g2d.fillRect(x + pressOffset, y + pressOffset, squareSize, squareSize);
                g2d.setColor(Color.WHITE);

                if (!pressed) {
                    g2d.fillRect(x, y, squareSize, borderThickness);
                    g2d.fillRect(x, y, borderThickness, squareSize);
                    g2d.fillRect(x + squareSize - borderThickness, y, borderThickness, squareSize);
                    g2d.fillRect(x, y + squareSize - bottomBorderThickness, squareSize, bottomBorderThickness);
                } else {
                    g2d.fillRect(x + squareSize - borderThickness + pressOffset, y + pressOffset,
                            borderThickness, squareSize);
                    g2d.fillRect(x + pressOffset, y + squareSize - bottomBorderThickness + pressOffset,
                            squareSize, bottomBorderThickness);
                    g2d.setColor(new Color(128, 128, 128));
                    g2d.fillRect(x + pressOffset, y + pressOffset, squareSize, borderThickness);
                    g2d.fillRect(x + pressOffset, y + pressOffset, borderThickness, squareSize);
                }

                int houseOffsetX = -4;
                int houseOffsetY = 14;
                int houseWidth = 15;
                int houseHeight = 12;
                int roofHeight = 6;
                int houseX = x + (squareSize - houseWidth) / 2 + houseOffsetX + pressOffset;
                int houseY = y + (squareSize - houseHeight) / 2 + houseOffsetY + pressOffset;
                g2d.setColor(Color.WHITE);
                int[] xPoints = {houseX, houseX + houseWidth / 2, houseX + houseWidth};
                int[] yPoints = {houseY + roofHeight, houseY, houseY + roofHeight};
                g2d.fillPolygon(xPoints, yPoints, 3);
                int bodyWidth = (int)(houseWidth * 0.8);
                int bodyHeight = houseHeight - roofHeight;
                int bodyX = houseX + (houseWidth - bodyWidth) / 2;
                int bodyY = houseY + roofHeight;
                g2d.fillRect(bodyX, bodyY, bodyWidth, bodyHeight);
                int doorWidth = bodyWidth / 3;
                int doorHeight = (int)(bodyHeight * 0.6);
                int doorX = bodyX + (bodyWidth - doorWidth) / 2;
                int doorY = bodyY + bodyHeight - doorHeight;
                g2d.setColor(new Color(0, 0, 128));
                g2d.fillRect(doorX, doorY, doorWidth, doorHeight);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 100);
            }

            private Rectangle homeButtonBounds = new Rectangle(-10, -29, 50, 50);

            public void setButtonPressed(boolean pressed) {
                this.isPressed = pressed;
                repaint();
            }
        };

        statusBar.setForeground(Color.BLACK);
        statusBar.setOpaque(true);
        statusBar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        statusBar.setToolTipText("Toggle Home Panel (Ctrl+D)");

        statusBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getX() <= 40 && e.getY() >= 0 && e.getY() <= 50) {
                    statusBar.putClientProperty("pressed", Boolean.TRUE);
                    try {
                        Method method = statusBar.getClass().getMethod("setButtonPressed", boolean.class);
                        method.invoke(statusBar, true);
                    } catch (Exception ex) {
                        statusBar.repaint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (Boolean.TRUE.equals(statusBar.getClientProperty("pressed"))) {
                    statusBar.putClientProperty("pressed", Boolean.FALSE);
                    try {
                        Method method = statusBar.getClass().getMethod("setButtonPressed", boolean.class);
                        method.invoke(statusBar, false);
                    } catch (Exception ex) {
                        statusBar.repaint();
                    }

                    if (e.getX() <= 40 && e.getY() >= 0 && e.getY() <= 50) {
                        toggleHomePanel();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (Boolean.TRUE.equals(statusBar.getClientProperty("pressed"))) {
                    statusBar.putClientProperty("pressed", Boolean.FALSE);
                    try {
                        Method method = statusBar.getClass().getMethod("setButtonPressed", boolean.class);
                        method.invoke(statusBar, false);
                    } catch (Exception ex) {
                        statusBar.repaint();
                    }
                }
            }
        });

        mainPanel.add(statusBar, BorderLayout.EAST);
        mainPanel.setComponentZOrder(statusBar, 0);

        mainPanel.revalidate();
        mainPanel.repaint();

        loadRecentDocuments();

        setupTabPopupMenu();

        createNewTextWindow();
    }

    private void showTerminalTipPopup() {

        if (shouldShowTerminalTip()) {

            JDialog tipDialog = new JDialog(this, "Terminal Tip", true);
            tipDialog.setLayout(new BorderLayout());
            tipDialog.setMinimumSize(new Dimension(550, 450));
            tipDialog.setLocationRelativeTo(this);


            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


            String[] imagePaths = {
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_1.png",
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_2.png",
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_3.png",
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_4.png",
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_5.png",
                    "/Users/falahsheikh/Desktop/workSpace/draft_folder/term_6.png"
            };

            String[] imageTexts = {
                    "Open the Terminal with Ctrl+Shift+T or from Window > Terminal. The terminal recognizes most basic Linux commands for navigating your file system.",
                    "Use the terminal to change directories (cd command) and navigate to any folder you want to display in the Documents panel.",
                    "After navigating to your desired directory, use the 'render' command to update the Documents panel with the contents of the current location.",
                    "A confirmation popup will appear showing the path change. Review it and click 'OK' to update the Documents panel with the new directory contents.",
                    "The Documents panel will now display all files from your selected directory, making it easier to access your project files.",
                    "You can repeat this process anytime to quickly switch between different project directories without leaving the application."
            };


            final int[] currentImageIndex = {0};


            JPanel imagePanel = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(500, 300);
                }
            };
            imagePanel.setBackground(Color.WHITE);
            imagePanel.setBorder(BorderFactory.createEtchedBorder());


            JLabel imageLabel = new JLabel("Loading image...", JLabel.CENTER);
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setVerticalAlignment(JLabel.CENTER);
            imagePanel.add(imageLabel, BorderLayout.CENTER);


            loadAndResizeImage(imagePaths[currentImageIndex[0]], imageLabel, 500, 300);


            JTextArea tipText = new JTextArea(imageTexts[Math.min(currentImageIndex[0], imageTexts.length-1)]);
            tipText.setEditable(false);
            tipText.setLineWrap(true);
            tipText.setWrapStyleWord(true);
            tipText.setBackground(contentPanel.getBackground());
            tipText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            tipText.setPreferredSize(new Dimension(450, 60));


            JPanel imageControlPanel = new JPanel();
            JButton prevButton = new JButton("Previous");
            JButton nextButton = new JButton("Next");

            prevButton.addActionListener(e -> {
                currentImageIndex[0] = (currentImageIndex[0] - 1 + imagePaths.length) % imagePaths.length;
                loadAndResizeImage(imagePaths[currentImageIndex[0]], imageLabel, 500, 300);


                int textIndex = Math.min(currentImageIndex[0], imageTexts.length-1);
                tipText.setText(imageTexts[textIndex]);
            });

            nextButton.addActionListener(e -> {
                currentImageIndex[0] = (currentImageIndex[0] + 1) % imagePaths.length;
                loadAndResizeImage(imagePaths[currentImageIndex[0]], imageLabel, 500, 300);


                int textIndex = Math.min(currentImageIndex[0], imageTexts.length-1);
                tipText.setText(imageTexts[textIndex]);
            });

            imageControlPanel.add(prevButton);
            imageControlPanel.add(nextButton);


            JPanel mainContentPanel = new JPanel(new BorderLayout(5, 5));
            mainContentPanel.add(imagePanel, BorderLayout.CENTER);
            mainContentPanel.add(tipText, BorderLayout.SOUTH);


            JPanel buttonPanel = new JPanel();
            JButton okButton = new JButton("OK");
            JButton dontAskAgainButton = new JButton("Don't Ask Again");

            okButton.addActionListener(e -> {
                tipDialog.dispose();
            });

            dontAskAgainButton.addActionListener(e -> {
                setShouldShowTerminalTip(false);
                tipDialog.dispose();
            });

            buttonPanel.add(okButton);
            buttonPanel.add(dontAskAgainButton);


            contentPanel.add(imageControlPanel, BorderLayout.NORTH);
            contentPanel.add(mainContentPanel, BorderLayout.CENTER);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);


            tipDialog.add(contentPanel);
            tipDialog.pack();
            tipDialog.setVisible(true);
        }
    }


    private void loadAndResizeImage(String imagePath, JLabel imageLabel, int maxWidth, int maxHeight) {
        try {
            File imageFile = new File(imagePath);
            System.out.println("Loading image from: " + imageFile.getAbsolutePath());

            if (imageFile.exists() && imageFile.canRead()) {

                ImageIcon originalIcon = new ImageIcon(imageFile.getAbsolutePath());


                if (originalIcon.getIconWidth() <= 0) {
                    System.err.println("Image appears to be invalid or empty");
                    imageLabel.setIcon(null);
                    imageLabel.setText("Invalid image file");
                    return;
                }


                int originalWidth = originalIcon.getIconWidth();
                int originalHeight = originalIcon.getIconHeight();


                double widthScale = (double) maxWidth / originalWidth;
                double heightScale = (double) maxHeight / originalHeight;


                double scale = Math.min(widthScale, heightScale);


                int scaledWidth = (int) (originalWidth * scale);
                int scaledHeight = (int) (originalHeight * scale);


                Image scaledImage = originalIcon.getImage().getScaledInstance(
                        scaledWidth, scaledHeight, Image.SCALE_SMOOTH);


                ImageIcon scaledIcon = new ImageIcon(scaledImage);


                imageLabel.setIcon(scaledIcon);
                imageLabel.setText("");

                System.out.println("Image resized from " + originalWidth + "x" + originalHeight +
                        " to " + scaledWidth + "x" + scaledHeight);
            } else {
                System.err.println("Image file does not exist or cannot be read: " + imageFile.getAbsolutePath());
                imageLabel.setIcon(null);
                imageLabel.setText("Image not found: " + imagePath);
            }
        } catch (Exception ex) {
            System.err.println("Error loading image: " + ex.getMessage());
            ex.printStackTrace();
            imageLabel.setIcon(null);
            imageLabel.setText("Error loading image");
        }
    }


    private void updateImage(String imagePath, JLabel imageLabel) {
        try {
            File imageFile = new File(imagePath);
            System.out.println("Loading image from: " + imageFile.getAbsolutePath());

            if (imageFile.exists() && imageFile.canRead()) {
                ImageIcon newImage = new ImageIcon(imageFile.getAbsolutePath());

                if (newImage.getIconWidth() <= 0) {
                    System.err.println("Image appears to be invalid or empty");
                    imageLabel.setIcon(null);
                    imageLabel.setText("Invalid image file");
                } else {
                    imageLabel.setIcon(newImage);
                    imageLabel.setText("");
                }
            } else {
                System.err.println("Image file does not exist or cannot be read: " + imageFile.getAbsolutePath());
                imageLabel.setIcon(null);
                imageLabel.setText("Image not found: " + imagePath);
            }
        } catch (Exception ex) {
            System.err.println("Error loading image: " + ex.getMessage());
            ex.printStackTrace();
            imageLabel.setIcon(null);
            imageLabel.setText("Error loading image");
        }
    }

    private boolean shouldShowTerminalTip() {


        return showTerminalTip;
    }

    private void setShouldShowTerminalTip(boolean value) {


        showTerminalTip = value;
    }

    private void openTerminalWithTip() {

        showTerminalTipPopup();


        createTerminalTab();
    }


    private static boolean showTerminalTip = true;

    private void openInSplitView() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length == 2) {
                openFilesInSplitView(files);
            } else {
                JOptionPane.showMessageDialog(this, "You must select exactly 2 files for split view.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String executeCommand(String command, File workingDirectory) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {

                processBuilder.command("cmd.exe", "/c", command);
            } else {

                processBuilder.command("bash", "-c", command);
            }
            processBuilder.directory(workingDirectory);
            Process process = processBuilder.start();


            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }


            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                output.append(line).append("\n");
            }


            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command failed with exit code: ").append(exitCode).append("\n");
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage()).append("\n");
        }
        return output.toString();
    }

    private void openFilesInSplitView(File[] files) {
        if (files.length != 2) {
            JOptionPane.showMessageDialog(this, "You must select exactly 2 files for split view.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);

        JTextPane leftPane = createTextPaneForFile(files[0]);
        JTextPane rightPane = createTextPaneForFile(files[1]);

        splitPane.setLeftComponent(new JScrollPane(leftPane));
        splitPane.setRightComponent(new JScrollPane(rightPane));

        String tabTitle = files[0].getName() + " | " + files[1].getName();
        editorTabbedPane.addTab(tabTitle, splitPane);
        editorTabbedPane.setSelectedIndex(editorTabbedPane.getTabCount() - 1);

        filePaths.put(leftPane, files[0]);
        filePaths.put(rightPane, files[1]);

        statusBar.setText(" Opened in split view: " + tabTitle);
    }

    private JTextPane createTextPaneForFile(File file) {
        JTextPane textPane = new JTextPane();
        textPane.setFont(new Font("Arial", Font.PLAIN, 12));
        textPane.setBackground(Color.WHITE);
        textPane.setForeground(Color.BLACK);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            textPane.setText(content.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        UUID paneId = UUID.randomUUID();
        linkedDocuments.put(textPane, paneId);
        List<JTextPane> group = new ArrayList<>();
        group.add(textPane);
        documentGroups.put(paneId, group);

        textPanes.add(textPane);
        filePaths.put(textPane, file);

        UndoManager undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(undoManager);
        textPane.putClientProperty("undoManager", undoManager);

        return textPane;
    }

    private class ReadOnlyDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            fb.insertString(offset, text, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            fb.remove(offset, length);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            fb.replace(offset, length, text, attrs);
        }
    }

    private void createToggleHomePanelAction() {
        Action toggleHomePanelAction = new AbstractAction("Toggle Home Panel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleHomePanel();
            }
        };

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK);
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "toggleHomePanel");
        mainPanel.getActionMap().put("toggleHomePanel", toggleHomePanelAction);
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed()) {
                    g.setColor(new Color(192, 192, 192));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(128, 128, 128));
                    g.drawLine(0, 0, getWidth()-1, 0);
                    g.drawLine(0, 0, 0, getHeight()-1);
                    g.setColor(new Color(220, 220, 220));
                    g.drawLine(getWidth()-1, 1, getWidth()-1, getHeight()-1);
                    g.drawLine(1, getHeight()-1, getWidth()-1, getHeight()-1);
                } else {
                    g.setColor(new Color(192, 192, 192));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(255, 255, 255));
                    g.drawLine(0, 0, getWidth()-2, 0);
                    g.drawLine(0, 0, 0, getHeight()-2);
                    g.setColor(new Color(128, 128, 128));
                    g.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1);
                    g.drawLine(0, getHeight()-1, getWidth()-1, getHeight()-1);
                    g.setColor(new Color(160, 160, 160));
                    g.drawLine(getWidth()-2, 1, getWidth()-2, getHeight()-2);
                    g.drawLine(1, getHeight()-2, getWidth()-2, getHeight()-2);
                }

                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();

                if (getModel().isPressed()) {
                    g.setColor(Color.BLACK);
                    g.setFont(new Font("System", Font.PLAIN, 11));
                    g.drawString(getText(), (getWidth() - textWidth) / 2 + 1,
                            (getHeight() - textHeight) / 2 + fm.getAscent() + 1);
                } else {
                    g.setColor(Color.BLACK);
                    g.setFont(new Font("System", Font.PLAIN, 11));
                    g.drawString(getText(), (getWidth() - textWidth) / 2,
                            (getHeight() - textHeight) / 2 + fm.getAscent());
                }
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);

        button.setToolTipText(tooltip);
        button.setFont(new Font("System", Font.PLAIN, 11));

        button.setMargin(new Insets(2, 2, 2, 2));

        if (text.length() <= 1) {
            button.setPreferredSize(new Dimension(24, 24));
        } else if (text.length() <= 4) {
            button.setPreferredSize(new Dimension(36, 24));
        } else {
            button.setPreferredSize(new Dimension(48, 24));
        }

        return button;
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this, """
            Basic Commands:
            
            File Operations:
            Ctrl+N       - New Document
            Ctrl+O       - Open Document
            Ctrl+S       - Save Document
            Ctrl+Shift+S - Save As...
            Ctrl+W       - Close Window
            Ctrl+Q       - Exit
            
            Edit Operations:
            Ctrl+Z       - Undo
            Ctrl+Y       - Redo
            Ctrl+A       - Select All
            Ctrl+F       - Find Text
            Ctrl+H       - Replace Text
            
            Window Operations:
            Ctrl+D       - Duplicate Window
            Ctrl+R       - Rename Current Tab
            Ctrl+T       - Open in Split View
            Ctrl+Shift+T - Open Terminal

            
            View Operations:
            Ctrl+Shift+H - Toggle Home Panel
            Ctrl+Shift+W - Word Count
            
            Format Operations:
            Ctrl+Plus (+)  - Zoom In
            Ctrl+Minus (-) - Zoom Out
            """, "Draft Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void createNewTextWindow() {
        final JTextPane textPane = new JTextPane();
        textPane.setFont(new Font("Arial", Font.PLAIN, 12));
        textPane.setBackground(Color.WHITE);
        textPane.setForeground(Color.BLACK);

        final UndoManager undoManager = new UndoManager();
        Document doc = textPane.getDocument();
        doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        textPane.putClientProperty("undoManager", undoManager);

        UUID docId = UUID.randomUUID();
        linkedDocuments.put(textPane, docId);

        List<JTextPane> group = new ArrayList<>();
        group.add(textPane);
        documentGroups.put(docId, group);

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }
        });

        textPane.addCaretListener(e -> updateStatusBar(textPane));

        JScrollPane scrollPane = new JScrollPane(textPane);

        int documentNumber = findNextAvailableDocumentNumber();
        usedDocumentNumbers.add(documentNumber);
        String tabTitle = "Document " + documentNumber;

        editorTabbedPane.addTab(tabTitle, scrollPane);
        editorTabbedPane.setSelectedIndex(editorTabbedPane.getTabCount() - 1);

        textPanes.add(textPane);

        filePaths.put(textPane, null);

        textPane.requestFocus();
        statusBar.setText(" New document created");
    }

    private int findNextAvailableDocumentNumber() {
        int number = 1;
        while (usedDocumentNumbers.contains(number)) {
            number++;
        }
        return number;
    }

    private void setupTabPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameCurrentTab());

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> closeCurrentWindow());

        JMenuItem duplicateItem = new JMenuItem("Duplicate");
        duplicateItem.addActionListener(e -> duplicateCurrentWindow());

        JMenuItem duplicateSplitViewItem = new JMenuItem("Duplicate in Split View", KeyEvent.VK_S);
        duplicateSplitViewItem.addActionListener(e -> duplicateInSplitView());

        popupMenu.add(renameItem);
        popupMenu.add(closeItem);
        popupMenu.add(duplicateItem);
        popupMenu.add(duplicateSplitViewItem);

        editorTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkForPopupTrigger(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkForPopupTrigger(e);
            }

            private void checkForPopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int tabIndex = editorTabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 0) {
                        editorTabbedPane.setSelectedIndex(tabIndex);
                        popupMenu.show(editorTabbedPane, e.getX(), e.getY());
                    }
                } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int tabIndex = editorTabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 0) {
                        renameTab(tabIndex);
                    }
                }
            }
        });
    }

    private void renameTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < editorTabbedPane.getTabCount()) {
            String currentName = editorTabbedPane.getTitleAt(tabIndex);
            String newName = JOptionPane.showInputDialog(this, "Enter new tab name:", currentName);
            if (newName != null && !newName.trim().isEmpty()) {
                editorTabbedPane.setTitleAt(tabIndex, newName);
                updateStatusBar(getCurrentTextPane());
            }
        }
    }

    private void renameCurrentTab() {
        renameTab(editorTabbedPane.getSelectedIndex());
    }

    private void duplicateCurrentWindow() {
        int selectedIndex = editorTabbedPane.getSelectedIndex();
        if (selectedIndex == -1) {
            return;
        }

        Component selectedComponent = editorTabbedPane.getComponentAt(selectedIndex);

        if (selectedComponent instanceof JPanel) {
            JPanel selectedPanel = (JPanel) selectedComponent;
            if (Boolean.TRUE.equals(selectedPanel.getClientProperty("isTerminal"))) {

                JOptionPane.showMessageDialog(this,
                        "Cannot duplicate the terminal tab.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        JTextPane currentTextPane = getCurrentTextPane();
        if (currentTextPane != null) {
            final JTextPane newTextPane = new JTextPane();
            newTextPane.setFont(currentTextPane.getFont());
            newTextPane.setBackground(Color.WHITE);
            newTextPane.setForeground(Color.BLACK);

            final UndoManager undoManager = new UndoManager();
            Document doc = newTextPane.getDocument();
            doc.addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
            newTextPane.putClientProperty("undoManager", undoManager);

            UUID groupId = linkedDocuments.get(currentTextPane);

            newTextPane.setText(currentTextPane.getText());

            linkedDocuments.put(newTextPane, groupId);
            List<JTextPane> group = documentGroups.get(groupId);
            if (group == null) {
                group = new ArrayList<>();
                documentGroups.put(groupId, group);
            }
            group.add(newTextPane);

            newTextPane.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateLinkedDocuments(newTextPane);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateLinkedDocuments(newTextPane);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateLinkedDocuments(newTextPane);
                }
            });

            newTextPane.addCaretListener(e -> updateStatusBar(newTextPane));

            JScrollPane scrollPane = new JScrollPane(newTextPane);

            int currentIndex = editorTabbedPane.getSelectedIndex();
            String title = editorTabbedPane.getTitleAt(currentIndex);

            textPanes.add(newTextPane);
            editorTabbedPane.addTab(title, scrollPane);
            editorTabbedPane.setSelectedIndex(editorTabbedPane.getTabCount() - 1);

            newTextPane.requestFocus();
            statusBar.setText(" Window duplicated");

            if (filePaths.containsKey(currentTextPane)) {
                filePaths.put(newTextPane, filePaths.get(currentTextPane));
            }
        }
    }

    private void duplicateInSplitView() {
        JTextPane currentTextPane = getCurrentTextPane();
        if (currentTextPane != null) {
            File currentFile = filePaths.get(currentTextPane);
            if (currentFile == null) {
                JOptionPane.showMessageDialog(this, "No file is associated with the current tab.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(0.5);
            splitPane.setResizeWeight(0.5);

            JTextPane leftPane = createTextPaneForFile(currentFile);
            JScrollPane leftScrollPane = new JScrollPane(leftPane);

            JTextPane rightPane = createTextPaneForFile(currentFile);
            JScrollPane rightScrollPane = new JScrollPane(rightPane);

            UUID groupId = linkedDocuments.get(currentTextPane);
            if (groupId == null) {
                groupId = UUID.randomUUID();
                linkedDocuments.put(currentTextPane, groupId);
                List<JTextPane> group = new ArrayList<>();
                group.add(currentTextPane);
                documentGroups.put(groupId, group);
            }
            linkedDocuments.put(leftPane, groupId);
            linkedDocuments.put(rightPane, groupId);
            documentGroups.get(groupId).add(leftPane);
            documentGroups.get(groupId).add(rightPane);

            addMirroringDocumentListener(leftPane, groupId);
            addMirroringDocumentListener(rightPane, groupId);

            splitPane.setLeftComponent(leftScrollPane);
            splitPane.setRightComponent(rightScrollPane);

            String tabTitle = currentFile.getName() + " | " + currentFile.getName();
            editorTabbedPane.addTab(tabTitle, splitPane);
            editorTabbedPane.setSelectedIndex(editorTabbedPane.getTabCount() - 1);

            statusBar.setText(" Duplicated in split view: " + currentFile.getName());
        }
    }

    private void addMirroringDocumentListener(JTextPane textPane, UUID groupId) {
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLinkedDocuments(textPane);
            }
        });
    }

    private void updateLinkedDocuments(JTextPane sourcePane) {
        if (!sourcePane.isEditable()) return;

        String sourceContent = sourcePane.getText();
        UUID groupId = linkedDocuments.get(sourcePane);

        if (groupId != null) {
            List<JTextPane> group = documentGroups.get(groupId);
            if (group != null) {
                for (JTextPane pane : group) {
                    if (pane != sourcePane) {
                        pane.setEditable(false);
                        pane.setText(sourceContent);
                        pane.setEditable(true);
                    }
                }
            }
        }
    }

    private void updateStatusBar(JTextPane textPane) {
        try {
            int pos = textPane.getCaretPosition();
            int line = textPane.getDocument().getDefaultRootElement().getElementIndex(pos) + 1;
            int col = pos - textPane.getDocument().getDefaultRootElement().getElement(line - 1).getStartOffset() + 1;

            int tabIndex = editorTabbedPane.indexOfComponent(textPane.getParent().getParent());
            String tabName = "Untitled";
            if (tabIndex >= 0) {
                tabName = editorTabbedPane.getTitleAt(tabIndex);
            }

            statusBar.setText(" " + tabName + " | Line: " + line + " Col: " + col);
        } catch (Exception ex) {
            statusBar.setText(" Ready");
        }
    }

    private void closeCurrentWindow() {
        int selectedIndex = editorTabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {

            if (editorTabbedPane.getTabCount() <= 1) {
                JOptionPane.showMessageDialog(this,
                        "Cannot close the last window.",
                        "Draft", JOptionPane.INFORMATION_MESSAGE);
                return;
            }


            Component selectedComponent = editorTabbedPane.getComponentAt(selectedIndex);
            JTextPane textPane = null;
            if (selectedComponent instanceof JScrollPane) {
                textPane = (JTextPane) ((JScrollPane) selectedComponent).getViewport().getView();
            }


            if (textPane != null) {

                textPanes.remove(textPane);


                filePaths.remove(textPane);


                UUID groupId = linkedDocuments.get(textPane);
                if (groupId != null) {
                    List<JTextPane> group = documentGroups.get(groupId);
                    if (group != null) {
                        group.remove(textPane);
                        if (group.isEmpty()) {
                            documentGroups.remove(groupId);
                        }
                    }
                    linkedDocuments.remove(textPane);
                }


                String tabTitle = editorTabbedPane.getTitleAt(selectedIndex);
                if (tabTitle.startsWith("Document ")) {
                    try {
                        int documentNumber = Integer.parseInt(tabTitle.substring("Document ".length()));
                        usedDocumentNumbers.remove(documentNumber);
                    } catch (NumberFormatException e) {

                    }
                }
            }


            editorTabbedPane.removeTabAt(selectedIndex);


            statusBar.setText(" Window closed");
        }
    }

    private JToolBar createToolbar() {
        JToolBar toolBar = new JToolBar() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(192, 192, 192));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(128, 128, 128));
                g2d.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createRaisedBevelBorder());
        toolBar.setBackground(new Color(192, 192, 192));

        class OldStyleSeparator extends JComponent {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(6, 24);
            }

            @Override
            protected void paintComponent(Graphics g) {
                int height = getHeight();
                int x = 3;

                g.setColor(new Color(128, 128, 128));
                g.drawLine(x, 4, x, height - 5);
                g.setColor(new Color(255, 255, 255));
                g.drawLine(x + 1, 4, x + 1, height - 5);
            }
        }

        return toolBar;
    }

    private JPanel createDocumentPanel(JTextPane textPane, String tabName) {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(tabName, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(label, BorderLayout.NORTH);

        JTextPane newPane = new JTextPane();
        newPane.setText(textPane.getText());
        newPane.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(newPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String promptForDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory for Documents");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            return selectedDirectory.getAbsolutePath();
        } else {
            return recentDocumentsPath;
        }
    }

    private void openFile(File file) {
        if (file == null) {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }

        if (isDisallowedFileType(file)) {
            JOptionPane.showMessageDialog(this,
                    "The selected file type is not supported. Please select a text-based file.",
                    "Unsupported File Type", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (Map.Entry<JTextPane, File> entry : filePaths.entrySet()) {
            if (file.equals(entry.getValue())) {
                JTextPane existingPane = entry.getKey();
                UUID groupId = linkedDocuments.get(existingPane);
                createNewTextWindow();
                JTextPane newPane = getCurrentTextPane();
                if (newPane != null) {
                    newPane.setText(existingPane.getText());
                    linkedDocuments.put(newPane, groupId);
                    documentGroups.get(groupId).add(newPane);
                    editorTabbedPane.setTitleAt(editorTabbedPane.getSelectedIndex(), file.getName());
                    filePaths.put(newPane, file);
                }
                return;
            }
        }

        try {
            createNewTextWindow();
            JTextPane newPane = getCurrentTextPane();
            if (newPane != null) {
                if (file.getName().toLowerCase().endsWith(".rtf")) {
                    RTFEditorKit rtfKit = new RTFEditorKit();
                    rtfKit.read(new FileInputStream(file), newPane.getDocument(), 0);
                } else {
                    loadPlainTextFile(file, newPane);
                }

                int currentIndex = editorTabbedPane.getSelectedIndex();
                editorTabbedPane.setTitleAt(currentIndex, file.getName());
                filePaths.put(newPane, file);
            }

            statusBar.setText(" Opened: " + file.getAbsolutePath());
        } catch (IOException | BadLocationException e) {
            JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPlainTextFile(File file, JTextPane textPane) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        textPane.setText(content.toString());
    }

    private boolean isDisallowedFileType(File file) {
        String[] disallowedExtensions = {".png", ".jpg", ".jpeg", ".gif", ".pdf", ".zip", ".exe", ".dmg", ".app"};
        String fileName = file.getName().toLowerCase();

        for (String extension : disallowedExtensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void openFileWithChooser() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            openFile(file);
        }
    }

    private void addToHomePanel(String fileName) {
        JLabel label = new JLabel(fileName, UIManager.getIcon("FileView.fileIcon"), SwingConstants.LEFT);
        homeTabbedPane.addTab(fileName, label);

        if (!homePanel.isVisible() && homeTabbedPane.getTabCount() == 1) {
            toggleHomePanel();
        }
    }

    private void refreshRecentFiles() {
        refreshRecentFiles(new File(recentDocumentsPath));
    }

    public void updateRecentDocumentsPath(String newPath) {
        this.recentDocumentsPath = newPath;
        refreshRecentFiles(new File(newPath));
    }

    private void refreshRecentFiles(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {

                homePanel.removeAll();

                JPanel fileListPanel = new JPanel();
                fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
                fileListPanel.setBackground(new Color(192, 192, 192));

                for (File file : files) {
                    if (file.isFile()) {
                        JPanel fileItemPanel = new JPanel(new BorderLayout());
                        fileItemPanel.setBackground(new Color(220, 220, 220));
                        fileItemPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

                        fileItemPanel.setPreferredSize(new Dimension(fileListPanel.getWidth(), 25));
                        fileItemPanel.setMinimumSize(new Dimension(10, 25));
                        fileItemPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));

                        JLabel fileLabel = new JLabel(file.getName(), UIManager.getIcon("FileView.fileIcon"), SwingConstants.LEFT);
                        fileLabel.setOpaque(true);
                        fileLabel.setBackground(new Color(220, 220, 220));

                        fileItemPanel.add(fileLabel, BorderLayout.CENTER);

                        fileItemPanel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                for (Component comp : fileListPanel.getComponents()) {
                                    if (comp instanceof JPanel) {
                                        comp.setBackground(new Color(220, 220, 220));
                                        for (Component child : ((JPanel) comp).getComponents()) {
                                            if (child instanceof JLabel) {
                                                child.setBackground(new Color(220, 220, 220));
                                            }
                                        }
                                    }
                                }

                                fileItemPanel.setBackground(new Color(184, 207, 229));
                                fileLabel.setBackground(new Color(184, 207, 229));

                                if (e.getClickCount() == 2) {
                                    openFile(file);
                                }
                            }
                        });

                        fileListPanel.add(fileItemPanel);
                        fileListPanel.add(Box.createRigidArea(new Dimension(0, 2)));
                    }
                }

                JScrollPane scrollPane = new JScrollPane(fileListPanel);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                int preferredHeight = Math.min(500, files.length * 90);
                scrollPane.setPreferredSize(new Dimension(mainPanel.getWidth(), preferredHeight));

                scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                homePanel.add(scrollPane, BorderLayout.CENTER);

                homePanel.revalidate();
                homePanel.repaint();
            }
        }
    }

    private void addTerminalShortcut() {
        Action openTerminalAction = new AbstractAction("Open Terminal") {
            @Override
            public void actionPerformed(ActionEvent e) {
                openTerminalWithTip();
            }
        };

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK);
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "openTerminal");
        mainPanel.getActionMap().put("openTerminal", openTerminalAction);
    }

    private void saveFile(boolean saveAs) {
        Component selectedComponent = editorTabbedPane.getSelectedComponent();
        if (selectedComponent instanceof JScrollPane) {
            JTextPane textPane = (JTextPane) ((JScrollPane) selectedComponent).getViewport().getView();
            saveSingleFile(textPane, saveAs);
        } else if (selectedComponent instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) selectedComponent;
            Component leftComponent = splitPane.getLeftComponent();
            Component rightComponent = splitPane.getRightComponent();

            boolean leftSaved = false;
            boolean rightSaved = false;

            if (leftComponent instanceof JScrollPane) {
                JTextPane leftPane = (JTextPane) ((JScrollPane) leftComponent).getViewport().getView();
                leftSaved = saveSingleFile(leftPane, saveAs, false);
            }
            if (rightComponent instanceof JScrollPane) {
                JTextPane rightPane = (JTextPane) ((JScrollPane) rightComponent).getViewport().getView();
                rightSaved = saveSingleFile(rightPane, saveAs, false);
            }

            if (leftSaved || rightSaved) {
                JOptionPane.showMessageDialog(this, "Files saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);

                int tabIndex = editorTabbedPane.getSelectedIndex();
                if (tabIndex != -1) {
                    JTextPane leftPane = (JTextPane) ((JScrollPane) splitPane.getLeftComponent()).getViewport().getView();
                    JTextPane rightPane = (JTextPane) ((JScrollPane) splitPane.getRightComponent()).getViewport().getView();

                    File leftFile = filePaths.get(leftPane);
                    File rightFile = filePaths.get(rightPane);

                    if (leftFile != null && rightFile != null) {
                        editorTabbedPane.setTitleAt(tabIndex, leftFile.getName() + " | " + rightFile.getName());
                    }
                }
            }
        }
    }

    private void createTerminalTab() {
        JPanel terminalPanel = new JPanel(new BorderLayout());
        JTextArea terminalOutput = new JTextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setBackground(Color.BLACK);
        terminalOutput.setForeground(Color.WHITE);
        terminalOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));


        TerminalInputField terminalInput = new TerminalInputField();

        terminalInput.addActionListener(e -> {
            String command = terminalInput.getText().trim();
            terminalInput.setText("");
            terminalOutput.append(">>> " + command + "\n");

            if (command.equalsIgnoreCase("render")) {

                String folderPath = JOptionPane.showInputDialog(
                        this,
                        "Enter folder path:",
                        currentDirectory.getAbsolutePath()
                );
                if (folderPath != null && !folderPath.isEmpty()) {
                    File folder = new File(folderPath);
                    if (folder.exists() && folder.isDirectory()) {
                        updateRecentDocumentsPath(folderPath);
                        terminalOutput.append("Rendered folder: " + folderPath + "\n");
                    } else {
                        terminalOutput.append("Invalid folder path: " + folderPath + "\n");
                    }
                }
            } else if (command.startsWith("cd ")) {

                String newDir = command.substring(3).trim();
                File newDirectory;
                if (newDir.startsWith("/") || newDir.startsWith("\\") || newDir.matches("[A-Za-z]:.*")) {

                    newDirectory = new File(newDir);
                } else {

                    newDirectory = new File(currentDirectory, newDir);
                }

                if (newDirectory.exists() && newDirectory.isDirectory()) {
                    currentDirectory = newDirectory;
                    terminalOutput.append("Changed directory to: " + currentDirectory.getAbsolutePath() + "\n");
                } else {
                    terminalOutput.append("Directory not found: " + newDir + "\n");
                }
            } else if (command.equalsIgnoreCase("pwd")) {

                terminalOutput.append(currentDirectory.getAbsolutePath() + "\n");
            } else {

                String output = executeCommand(command, currentDirectory);
                terminalOutput.append(output);
            }


            terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
        });

        terminalPanel.add(new JScrollPane(terminalOutput), BorderLayout.CENTER);
        terminalPanel.add(terminalInput, BorderLayout.SOUTH);


        editorTabbedPane.addTab("Terminal", terminalPanel);
        editorTabbedPane.setSelectedIndex(editorTabbedPane.getTabCount() - 1);

        terminalPanel.putClientProperty("isTerminal", true);

    }

    private boolean saveSingleFile(JTextPane textPane, boolean saveAs, boolean showPopup) {
        if (textPane != null) {
            File file = filePaths.get(textPane);
            if (file == null || saveAs) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showSaveDialog(this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    file = fileChooser.getSelectedFile();
                    filePaths.put(textPane, file);
                } else {
                    return false;
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textPane.getText());

                refreshRecentFiles();

                if (showPopup && !(textPane.getParent().getParent() instanceof JSplitPane)) {
                    int tabIndex = editorTabbedPane.indexOfComponent(textPane.getParent().getParent());
                    if (tabIndex != -1) {
                        editorTabbedPane.setTitleAt(tabIndex, file.getName());
                    }
                }

                statusBar.setText(" Saved: " + file.getAbsolutePath());

                if (showPopup) {
                    JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
                }

                return true;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    private void saveSingleFile(JTextPane textPane, boolean saveAs) {
        if (textPane != null) {
            File file = filePaths.get(textPane);
            if (file == null || saveAs) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showSaveDialog(this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    file = fileChooser.getSelectedFile();
                    filePaths.put(textPane, file);
                } else {
                    return;
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textPane.getText());
                JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);

                refreshRecentFiles();

                int index = textPanes.indexOf(textPane);
                if (index >= 0) {
                    editorTabbedPane.setTitleAt(index, file.getName());
                    updateStatusBar(textPane);
                }

                statusBar.setText(" Saved: " + file.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadRecentDocuments() {
        File recentDocumentsFolder = new File(recentDocumentsPath);
        if (recentDocumentsFolder.exists() && recentDocumentsFolder.isDirectory()) {
            File[] files = recentDocumentsFolder.listFiles();
            if (files != null) {
                homePanel.removeAll();

                homePanel.setLayout(new BorderLayout());

                JPanel fileListPanel = new JPanel();
                fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
                fileListPanel.setBackground(new Color(192, 192, 192));

                for (File file : files) {
                    if (file.isFile()) {
                        JPanel fileItemPanel = new JPanel(new BorderLayout());
                        fileItemPanel.setBackground(new Color(220, 220, 220));
                        fileItemPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

                        fileItemPanel.setPreferredSize(new Dimension(fileListPanel.getWidth(), 25));
                        fileItemPanel.setMinimumSize(new Dimension(10, 25));
                        fileItemPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));

                        JLabel fileLabel = new JLabel(file.getName(), UIManager.getIcon("FileView.fileIcon"), SwingConstants.LEFT);
                        fileLabel.setOpaque(true);
                        fileLabel.setBackground(new Color(220, 220, 220));

                        fileItemPanel.add(fileLabel, BorderLayout.CENTER);

                        fileItemPanel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                for (Component comp : fileListPanel.getComponents()) {
                                    if (comp instanceof JPanel) {
                                        comp.setBackground(new Color(220, 220, 220));
                                        for (Component child : ((JPanel) comp).getComponents()) {
                                            if (child instanceof JLabel) {
                                                child.setBackground(new Color(220, 220, 220));
                                            }
                                        }
                                    }
                                }

                                fileItemPanel.setBackground(new Color(184, 207, 229));
                                fileLabel.setBackground(new Color(184, 207, 229));

                                if (e.getClickCount() == 2) {
                                    openFile(file);
                                }
                            }
                        });

                        fileListPanel.add(fileItemPanel);
                        fileListPanel.add(Box.createRigidArea(new Dimension(0, 2)));
                    }
                }

                JScrollPane scrollPane = new JScrollPane(fileListPanel);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                int preferredHeight = Math.min(500, files.length * 90);
                scrollPane.setPreferredSize(new Dimension(mainPanel.getWidth(), preferredHeight));

                scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                homePanel.add(scrollPane, BorderLayout.CENTER);

                homePanel.revalidate();
                homePanel.repaint();
            }
        }
    }

    private void toggleHomePanel() {
        homePanel.setVisible(!homePanel.isVisible());

        if (homePanel.isVisible()) {
            refreshRecentFiles(new File(recentDocumentsPath));
            mainPanel.add(homePanel, BorderLayout.SOUTH);
        } else {
            mainPanel.remove(homePanel);
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void addWindowResizeListener() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (homePanel.isVisible()) {
                    int panelHeight = 160;
                    homePanel.setPreferredSize(new Dimension(mainPanel.getWidth(), panelHeight));
                    homePanel.setMinimumSize(new Dimension(mainPanel.getWidth(), panelHeight));
                    homePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight));
                    homePanel.revalidate();
                    homePanel.repaint();
                }
            }
        });
    }

    private void applyStyle(Object styleKey) {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null && textPane.isEditable()) {
            StyledDocument doc = textPane.getStyledDocument();
            int selStart = textPane.getSelectionStart();
            int selEnd = textPane.getSelectionEnd();

            if (selStart != selEnd) {
                MutableAttributeSet attrs = new SimpleAttributeSet();

                if (styleKey.equals(StyleConstants.Bold)) {
                    boolean isBold = StyleConstants.isBold(doc.getCharacterElement(selStart).getAttributes());
                    StyleConstants.setBold(attrs, !isBold);
                    statusBar.setText(" " + (!isBold ? "Applied" : "Removed") + " Bold to selection");
                } else if (styleKey.equals(StyleConstants.Italic)) {
                    boolean isItalic = StyleConstants.isItalic(doc.getCharacterElement(selStart).getAttributes());
                    StyleConstants.setItalic(attrs, !isItalic);
                    statusBar.setText(" " + (!isItalic ? "Applied" : "Removed") + " Italic to selection");
                }

                doc.setCharacterAttributes(selStart, selEnd - selStart, attrs, false);

                textPane.getDocument().addUndoableEditListener(e -> {
                    UndoManager undoManager = (UndoManager) textPane.getClientProperty("undoManager");
                    if (undoManager != null) {
                        undoManager.addEdit(e.getEdit());
                    }
                });
            }
        }
    }

    private void changeFontSize(int delta) {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null && textPane.isEditable()) {
            Font currentFont = textPane.getFont();
            int newSize = currentFont.getSize() + delta;
            if (newSize > 0) {
                Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), newSize);
                textPane.setFont(newFont);
                statusBar.setText(" Zoom: " + newSize);
            }
        }
    }

    private boolean isLeftPane(JTextPane textPane) {
        Component parent = textPane.getParent();
        while (parent != null) {
            if (parent instanceof JSplitPane) {
                JSplitPane splitPane = (JSplitPane) parent;
                return splitPane.getLeftComponent().equals(textPane.getParent());
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void insertImage() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File file) {
                    String filename = file.getName().toLowerCase();
                    return filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                            filename.endsWith(".gif") || filename.endsWith(".png") ||
                            file.isDirectory();
                }
                public String getDescription() {
                    return "Image Files (*.jpg, *.jpeg, *.gif, *.png)";
                }
            });

            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                    if (icon.getIconWidth() > 300) {
                        Image img = icon.getImage();
                        Image scaledImg = img.getScaledInstance(300, -1, Image.SCALE_SMOOTH);
                        icon = new ImageIcon(scaledImg);
                    }
                    textPane.insertIcon(icon);
                    statusBar.setText(" Inserted image: " + file.getName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error inserting image: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void performUndoRedo(boolean isUndo) {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            UndoManager undoManager = (UndoManager) textPane.getClientProperty("undoManager");
            if (undoManager != null) {
                if (isUndo) {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } else {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                }
            }
        }
    }

    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit? Any unsaved changes will be lost.",
                "Exit Draft", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private JTextPane getCurrentTextPane() {
        Component selectedComponent = editorTabbedPane.getSelectedComponent();
        if (selectedComponent instanceof JScrollPane) {
            Component viewportComponent = ((JScrollPane) selectedComponent).getViewport().getView();
            if (viewportComponent instanceof JTextPane) {
                return (JTextPane) viewportComponent;
            }
        } else if (selectedComponent instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) selectedComponent;
            Component leftComponent = splitPane.getLeftComponent();
            Component rightComponent = splitPane.getRightComponent();

            if (leftComponent instanceof JScrollPane) {
                Component leftTextPane = ((JScrollPane) leftComponent).getViewport().getView();
                if (leftTextPane instanceof JTextPane && leftTextPane.hasFocus()) {
                    return (JTextPane) leftTextPane;
                }
            }
            if (rightComponent instanceof JScrollPane) {
                Component rightTextPane = ((JScrollPane) rightComponent).getViewport().getView();
                if (rightTextPane instanceof JTextPane && rightTextPane.hasFocus()) {
                    return (JTextPane) rightTextPane;
                }
            }
        }
        return null;
    }

    private void findText() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            String findText = JOptionPane.showInputDialog(this, "Find:");
            if (findText != null && !findText.isEmpty()) {
                String text = textPane.getText();
                int index = text.indexOf(findText);
                if (index >= 0) {
                    textPane.setCaretPosition(index);
                    textPane.moveCaretPosition(index + findText.length());
                    textPane.requestFocus();
                } else {
                    JOptionPane.showMessageDialog(this, "Text not found.", "Find", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void replaceText() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            String findText = JOptionPane.showInputDialog(this, "Find:");
            if (findText != null && !findText.isEmpty()) {
                String replaceText = JOptionPane.showInputDialog(this, "Replace with:");
                if (replaceText != null) {
                    String text = textPane.getText();
                    text = text.replace(findText, replaceText);
                    textPane.setText(text);
                }
            }
        }
    }

    private void showWordCount() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            String text = textPane.getText();
            int wordCount = text.split("\\s+").length;
            JOptionPane.showMessageDialog(this, "Word Count: " + wordCount, "Word Count", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception ex) {
            }
        }

        JWindow splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createRaisedBevelBorder());
        content.setBackground(new Color(192, 192, 192));
        JLabel splashLabel = new JLabel("Draft v0.0.1", JLabel.CENTER);
        splashLabel.setFont(new Font("Arial", Font.BOLD, 24));
        content.add(splashLabel, BorderLayout.CENTER);
        JLabel copyrightLabel = new JLabel("© 2025 TENSA Solutions", JLabel.CENTER);
        content.add(copyrightLabel, BorderLayout.SOUTH);
        splash.setContentPane(content);
        splash.setSize(300, 150);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        Timer timer = new Timer(1500, e -> {
            splash.dispose();
            new DraftByFalah().setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private JPanel createTerminalPanel() {
        JPanel terminalPanel = new JPanel(new BorderLayout());
        JTextArea terminalOutput = new JTextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setBackground(Color.BLACK);
        terminalOutput.setForeground(Color.WHITE);
        terminalOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JTextField terminalInput = new JTextField();
        terminalInput.setBackground(Color.BLACK);
        terminalInput.setForeground(Color.WHITE);
        terminalInput.setFont(new Font("Monospaced", Font.PLAIN, 12));

        terminalInput.addActionListener(e -> {
            String command = terminalInput.getText().trim();
            terminalInput.setText("");
            terminalOutput.append("> " + command + "\n");

            if (command.equalsIgnoreCase("render")) {

                String folderPath = JOptionPane.showInputDialog(
                        this,
                        "Enter folder path:",
                        currentDirectory.getAbsolutePath()
                );
                if (folderPath != null && !folderPath.isEmpty()) {
                    File folder = new File(folderPath);
                    if (folder.exists() && folder.isDirectory()) {
                        refreshRecentFiles(folder);
                        terminalOutput.append("Rendered folder: " + folderPath + "\n");
                    } else {
                        terminalOutput.append("Invalid folder path: " + folderPath + "\n");
                    }
                }
            } else {
                terminalOutput.append("Unknown command: " + command + "\n");
            }
        });

        terminalPanel.add(new JScrollPane(terminalOutput), BorderLayout.CENTER);
        terminalPanel.add(terminalInput, BorderLayout.SOUTH);

        return terminalPanel;
    }

    public class TerminalInputField extends JTextField {
        private boolean cursorVisible = true;
        private Timer cursorTimer;
        private int promptWidth;

        public TerminalInputField() {

            setFont(new Font("Monospaced", Font.PLAIN, 12));
            setBackground(Color.BLACK);
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);


            FontMetrics fm = getFontMetrics(getFont());
            promptWidth = fm.stringWidth(">>> ");


            setMargin(new Insets(0, promptWidth, 0, 0));


            cursorTimer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cursorVisible = !cursorVisible;
                    repaint();
                }
            });
            cursorTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);


            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.WHITE);
            g2d.drawString(">>>", 5, getHeight() - 5);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            repaint();
        }
    }
}