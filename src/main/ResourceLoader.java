package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {

    GamePanel gamePanel;

    public ResourceLoader(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public void loadAll() {
        loadImages();
        loadFonts();
    }

    private void loadImages() {
        try {
            for (int i = 0; i < gamePanel.boyIdle.length; i++) {
                String name = "/res/img/player/idle_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 300, 500);
                image = image.getScaledInstance(GamePanel.TILE, GamePanel.BOYHEIGHT, 32);
                gamePanel.boyIdle[i] = image;
            }
            for (int i = 0; i < gamePanel.boyWalk.length; i++) {
                String name = "/res/img/player/walk_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 300, 500);
                image = image.getScaledInstance(GamePanel.TILE, GamePanel.BOYHEIGHT, 32);
                gamePanel.boyWalk[i] = image;
            }
            for (int i = 0; i < gamePanel.boyRun.length; i++) {
                String name = "/res/img/player/run_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 400, 500);
                image = image.getScaledInstance((int) -(GamePanel.TILE * 1.33), GamePanel.BOYHEIGHT, 32);
                gamePanel.boyRun[i] = image;
            }
            for (int i = 0; i < gamePanel.boyJump.length; i++) {
                String name = "/res/img/player/jump_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 400, 500);
                image = image.getScaledInstance((int) -(GamePanel.TILE * 1.33), GamePanel.BOYHEIGHT, 32);
                gamePanel.boyJump[i] = image;
            }
            for (int i = 0; i < gamePanel.boyFall.length; i++) {
                String name = "/res/img/player/fall_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 400, 500);
                image = image.getScaledInstance((int) -(GamePanel.TILE * 1.33), GamePanel.BOYHEIGHT, 32);
                gamePanel.boyFall[i] = image;
            }
            for (int i = 0; i < gamePanel.boyDead.length; i++) {
                String name = "/res/img/player/dead_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = ((BufferedImage) image).getSubimage(0, 0, 600, 500);
                image = image.getScaledInstance(GamePanel.TILE * 2, GamePanel.BOYHEIGHT, 32);
                gamePanel.boyDead[i] = image;
            }
            for (int i = 0; i < gamePanel.enemyWalk.length; i++) {
                String name = "/res/img/enemies/walk_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = image.getScaledInstance(GamePanel.TILE, GamePanel.TILE, 32);
                gamePanel.enemyWalk[i] = image;
            }
            for (int i = 0; i < gamePanel.tiles.length; i++) {
                String name = "/res/img/tiles/tile_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = image.getScaledInstance(GamePanel.TILE, GamePanel.TILE, 32);
                gamePanel.tiles[i] = image;
            }
            for (int i = 0; i < gamePanel.gold.length; i++) {
                String name = "/res/img/coins/gold_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                image = image.getScaledInstance(GamePanel.TILE / 2, GamePanel.TILE / 2, 32);
                gamePanel.gold[i] = image;
                name = "/res/img/coins/silver_" + (i + 1) + ".png";
                image = ImageIO.read(Platformer.class.getResource(name));
                image = image.getScaledInstance(GamePanel.TILE / 2, GamePanel.TILE / 2, 32);
                gamePanel.silver[i] = image;
                name = "/res/img/coins/bronze_" + (i + 1) + ".png";
                image = ImageIO.read(Platformer.class.getResource(name));
                image = image.getScaledInstance(GamePanel.TILE / 2, GamePanel.TILE / 2, 32);
                gamePanel.bronze[i] = image;
            }
            for (int i = 0; i < gamePanel.backgrounds.length; i++) {
                String name = "/res/img/backgrounds/background_" + (i + 1) + ".png";
                Image image = ImageIO.read(Platformer.class.getResource(name));
                if (i == 4) {
                    image = image.getScaledInstance(GamePanel.TILE * 12, (int) (GamePanel.TILE * 2.5), 32);
                } else {
                    image = image.getScaledInstance(GamePanel.TILE * 12, GamePanel.TILE * 5, 32);
                }
                gamePanel.backgrounds[i] = image;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFonts() {

        try {
            InputStream fontInput = Platformer.class.getResourceAsStream("/res/fonts/font.otf");
            InputStream scoreFontInput = Platformer.class.getResourceAsStream("/res/fonts/score_font.ttf");
            gamePanel.font = Font.createFont(Font.TRUETYPE_FONT, fontInput);
            gamePanel.scoreFont = Font.createFont(Font.TRUETYPE_FONT, scoreFontInput);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}
