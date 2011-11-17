package org.ubilab.cicp2011.cv;

import javax.swing.JFrame;
import com.googlecode.javacv.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * 盤面検出処理メインクラス
 * @author atsushi-o
 * @since 2011/11/17
 */
public class CvMain {
    private CvCapture capture;
    private CvRect ROIrect;
    private static final CanvasFrame src;
    
    static {
        src = new CanvasFrame("Source");
        src.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    /**
     * カメラのインデックスを指定してインスタンスを生成する
     * @param camera OpenCVで使用するカメラのインデックス
     * @since 2011/11/17
     */
    public CvMain(int camera) {
        this(camera, 1280, 960);
    }
    /**
     * カメラの入力解像度も指定した上でインスタンスを生成する
     * @param camera OpenCVで使用するカメラのインデックス
     * @param width 横方向の入力解像度
     * @param height 縦方向の入力解像度
     * @since 2011/11/17
     */
    public CvMain(int camera, int width, int height) {
        // カメラ設定
        capture = cvCreateCameraCapture(camera);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH, width);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT, height);
        
        while (true) {
            AnalyticProcess thread = new AnalyticProcess(_captureFrame());
            thread.start();
            try {
                // スレッドの実行が終了するまで待機
                thread.join();
            } catch (InterruptedException e) {
                
            }
        }
    }
    
    /**
     * カメラから画像をキャプチャし，結果を返す
     * <pre>
     * デバッグ用．
     * 本ビルド時は通常のcvQueryFrame()かそれに代わるメソッドに置き換え．
     * </pre>
     * @return キャプチャしたフレーム
     * @deprecated デバッグ用（ソースフレームを表示する）
     * @since 2011/11/17
     */
    private IplImage _captureFrame() {
        IplImage capFrame;
        capFrame = cvQueryFrame(capture);
        src.showImage(capFrame);
        return capFrame;
    }
    
    public static void main(String[] args) {
        new CvMain(0);
    }
}
