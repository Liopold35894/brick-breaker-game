import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class BrickBreaker extends JFrame {
    public BrickBreaker() {

        setTitle("Brick Breaker Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) {
        new BrickBreaker();
    }
}

class MusicManager {
    private Clip clip;
    private Clip soundEffectClip;
    private float cachedMusicVolume = 0.5f;
    private float cachedSoundEffectVolume = 0.5f;

    public void setCachedMusicVolume(float volume) {
        this.cachedMusicVolume = volume;
        if (clip != null) {
            setMusicVolume(volume * 100);
        }
    }

    public void setCachedSoundEffectVolume(float volume) {
        this.cachedSoundEffectVolume = volume;
        if (soundEffectClip != null) {
            setSoundEffectVolume(volume * 100);
        }
    }

    public float getCachedMusicVolume() {
        return cachedMusicVolume;
    }

    public float getCachedSoundEffectVolume() {
        return cachedSoundEffectVolume;
    }

    public void playMusic(String soundFileName, boolean loop) {
        try {
            if (clip != null && clip.isRunning()) {
                return;
            }
            File musicFile = new File("C:\\Users\\liopo\\Downloads\\Suno Music\\BrickBreakerMusic\\" + soundFileName);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            setMusicVolume(cachedMusicVolume * 100);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void playSound(String soundFileName) {
        try {
            if (soundEffectClip != null && soundEffectClip.isRunning()) {
                return;
            }
            File musicFile = new File("C:\\Users\\liopo\\Downloads\\Suno Music\\BrickBreakerMusic\\" + soundFileName);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            soundEffectClip = AudioSystem.getClip();
            soundEffectClip.open(audioStream);
            setSoundEffectVolume(cachedSoundEffectVolume * 100);
            soundEffectClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void setMusicVolume(float volumePercentage) {
        if (clip != null) {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = percentageToDecibels(volumePercentage);
            volumeControl.setValue(dB);
        }
    }
    
    public void setSoundEffectVolume(float volumePercentage) {
        if (soundEffectClip != null) {
            FloatControl volumeControl = (FloatControl) soundEffectClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = percentageToDecibels(volumePercentage);
            volumeControl.setValue(dB);
        }
    }
    
    private float percentageToDecibels(float volumePercentage) {
        if (volumePercentage == 0) {
            return -80.0f; // Mute
        }
        return (float) (20.0 * Math.log10(volumePercentage / 100.0));
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private boolean gameOver = false;
    private boolean victory = false;

    public enum GameState {
        MENU, PLAY, PAUSE, SETTINGS
    }
    
    // Game Properties
    private GameState gameState = GameState.MENU;
    private boolean demoMode = true;

    private MusicManager musicManager = new MusicManager();
    private boolean gameOverMusicPlayed = false;
    private boolean victoryMusicPlayed = false;

    private int gameTimeInSeconds = 180; // 3 minutes in seconds
    private Timer gameTimer;

    // Setting Properties
    private JSlider musicSlider;
    private JSlider soundEffectSlider;
    private JButton backButton = null;

    // Button Properties
    private Rectangle startButton = new Rectangle(280, 300, 200, 50);
    private Rectangle settingsButton = new Rectangle(280, 400, 200, 50);
    private Rectangle menuButton = new Rectangle(310, 320, 160, 40);
    private boolean highlightStartButton = false;
    private boolean highlightSettingsButton = false;

    // Paddle Properties
    private int paddleX = 350; 
    private final int paddleY = 550;
    private final int paddleWidth = 100;
    private final int paddleHeight = 5;
    private final int paddleSpeed = 20;

    // Ball Properties
    private int ballX = 400, ballY = 300;
    private final int ballDiameter = 10;
    private int ballXDir = -2, ballYDir = -3;

    // Brick Properties
    private final int rows = 5;
    private final int cols = 8;
    private final int brickWidth = 70;
    private final int brickHeight = 20;
    private final int brickPadding = 10;
    private final int brickOffsetX = 35;
    private final int brickOffsetY = 30;
    private List<Rectangle> bricks = new ArrayList<>();

    // other
    private int fadeOpacity = 0;

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();
    
                if (gameState == GameState.MENU) {
                    if (startButton.contains(mouseX, mouseY)) {
                        startGame();  // Start the game
                    } else if (settingsButton.contains(mouseX, mouseY)) {
                        gameState = GameState.SETTINGS;  // Open settings menu
                        musicManager.stopMusic();
                    }
                    repaint();
                } else if (gameState == GameState.SETTINGS) {
                    Rectangle backButton = new Rectangle(150, 300, 200, 50);
                    if (backButton.contains(mouseX, mouseY)) {
                        gameState = GameState.MENU;  // Go back to the main menu
                        repaint();
                    }
                } else if (gameState == GameState.PLAY) {
                    if ((gameOver || victory) && menuButton.contains(mouseX, mouseY)) {
                        musicManager.stopMusic();
                        gameOverMusicPlayed = false;
                        victoryMusicPlayed = false;
                        timer = null;
                        resetGame();
                    }
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();
        
                // Check if hovering over Start or Settings button
                highlightStartButton = startButton.contains(mouseX, mouseY);
                highlightSettingsButton = settingsButton.contains(mouseX, mouseY);
                repaint(); // Ensure repaint is triggered
            }
        });

        initializeBricks();
        startDemoMode();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw gradient background
        GradientPaint gradient = new GradientPaint(0, 0, Color.DARK_GRAY, 0, getHeight(), Color.BLACK);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw background gameplay
        if (gameState == GameState.MENU && demoMode) {
            drawPaddle(g2d);
            drawBall(g2d);
            drawBricks(g2d);
        }

        if (gameState == GameState.MENU) {
            drawMenu(g);
        } else if (gameState == GameState.PLAY) {
            if (gameOver) {
                drawGameOver(g);
            } else if (victory) {
                drawVictory(g);
            } else {
                drawPaddle(g);
                drawBall(g);
                drawBricks(g);
                drawInGameTimer(g);
            }
        } else if (gameState == GameState.SETTINGS) {
            drawSettings(g);
        } else if (gameState == GameState.PAUSE) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("Game Paused", getWidth() / 2 - 150, getHeight() / 2);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press ESC to Resume", getWidth() / 2 - 100, getHeight() / 2 + 40);
        }
    }

    private void initializeGameTimer() {
        gameTimer = new Timer(1000, e -> {
            if (gameTimeInSeconds > 0 && !gameOver && !victory) {
                gameTimeInSeconds--;
            } else {
                gameTimer.stop();
            }
            repaint();
        });
        gameTimer.start();
    }

    private void initializeBricks() {
        bricks.clear(); // Clear any existing bricks
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int brickX = j * (brickWidth + brickPadding) + brickOffsetX;
                int brickY = i * (brickHeight + brickPadding) + brickOffsetY;
                bricks.add(new Rectangle(brickX, brickY, brickWidth, brickHeight));
            }
        }
    }

    private void startDemoMode() {
        if (timer == null) {
            timer = new Timer(10, this);
        }
        timer.start();
    }

    private void resetGame() {
        gameState = GameState.MENU;
        demoMode = true;
        gameOver = false;
        victory = false;
        gameTimeInSeconds = 180; // reset game time to 3 minutes
        ballX = 400;
        ballY = 300;
        ballXDir = -2;
        ballYDir = -3;
        paddleX = 350;
        fadeOpacity = 0;

        initializeBricks();
        startDemoMode();
        repaint();
    }

    private void drawGameOver(Graphics g) {
        if (!gameOverMusicPlayed) {
            musicManager.stopMusic();
            musicManager.playMusic("gameOver.wav", true);
            gameOverMusicPlayed = true;
        }

        Graphics2D g2d = (Graphics2D) g;

        // Background fade effect
        g2d.setColor(new Color(0, 0, 0, fadeOpacity));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);
        g.fillRect(getWidth() / 2 - 105, getHeight() / 2 - 50, 205, 150);
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Game Over", getWidth() / 2 - 100, getHeight() / 2);

        g.setColor(Color.GRAY);
        g.fillRect(menuButton.x, menuButton.y, menuButton.width, menuButton.height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString("Return to Menu", menuButton.x + 22, menuButton.y + 28);
    }

    private void drawVictory(Graphics g) {
        if (!victoryMusicPlayed) {
            musicManager.stopMusic();
            musicManager.playMusic("victory.wav", true);
            victoryMusicPlayed = true;
        }

        Graphics2D g2d = (Graphics2D) g;

        // Background fade effect
        g2d.setColor(new Color(0, 0, 0, fadeOpacity));
        g2d.fillRect(0, 0, getWidth(), getHeight());


        g.setColor(Color.WHITE);
        g.fillRect(getWidth() / 2 - 105, getHeight() / 2 - 50, 220, 150);
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("You Win!", getWidth() / 2 - 70, getHeight() / 2);

        g.setColor(Color.GRAY);
        g.fillRect(menuButton.x, menuButton.y, menuButton.width, menuButton.height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString("Return to Menu", menuButton.x + 22, menuButton.y + 28);
    }

    private void startGame() {
        gameState = GameState.PLAY;
        demoMode = false;
        ballX = 400;
        ballY = 300;
        ballXDir = -2;
        ballYDir = -3;
        paddleX = 350;
    
        // Initialize bricks
        initializeBricks();
        // Initialize count down timer
        initializeGameTimer(); 
    
        // Initialize or start the timer
        if (timer == null) {
            timer = new Timer(10, this);
        }
        timer.start();
        musicManager.stopMusic();
        musicManager.playMusic("stageMusic1.wav", true);
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Brick Breaker", 270, 250);
    
        // Draw Start Game button
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        if (highlightStartButton) {
            g.setColor(Color.LIGHT_GRAY);
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(startButton.x, startButton.y, startButton.width, startButton.height);
        g.setColor(Color.BLACK);
        g.drawString("Start Game", startButton.x + 45, startButton.y + 35);
    
        // Draw Settings button
        if (highlightSettingsButton) {
            g.setColor(Color.LIGHT_GRAY);
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(settingsButton.x, settingsButton.y, settingsButton.width, settingsButton.height);
        g.setColor(Color.BLACK);
        g.drawString("Settings", settingsButton.x + 60, settingsButton.y + 35);

        if (gameState == GameState.MENU) {
            musicManager.playMusic("menuMusic1.wav", true);
        }
    }

    private void drawSettings(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Settings", 310, 100);

        // Music and sound effect labels
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Music Volume:", 130, 180);
        g.drawString("Sound Volume:", 130, 230);

        // Music and sound effect volume slider
        if (musicSlider == null || !isAncestorOf(musicSlider)) {
            musicSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
            musicSlider.setBounds(300, 150, 300, 45);
            musicSlider.setMajorTickSpacing(10);
            musicSlider.setPaintTicks(true);
            musicSlider.setPaintLabels(true);
            musicSlider.setValue((int) (musicManager.getCachedMusicVolume() * 100));
            musicSlider.addChangeListener(e -> {
                float sliderValue = musicSlider.getValue() / 100.0f;
                musicManager.setCachedMusicVolume(sliderValue);
            });
            add(musicSlider);
        }

        if (soundEffectSlider == null || !isAncestorOf(soundEffectSlider)) {
            soundEffectSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
            soundEffectSlider.setBounds(300, 200, 300, 45);
            soundEffectSlider.setMajorTickSpacing(10);
            soundEffectSlider.setPaintTicks(true);
            soundEffectSlider.setPaintLabels(true);
            soundEffectSlider.setValue((int) (musicManager.getCachedSoundEffectVolume() * 100));
            soundEffectSlider.addChangeListener(e -> {
                float sliderValue = soundEffectSlider.getValue() / 100.0f;
                musicManager.setCachedSoundEffectVolume(sliderValue);
            });
            add(soundEffectSlider);
        }
    
        // "Back" button to return to the main menu
        if (backButton == null || !isAncestorOf(backButton)) {
            backButton = new JButton("Back");
            backButton.setBounds(305, 480, 150, 50);
            backButton.addActionListener(e -> {
                gameState = GameState.MENU;
                remove(musicSlider);
                remove(soundEffectSlider);
                remove(backButton);
                repaint();
            });
            add(backButton);
        }
    }

    private void drawInGameTimer(Graphics g) {
        if (gameTimeInSeconds > 10) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.RED);
        }
        g.setFont(new Font("Arial", Font.BOLD, 24));
        int minutes = gameTimeInSeconds / 60;
        int seconds = gameTimeInSeconds % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        g.drawString("Time: " + timeString, getWidth() - 140, 25);
    }

    private void drawPaddle(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Outer glow
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillRoundRect(paddleX - 5, paddleY - 5, paddleWidth + 10, paddleHeight + 10, 10, 10);

        // Paddle body
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(paddleX, paddleY, paddleWidth, paddleHeight, 10, 10);
    }

    private void drawBall(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        //shadow
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillOval(ballX + 3, ballY + 3, ballDiameter, ballDiameter);

        // Ball body
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ballX, ballY, ballDiameter, ballDiameter);
    }

    private void drawBricks(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.WHITE);
        for (Rectangle brick : bricks) {
            g2d.fillRect(brick.x, brick.y, brick.width, brick.height);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.MENU && demoMode) {
            // Move the ball
            ballX += ballXDir;
            ballY += ballYDir;
    
            // Ball-wall collision
            if (ballX <= 0 || ballX >= getWidth() - ballDiameter) {
                ballXDir = -ballXDir;
            }
            if (ballY <= 0) {
                ballYDir = -ballYDir;
            }

            // check if ball hits paddle
            if (ballY + ballDiameter >= paddleY && ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                ballYDir = -ballYDir;
                // Calculate horizontal direction based on collision point
                int paddleCenter = paddleX + paddleWidth / 2;
                int ballCenter = ballX + ballDiameter / 2;

                // Offset from the paddle's center
                int offset = ballCenter - paddleCenter;

                // Normalize the offset to a range of [-1, 1]
                double normalizedOffset = (double) offset / (paddleWidth / 2);

                // Adjust horizontal speed and direction
                ballXDir = (int) (normalizedOffset * 4); // Multiplier adjusts angle steepness
                if (ballXDir == 0) {
                    ballXDir = (Math.random() > 0.5) ? 1 : -1; // Prevent ball from moving straight up
                }
            }
    
            // Simulate paddle following the ball
            paddleX = Math.max(0, Math.min(ballX - paddleWidth / 2, getWidth() - paddleWidth));
    
            // Ball-brick collision
            Iterator<Rectangle> iterator = bricks.iterator();
            while (iterator.hasNext()) {
                Rectangle brick = iterator.next();
                if (new Rectangle(ballX, ballY, ballDiameter, ballDiameter).intersects(brick)) {
                    iterator.remove();
                    ballYDir = -ballYDir;
                    break;
                }
            }
    
            // Restart the demo if all bricks are broken
            if (bricks.isEmpty() || ballY > getHeight()) {
                initializeBricks();
                ballX = 400;
                ballY = 300;
                ballXDir = -2;
                ballYDir = -3;
            }
    
            repaint();
        } else if (gameState == GameState.PLAY) {
            if (!gameOver && !victory) {
                ballX += ballXDir;
                ballY += ballYDir;
    
                // bounce ball off walls
                if (ballX <= 0 || ballX >= getWidth() - ballDiameter) {
                    ballXDir = -ballXDir;
                }
    
                if (ballY <= 0) {
                    ballYDir = -ballYDir;
                }
                
                // check if ball hits paddle
                if (ballY + ballDiameter >= paddleY && ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                    musicManager.playSound("paddleHit.wav");
                    ballYDir = -ballYDir;
                    // Calculate horizontal direction based on collision point
                    int paddleCenter = paddleX + paddleWidth / 2;
                    int ballCenter = ballX + ballDiameter / 2;

                    // Offset from the paddle's center
                    int offset = ballCenter - paddleCenter;

                    // Normalize the offset to a range of [-1, 1]
                    double normalizedOffset = (double) offset / (paddleWidth / 2);

                    // Adjust horizontal speed and direction
                    ballXDir = (int) (normalizedOffset * 4); // Multiplier adjusts angle steepness
                    if (ballXDir == 0) {
                        ballXDir = (Math.random() > 0.5) ? 1 : -1; // Prevent ball from moving straight up
                    }
                }

                // Prevent the ball for going too fast and going through the walls and paddle
                Rectangle ballBounds = new Rectangle(ballX, ballY, ballDiameter, ballDiameter);
                Rectangle paddleBounds = new Rectangle(paddleX, paddleY, paddleWidth, paddleHeight);
                if (ballBounds.intersects(paddleBounds)) {
                    ballYDir = -ballYDir;
                }
    
                // Check for collision with bricks
                Iterator<Rectangle> iterator = bricks.iterator();
                while (iterator.hasNext()) {
                    Rectangle brick = iterator.next();
                    if (new Rectangle(ballX, ballY, ballDiameter, ballDiameter).intersects(brick)) {
                        musicManager.playSound("brickBreak.wav");
                        iterator.remove(); // Remove the brick from the list
                        ballYDir = -ballYDir; // Reverse ball direction
                        break; // Only handle one collision per frame
                    }
                }
    
                // GameOver if ball goes below the paddle
                if (ballY > getHeight() || gameTimeInSeconds == 0) {
                    gameOver = true;
                    fadeOpacity = Math.min(fadeOpacity + 5, 255);
                    timer.stop();
                } else {
                    fadeOpacity = 0;
                }
    
                // Victory if all bricks are broken
                if (bricks.isEmpty()) {
                    victory = true;
                    fadeOpacity = Math.min(fadeOpacity + 5, 255);
                    timer.stop();
                } else {
                    fadeOpacity = 0;
                }
    
                repaint();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameState == GameState.PLAY) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (paddleX - paddleSpeed >= 0) {
                    paddleX -= paddleSpeed;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (paddleX + paddleWidth + paddleSpeed <= getWidth()) {
                    paddleX += paddleSpeed;
                }
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.PAUSE;
                timer.stop();
                repaint();
            }
    
            repaint();
        } else if (gameState == GameState.PAUSE) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.PLAY;
                timer.start();
                repaint();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}