package org.ubilab.cicp2011.cv;

/**
 * CvController用のデリゲートインターフェース
 * @author atsushi-o
 * @since 2011/11/30
 */
public interface CvControllerDelegate {
    /**
     * キャプチャを実行する
     * @since 2011/11/30
     */
    public void capture();
    /**
     * プログラムを終了する
     * @since 2011/11/30
     */
    public void quit();
}
