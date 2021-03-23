package main;

import java.awt.*;

public class Enemy extends Rectangle {

    int code;
    int posX;
    int posY;

    public Enemy(int code, int posX, int posY, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.code = code;
        this.posX = posX;
        this.posY = posY;
    }
}
