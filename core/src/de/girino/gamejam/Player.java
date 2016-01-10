package de.girino.gamejam;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class Player {

    public static final int FIRE_ENERGY = 20;
    public static final int MAX_ENERGY = 100;
    public static final int MAX_INVULNERABLE = 50;

    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 3;

    public Sprite sprite;
    public int direction;
    public int invulnerable = 0;
    public int health = 3;
    public int energy = 0;

    public void reverseDirection() {
        switch(direction) {
            case DIRECTION_DOWN:
                direction = DIRECTION_UP;
                return;
            case DIRECTION_UP:
                direction =  DIRECTION_DOWN;
                return;
            case DIRECTION_RIGHT:
                direction =  DIRECTION_LEFT;
                return;
            case DIRECTION_LEFT:
                direction =  DIRECTION_RIGHT;
                return;
        }
    }

}
