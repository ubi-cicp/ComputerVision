package org.ubilab.cicp2011.cv;

import org.ubilab.cicp2011.cv.event.ShogiMoveEvent;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * AnalyticProcessのデリゲートクラス
 * @author atsushi-o
 * @since 2011/11/21
 */
public interface AnalyticProcessDelegate {
    /**
     * 指定した名前のCanvasFrameを生成する
     * @param key CanvasFrameの名前
     * @since 2011/11/21
     */
    public void createCanvas(String key);
    /**
     * Imageを指定したkeyのCanvasFrameに表示する
     * @param key CanvasFrame名
     * @param image 表示する画像
     * @since 2011/11/21
     */
    public void showImage(String key, IplImage image);
    /**
     * 一つ前のフレーム画像を取得する
     * @return 一つ前のフレーム画像
     */
    public IplImage getPrevFrame();
    /**
     * 一つ前のフレーム画像を保存する
     * @param image フレーム画像
     */
    public void setPrevFrame(IplImage image);
    /**
     * 変化のあったマス目を返す
     * @param evt 変化のあったマス目を表すイベントオブジェクト
     */
    public void moveEventOccur(ShogiMoveEvent evt);
}
