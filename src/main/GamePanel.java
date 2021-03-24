package main;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

public class GamePanel extends JPanel implements Runnable {

    final static int TILE = 80, WIDTH = TILE * 16, HEIGHT = TILE * 12, BOYHEIGHT = (TILE * 500) / 300, BOYDISTANCE = TILE * 7, BOYSTARTY = TILE * 11 - BOYHEIGHT, FPS = 1000 / 60;
    final static float VOLUME = 0.01f;
    static int boyX, boyY, aniCounter, boyIdleCounter, boyWalkCounter, boyRunCounter, boyJumpCounter, boyFallCounter, boyDeadCounter, enemyWalkCounter, backgroundCounter, backgroundX, enemyCounter, score;
    boolean running, walk, run, jump, dead, fall, isHeadingRight, pause;
    Image[] boyIdle = new Image[15];
    Image[] boyWalk = new Image[15];
    Image[] boyRun = new Image[15];
    Image[] boyJump = new Image[9];
    Image[] boyFall = new Image[7];
    Image[] boyDead = new Image[15];
    Image[] enemyWalk = new Image[2];
    Image[] tiles = new Image[19];
    Image[] gold = new Image[6];
    Image[] silver = new Image[6];
    Image[] bronze = new Image[6];
    Image[] backgrounds = new Image[9];
    Enemy[] enemies = new Enemy[4];
    Font font, scoreFont;
    Thread gameLoop, musicLoop, noiseLoop;
    Rectangle player = new Rectangle();
    boolean[] keysPressed = new boolean[3];
    int[][] grid = new int[1000][HEIGHT / TILE];

    public GamePanel() {

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setFocusable(true);
        this.requestFocus();
        this.setBackground(new Color(135, 206, 235));

        reset();
        loadResources(this);

        InputMap[] inputMaps = new InputMap[]{
                this.getInputMap(JComponent.WHEN_FOCUSED),
                this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT),
                this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
        };
        createInputMap(inputMaps);
        this.getActionMap().put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[0] = true;
                }
            }
        });
        this.getActionMap().put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[1] = true;
                }
            }
        });
        this.getActionMap().put("shift right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[0] = true;
                    keysPressed[2] = true;
                }
            }
        });
        this.getActionMap().put("shift left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[1] = true;
                    keysPressed[2] = true;
                }
            }
        });
        this.getActionMap().put("release right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[0] = false;
                }
            }
        });
        this.getActionMap().put("release left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[1] = false;
                }
            }
        });
        this.getActionMap().put("release shift right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[0] = false;
                }
            }
        });
        this.getActionMap().put("release shift left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[1] = false;
                }
            }
        });
        this.getActionMap().put("shift", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[2] = true;
                }
            }
        });
        this.getActionMap().put("release shift", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead) {
                    keysPressed[2] = false;
                }
            }
        });
        this.getActionMap().put("jump", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!dead && !pause && grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT) / TILE] > 10 && grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT) / TILE] < 18) {
                    playSound("jump");
                    jump = true;
                }
            }
        });
        this.getActionMap().put("pause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                pause = !pause;
                if (!pause) {
                    initThreads();
                }
                repaint();
            }
        });
        if (!running) {
            initThreads();
        }
    }

    public static void playSound(String name) {

        try {
            URL url = Platformer.class.getResource("/res/sounds/" + name + ".wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(20f * (float) Math.log10(VOLUME));
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    private void createInputMap(InputMap[] inputMaps) {
        for (InputMap i : inputMaps) {
            i.put(KeyStroke.getKeyStroke("RIGHT"), "right");
            i.put(KeyStroke.getKeyStroke("LEFT"), "left");
            i.put(KeyStroke.getKeyStroke("shift RIGHT"), "shift right");
            i.put(KeyStroke.getKeyStroke("shift LEFT"), "shift left");
            i.put(KeyStroke.getKeyStroke("released RIGHT"), "release right");
            i.put(KeyStroke.getKeyStroke("released LEFT"), "release left");
            i.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK, true), "release shift right");
            i.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK, true), "release shift left");
            i.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK, false), "shift");
            i.put(KeyStroke.getKeyStroke("released SHIFT"), "release shift");
            i.put(KeyStroke.getKeyStroke("SPACE"), "jump");
            i.put(KeyStroke.getKeyStroke("PAUSE"), "pause");
        }
    }

    private void initThreads() {
        musicLoop = new Music();
        gameLoop = new Thread(this);

        gameLoop.start();
        musicLoop.start();
        running = true;
    }

    private void reset() {
        running = false;
        walk = false;
        run = false;
        jump = false;
        dead = false;
        fall = false;
        pause = false;
        isHeadingRight = true;
        boyX = BOYDISTANCE + TILE * 6;
        boyY = BOYSTARTY;
        aniCounter = 0;
        boyIdleCounter = 0;
        boyWalkCounter = 0;
        boyRunCounter = 0;
        boyJumpCounter = 0;
        boyFallCounter = 0;
        boyDeadCounter = 0;
        enemyWalkCounter = 0;
        backgroundCounter = 0;
        backgroundX = 0;
        enemyCounter = 0;
        score = 0;
        Arrays.fill(keysPressed, false);
        player.setBounds(boyX, boyY, TILE / 2, (int) (BOYHEIGHT - (TILE * 0.33)));
        loadMap();
        initEnemies();
    }

    private void loadMap() {

        BufferedReader bufferedReader = null;

        try {
            InputStream input = getClass().getResourceAsStream("/res/maps/map.txt");
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String row;
            int i = 0;

            while ((row = bufferedReader.readLine()) != null) {
                char[] chars = row.toCharArray();
                int decimal;
                int unity;
                int k = 0;

                for (int j = 0; j < chars.length / 2; j++) {
                    decimal = Character.getNumericValue(chars[k]) * 10;
                    unity = Character.getNumericValue(chars[k + 1]);
                    grid[j][i] = decimal + unity;
                    k += 2;
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void initEnemies() {
        enemies[0] = new Enemy(0, 136, 3, 0, 0, TILE / 2, TILE / 3);
        enemies[1] = new Enemy(1, 189, 6, 0, 0, TILE / 2, TILE / 3);
        enemies[2] = new Enemy(2, 11, 12, 0, 0, TILE, TILE);
        enemies[3] = new Enemy(1, 10, 10, 0, 0, TILE / 2, TILE / 3);
    }

    private void loadResources(GamePanel gamePanel) {
        ResourceLoader loader = new ResourceLoader(gamePanel);
        loader.loadAll();
    }

    private void move() {

        if (!dead) {
            for (int i = 0; i < enemies.length; i++) {
                if (player.intersects(enemies[i])) {
                    playSound("death");
                    dead = true;
                    return;
                }
            }
            if (jump) {
                if (boyY >= TILE / 8 && (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] < 11 || grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] > 18)) {
                    boyY -= TILE / 16;
                }
                fall = false;
            } else if (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT) / TILE] < 11 || grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT) / TILE] > 18) {
                boyY += TILE / 16;
                if (boyY + BOYHEIGHT == HEIGHT) {
                    playSound("fall");
                    reset();
                }
                fall = true;
            } else {
                fall = false;
            }
            if (keysPressed[0]) {
                if (keysPressed[2]) {
                    if ((grid[(boyX + TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] < 11 || grid[(boyX + TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] > 18) && (grid[(boyX + TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] < 11 || grid[(boyX + TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] > 18)) {
                        boyX += TILE / 32;
                        if (boyX / TILE + 10 > 16) {
                            backgroundX -= TILE / 24;
                        }
                    }
                } else {
                    if ((grid[(boyX + TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] < 11 || grid[(boyX + TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] > 18) && (grid[(boyX + TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] < 11 || grid[(boyX + TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] > 18)) {
                        boyX += TILE / 16;
                        if (boyX / TILE + 10 > 16) {
                            backgroundX -= TILE / 12;
                        }
                    }
                }
            }
            if (keysPressed[1]) {
                if (keysPressed[2]) {
                    if ((grid[(boyX - TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] < 11 || grid[(boyX - TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] > 18) && (grid[(boyX - TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] < 11 || grid[(boyX - TILE / 16 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] > 18)) {
                        boyX -= TILE / 32;
                        if (boyX / TILE + 10 > 16) {
                            backgroundX += TILE / 24;
                        }
                    }
                } else {
                    if ((grid[(boyX - TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] < 11 || grid[(boyX - TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - 1) / TILE] > 18) && (grid[(boyX - TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] < 11 || grid[(boyX - TILE / 8 + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE + 10) / TILE] > 18)) {
                        boyX -= TILE / 16;
                        if (boyX / TILE + 10 > 16) {
                            backgroundX += TILE / 12;
                        }
                    }
                }
            }
            if (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] == 1) {
                playSound("death");
                dead = true;
            }
            if (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] == 19) {
                playSound("coin");
                grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] = 0;
                score += 10;
            }
            if (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] == 20) {
                playSound("coin");
                grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] = 0;
                score += 5;
            }
            if (grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] == 21) {
                playSound("coin");
                grid[(boyX + TILE / 2) / TILE][(boyY + BOYHEIGHT - TILE) / TILE] = 0;
                score++;
            }
        }
    }

    private Image boyIdle() {
        boyWalkCounter = 0;
        boyRunCounter = 0;
        boyJumpCounter = 0;
        boyFallCounter = 0;
        boyDeadCounter = 0;
        if (aniCounter % 5 == 0) {
            boyIdleCounter++;
        }
        return boyIdle[boyIdleCounter % boyIdle.length];
    }

    private Image boyWalk() {
        boyIdleCounter = 0;
        boyRunCounter = 0;
        boyJumpCounter = 0;
        boyFallCounter = 0;
        boyDeadCounter = 0;
        if (aniCounter % 3 == 0) {
            boyWalkCounter++;
        }
        return boyWalk[boyWalkCounter % boyWalk.length];
    }

    private Image boyRun() {
        boyIdleCounter = 0;
        boyWalkCounter = 0;
        boyJumpCounter = 0;
        boyFallCounter = 0;
        boyDeadCounter = 0;
        if (aniCounter % 3 == 0) {
            boyRunCounter++;
        }
        return boyRun[boyRunCounter % boyRun.length];
    }

    private Image boyJump() {
        boyIdleCounter = 0;
        boyWalkCounter = 0;
        boyRunCounter = 0;
        boyFallCounter = 0;
        boyDeadCounter = 0;
        if (aniCounter % 3 == 0) {
            boyJumpCounter++;
        }
        return boyJump[boyJumpCounter % boyJump.length];
    }

    private Image boyFall() {
        boyIdleCounter = 0;
        boyWalkCounter = 0;
        boyRunCounter = 0;
        boyJumpCounter = 0;
        boyDeadCounter = 0;
        if (aniCounter % 3 == 0) {
            boyFallCounter++;
        }
        return boyFall[boyFallCounter % boyFall.length];
    }

    private Image boyDie() {
        boyIdleCounter = 0;
        boyWalkCounter = 0;
        boyRunCounter = 0;
        boyJumpCounter = 0;
        boyFallCounter = 0;
        if (aniCounter % 3 == 0) {
            boyDeadCounter++;
            if (boyDeadCounter == boyDead.length) {
                reset();
            }
        }
        return boyDead[boyDeadCounter % boyDead.length];
    }

    private Image enemyWalk() {
        if (aniCounter % 20 == 0) {
            enemyWalkCounter++;
        }
        return enemyWalk[enemyWalkCounter % enemyWalk.length];
    }

    private Image gold() {
        return gold[(aniCounter / 6) % gold.length];
    }

    private Image silver() {
        return silver[(aniCounter / 6) % silver.length];
    }

    private Image bronze() {
        return bronze[(aniCounter / 6) % bronze.length];
    }

    private void drawBackground(Graphics2D g) {

        g.drawImage(backgrounds[6], (backgroundCounter / 2) % (TILE * 12) - TILE * 24, (int) (TILE * 2.8), null);
        g.drawImage(backgrounds[6], (backgroundCounter / 2) % (TILE * 12) - TILE * 12, (int) (TILE * 2.8), null);
        g.drawImage(backgrounds[6], (backgroundCounter / 2) % (TILE * 12), (int) (TILE * 2.8), null);
        g.drawImage(backgrounds[6], ((backgroundCounter / 2) % (TILE * 12)) + TILE * 12, (int) (TILE * 2.8), null);
        g.drawImage(backgrounds[6], ((backgroundCounter / 2) % (TILE * 12)) + TILE * 24, (int) (TILE * 2.8), null);

        g.drawImage(backgrounds[5], backgroundCounter % (TILE * 12) - TILE * 24, (int) (TILE * 3.8), null);
        g.drawImage(backgrounds[5], backgroundCounter % (TILE * 12) - TILE * 12, (int) (TILE * 3.8), null);
        g.drawImage(backgrounds[5], backgroundCounter % (TILE * 12), (int) (TILE * 3.8), null);
        g.drawImage(backgrounds[5], (backgroundCounter % (TILE * 12)) + TILE * 12, (int) (TILE * 3.8), null);
        g.drawImage(backgrounds[5], (backgroundCounter % (TILE * 12)) + TILE * 24, (int) (TILE * 3.8), null);

        g.drawImage(backgrounds[4], backgroundCounter % (TILE * 12) - TILE * 24, (int) (TILE * 7.2), null);
        g.drawImage(backgrounds[4], backgroundCounter % (TILE * 12) - TILE * 12, (int) (TILE * 7.2), null);
        g.drawImage(backgrounds[4], backgroundCounter % (TILE * 12), (int) (TILE * 7.2), null);
        g.drawImage(backgrounds[4], (backgroundCounter % (TILE * 12)) + TILE * 12, (int) (TILE * 7.2), null);
        g.drawImage(backgrounds[4], (backgroundCounter % (TILE * 12)) + TILE * 24, (int) (TILE * 7.2), null);

        g.drawImage(backgrounds[0], backgroundX % (TILE * 12) - TILE * 24, (int) (TILE * 7.8), null);
        g.drawImage(backgrounds[0], backgroundX % (TILE * 12) - TILE * 12, (int) (TILE * 7.8), null);
        g.drawImage(backgrounds[0], backgroundX % (TILE * 12), (int) (TILE * 7.8), null);
        g.drawImage(backgrounds[0], (backgroundX % (TILE * 12)) + TILE * 12, (int) (TILE * 7.8), null);
        g.drawImage(backgrounds[0], (backgroundX % (TILE * 12)) + TILE * 24, (int) (TILE * 7.8), null);
    }

//    private void drawObstacles(Graphics2D g) {
//
//        int posX = 10;
//        int posY = 10;
//
//        if (boyX / TILE + 10 <= 16) {
//            if (enemyCounter > TILE * 6) {
//                g.drawImage(tiles[18], TILE + TILE * (posX - 8), TILE * 12 - enemyCounter + TILE * (posY - 8), null);
//            } else {
//                g.drawImage(tiles[18], TILE + TILE * (posX - 8), enemyCounter + TILE * (posY - 8), null);
//            }
//        } else {
//            if (enemyCounter > TILE * 6) {
//                g.drawImage(tiles[18], TILE * posX - boyX, TILE * 12 - enemyCounter + TILE * (posY - 8), null);
//            } else {
//                g.drawImage(tiles[18], TILE * posX - boyX, enemyCounter + TILE * (posY - 8), null);
//            }
//        }
//    }

    private void drawMap(Graphics2D g) {

        if (boyX / TILE + 10 <= 16) {
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    switch (grid[i][j]) {
                        case 1:
                            g.drawImage(tiles[0], TILE * i, TILE * j, null);
                            break;
                        case 2:
                            g.drawImage(tiles[1], TILE * i, TILE * j, null);
                            break;
                        case 3:
                            g.drawImage(tiles[2], TILE * i, TILE * j, null);
                            break;
                        case 4:
                            g.drawImage(tiles[3], TILE * i, TILE * j, null);
                            break;
                        case 5:
                            g.drawImage(tiles[4], TILE * i, TILE * j, null);
                            break;
                        case 6:
                            g.drawImage(tiles[5], TILE * i, TILE * j, null);
                            break;
                        case 7:
                            g.drawImage(tiles[6], TILE * i, TILE * j, null);
                            break;
                        case 8:
                            g.drawImage(tiles[7], TILE * i, TILE * j, null);
                            break;
                        case 9:
                            g.drawImage(tiles[8], TILE * i, TILE * j, null);
                            break;
                        case 10:
                            g.drawImage(tiles[9], TILE * i, TILE * j, null);
                            break;
                        case 11:
                            g.drawImage(tiles[10], TILE * i, TILE * j, null);
                            break;
                        case 12:
                            g.drawImage(tiles[11], TILE * i, TILE * j, null);
                            break;
                        case 13:
                            g.drawImage(tiles[12], TILE * i, TILE * j, null);
                            break;
                        case 14:
                            g.drawImage(tiles[13], TILE * i, TILE * j, null);
                            break;
                        case 15:
                            g.drawImage(tiles[14], TILE * i, TILE * j, null);
                            break;
                        case 16:
                            g.drawImage(tiles[15], TILE * i, TILE * j, null);
                            break;
                        case 17:
                            g.drawImage(tiles[16], TILE * i, TILE * j, null);
                            break;
                        case 18:
                            g.drawImage(tiles[17], TILE * i, TILE * j, null);
                            break;
                        case 19:
                            g.drawImage(gold(), TILE * i + TILE / 4, TILE * j + TILE / 4, null);
                            break;
                        case 20:
                            g.drawImage(silver(), TILE * i + TILE / 4, TILE * j + TILE / 4, null);
                            break;
                        case 21:
                            g.drawImage(bronze(), TILE * i + TILE / 4, TILE * j + TILE / 4, null);
                    }
                }
            }
        } else {
            for (int i = boyX / TILE - 7; i < boyX / TILE + 10; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    switch (grid[i][j]) {
                        case 1:
                            g.drawImage(tiles[0], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 2:
                            g.drawImage(tiles[1], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 3:
                            g.drawImage(tiles[2], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 4:
                            g.drawImage(tiles[3], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 5:
                            g.drawImage(tiles[4], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 6:
                            g.drawImage(tiles[5], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 7:
                            g.drawImage(tiles[6], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 8:
                            g.drawImage(tiles[7], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 9:
                            g.drawImage(tiles[8], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 10:
                            g.drawImage(tiles[9], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 11:
                            g.drawImage(tiles[10], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 12:
                            g.drawImage(tiles[11], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 13:
                            g.drawImage(tiles[12], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 14:
                            g.drawImage(tiles[13], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 15:
                            g.drawImage(tiles[14], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 16:
                            g.drawImage(tiles[15], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 17:
                            g.drawImage(tiles[16], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 18:
                            g.drawImage(tiles[17], TILE * i - boyX + BOYDISTANCE, TILE * j, null);
                            break;
                        case 19:
                            g.drawImage(gold(), TILE * i - boyX + BOYDISTANCE + TILE / 4, TILE * j + TILE / 4, null);
                            break;
                        case 20:
                            g.drawImage(silver(), TILE * i - boyX + BOYDISTANCE + TILE / 4, TILE * j + TILE / 4, null);
                            break;
                        case 21:
                            g.drawImage(bronze(), TILE * i - boyX + BOYDISTANCE + TILE / 4, TILE * j + TILE / 4, null);
                    }
                }
            }
        }
    }

    private void drawEnemies(Graphics2D g) {

        for (int i = 0; i < enemies.length; i++) {

            int posX = enemies[i].posX;
            int posY = enemies[i].posY;

            switch (enemies[i].code) {
                case 0:
                case 1:
                    if (boyX / TILE + 10 <= 16) {
                        if (enemyCounter > TILE * 6) {
                            g.drawImage(enemyWalk(), TILE * 12 - enemyCounter + TILE + TILE * (posX - 8), TILE * posY, null);
                            enemies[i].setLocation(TILE * 12 - enemyCounter + TILE + TILE * (posX - 8) + TILE / 4, TILE * posY + (TILE * 2 / 3));
                        } else {
                            g.drawImage(enemyWalk(), enemyCounter + TILE + TILE + TILE * (posX - 8), TILE * posY, -TILE, TILE, null);
                            enemies[i].setLocation(enemyCounter + TILE + TILE * (posX - 8) + TILE / 4, TILE * posY + (TILE * 2 / 3));
                        }
                    } else {
                        if (enemyCounter > TILE * 6) {
                            g.drawImage(enemyWalk(), TILE * 12 - enemyCounter - boyX + TILE * posX, TILE * posY, null);
                            enemies[i].setLocation(TILE * 12 - enemyCounter - boyX + TILE * posX + TILE / 4, TILE * posY + (TILE * 2 / 3));
                        } else {
                            g.drawImage(enemyWalk(), enemyCounter - boyX + TILE * posX + TILE, TILE * posY, -TILE, TILE, null);
                            enemies[i].setLocation(enemyCounter - boyX + TILE * posX + TILE / 4, TILE * posY + (TILE * 2 / 3));
                        }
                    }
                    break;
                case 2:
                    if (boyX / TILE + 10 <= 16) {
                        if (enemyCounter > TILE * 6) {
                            g.drawImage(tiles[18], TILE + TILE * (posX - 8), TILE * 12 - enemyCounter + TILE * (posY - 8), null);
                            enemies[i].setLocation(TILE + TILE * (posX - 8), TILE * 12 - enemyCounter + TILE * (posY - 8));
                        } else {
                            g.drawImage(tiles[18], TILE + TILE * (posX - 8), enemyCounter + TILE * (posY - 8), null);
                            enemies[i].setLocation(TILE + TILE * (posX - 8), enemyCounter + TILE * (posY - 8));
                        }
                    } else {
                        if (enemyCounter > TILE * 6) {
                            g.drawImage(tiles[18], TILE * posX - boyX, TILE * 12 - enemyCounter + TILE * (posY - 8), null);
                            enemies[i].setLocation(TILE * posX - boyX, TILE * 12 - enemyCounter + TILE * (posY - 8));
                        } else {
                            g.drawImage(tiles[18], TILE * posX - boyX, enemyCounter + TILE * (posY - 8), null);
                            enemies[i].setLocation(TILE * posX - boyX, enemyCounter + TILE * (posY - 8));
                        }
                    }
                    break;
            }
        }
    }

    private void drawBoy(Graphics2D g) {

        if (!(keysPressed[0] && keysPressed[1])) {
            if (keysPressed[0]) {
                isHeadingRight = true;
            } else if (keysPressed[1]) {
                isHeadingRight = false;
            }
            if ((keysPressed[0] || keysPressed[1]) && !keysPressed[2]) {
                run = true;
                walk = false;
            } else if (keysPressed[0] || keysPressed[1]) {
                run = false;
                walk = true;
            } else {
                run = false;
                walk = false;
            }
        } else {
            run = false;
            walk = false;
        }

        if (boyX / TILE + 10 <= 16) {
            if (!dead) {
                if (!jump) {
                    if (!fall) {
                        if (!walk) {
                            if (!run) {
                                if (isHeadingRight) {
                                    g.drawImage(boyIdle(), boyX, boyY, null);
                                    player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                } else {
                                    g.drawImage(boyIdle(), boyX + TILE, boyY, -TILE, BOYHEIGHT, null);
                                    player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                }
                            } else {
                                if (isHeadingRight) {
                                    g.drawImage(boyRun(), boyX, boyY, null);
                                    player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                } else {
                                    g.drawImage(boyRun(), boyX + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                                    player.setLocation((int) (boyX + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                                }
                            }
                        } else {
                            if (isHeadingRight) {
                                g.drawImage(boyWalk(), boyX, boyY, null);
                                player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                            } else {
                                g.drawImage(boyWalk(), boyX + TILE, boyY, -TILE, BOYHEIGHT, null);
                                player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                            }
                        }
                    } else {
                        if (isHeadingRight) {
                            g.drawImage(boyFall(), boyX, boyY, null);
                            player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                        } else {
                            g.drawImage(boyFall(), boyX + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                            player.setLocation((int) (boyX + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                        }
                    }
                } else {
                    if (isHeadingRight) {
                        g.drawImage(boyJump(), boyX, boyY, null);
                        player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                    } else {
                        g.drawImage(boyJump(), boyX + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                        player.setLocation((int) (boyX + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                    }
                }
            } else {
                if (isHeadingRight) {
                    g.drawImage(boyDie(), boyX, boyY, null);
                    player.setLocation(boyX + TILE / 4, (int) (boyY + (TILE * 0.33)));
                } else {
                    g.drawImage(boyDie(), boyX + TILE, boyY, -TILE * 2, BOYHEIGHT, null);
                    player.setLocation(boyX + TILE / 4 + TILE, (int) (boyY + (TILE * 0.33)));
                }
            }
        } else {
            if (!dead) {
                if (!jump) {
                    if (!fall) {
                        if (!walk) {
                            if (!run) {
                                if (isHeadingRight) {
                                    g.drawImage(boyIdle(), BOYDISTANCE, boyY, null);
                                    player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                } else {
                                    g.drawImage(boyIdle(), BOYDISTANCE + TILE, boyY, -TILE, BOYHEIGHT, null);
                                    player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                }
                            } else {
                                if (isHeadingRight) {
                                    g.drawImage(boyRun(), BOYDISTANCE, boyY, null);
                                    player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                                } else {
                                    g.drawImage(boyRun(), BOYDISTANCE + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                                    player.setLocation((int) (BOYDISTANCE + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                                }
                            }
                        } else {
                            if (isHeadingRight) {
                                g.drawImage(boyWalk(), BOYDISTANCE, boyY, null);
                                player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                            } else {
                                g.drawImage(boyWalk(), BOYDISTANCE + TILE, boyY, -TILE, BOYHEIGHT, null);
                                player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                            }
                        }
                    } else {
                        if (isHeadingRight) {
                            g.drawImage(boyFall(), BOYDISTANCE, boyY, null);
                            player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                        } else {
                            g.drawImage(boyFall(), BOYDISTANCE + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                            player.setLocation((int) (BOYDISTANCE + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                        }
                    }
                } else {
                    if (isHeadingRight) {
                        g.drawImage(boyJump(), BOYDISTANCE, boyY, null);
                        player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                    } else {
                        g.drawImage(boyJump(), BOYDISTANCE + TILE, boyY, (int) -(TILE * 1.33), BOYHEIGHT, null);
                        player.setLocation((int) (BOYDISTANCE + TILE / 4 + (TILE * 0.33)), (int) (boyY + (TILE * 0.33)));
                    }
                }
            } else {
                if (isHeadingRight) {
                    g.drawImage(boyDie(), BOYDISTANCE, boyY, null);
                    player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                } else {
                    g.drawImage(boyDie(), BOYDISTANCE + TILE, boyY, -(TILE * 2), BOYHEIGHT, null);
                    player.setLocation(BOYDISTANCE + TILE / 4, (int) (boyY + (TILE * 0.33)));
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawMap(g);
        drawBoy(g);
        drawEnemies(g);

        if (pause) {
            g.setColor(new Color(20, 20, 20));
            g.setFont(font.deriveFont((float) TILE));
            g.drawString("GAME PAUSED", (float) (WIDTH / 3.5), (float) HEIGHT / 2);
        }

        String scoreString = "SCORE: " + score;
        g.setColor(new Color(240, 240, 240));
        g.setFont(scoreFont.deriveFont((float) TILE / 4));
        g.drawString(scoreString, WIDTH / 256, HEIGHT / 48);

        if (boyIdleCounter >= boyIdle.length) {
            boyIdleCounter = 0;
        }
        if (boyWalkCounter >= boyWalk.length) {
            boyWalkCounter = 0;
        }
        if (boyRunCounter >= boyRun.length) {
            boyRunCounter = 0;
        }
        if (boyJumpCounter >= boyJump.length - 1) {
            jump = false;
            boyJumpCounter = 0;
        }
        if (boyFallCounter >= boyFall.length) {
            boyFallCounter = 0;
        }
        if (boyDeadCounter >= boyDead.length) {
            boyDeadCounter = 0;
        }
        if (enemyCounter >= TILE * 12) {
            enemyCounter = 0;
        }
    }

    @Override
    public void run() {
        while (!pause) {
            try {
                Thread.sleep(FPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            backgroundCounter--;
            aniCounter++;
            enemyCounter++;
            move();
            repaint();
        }
    }
}
