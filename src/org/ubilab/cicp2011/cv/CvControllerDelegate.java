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
     * ウインドウの表示・非表示を切り替える
     * @param key 切り替え対象のウインドウ名
     * @param b 表示・非表示
     * @since 2011/12/01
     */
    public void setVisible(String key, boolean b);
    /**
     * プログラムを終了する
     * @since 2011/11/30
     */
    public void quit();
}
