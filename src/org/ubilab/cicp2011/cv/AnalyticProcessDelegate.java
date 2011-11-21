package org.ubilab.cicp2011.cv;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * AnalyticProcessのデリゲートクラス
 * @author atsushi-o
 * @since 2011/11/21
 */
public interface AnalyticProcessDelegate {
    /**
     * Imageを指定したkeyのCanvasFrameに表示する
     * @param key CanvasFrame名
     * @param image 表示する画像
     */
    public void showImage(String key, IplImage image);
}
