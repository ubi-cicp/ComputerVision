package org.ubilab.cicp2011.cv.event;

/**
 * 将棋駒の移動イベントリスナ
 * @author atsushi-o
 */
public interface ShogiMoveEventListener extends java.util.EventListener {
    /**
     * 将棋駒の移動イベントの発生
     * @param evt
     */
    public void move(ShogiMoveEvent evt);
}
