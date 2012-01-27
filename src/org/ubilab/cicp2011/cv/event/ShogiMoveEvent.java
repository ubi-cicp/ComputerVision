package org.ubilab.cicp2011.cv.event;

/**
 * 将棋駒の移動イベントオブジェクト
 * @author atsushi-o
 */
public class ShogiMoveEvent extends java.util.EventObject {
    private final int x;
    private final int y;

    public ShogiMoveEvent(Object source, int x, int y) {
        super(source);
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
