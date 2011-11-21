package org.ubilab.cicp2011.cv;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.googlecode.javacv.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * 盤面検出処理メインクラス
 * <pre>
 * インスタンスの生成にはBuilderクラスを使用する
 * <blockquote>
 * new CvMain.Builder(0).build();
 * </blockquote>
 * </pre>
 * @author atsushi-o
 * @since 2011/11/17
 */
public class CvMain {
    private CvCapture capture;
    private CvRect ROIrect;
    private boolean debug;
    private static final CanvasFrame src;
    
    static {
        src = new CanvasFrame("Source");
        src.setVisible(false);
        src.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        src.addWindowListener(new WindowAdapter() {
			// ウィンドウが閉じるときに呼ばれる
			@Override
			public void windowClosing(WindowEvent e) {
				AnalyticProcess.releaseMemStorage();
			}
		});
    }

    /**
     * CvMainのインスタンス生成クラス
     * @since 2011/11/21
     */
    public static class Builder {
        // Required param
        private final int camera;
        
        // Optional param
        private int width       = 1280;
        private int height      = 960;
        private boolean debug   = false;
        
        /**
         * 必須パラメータを指定
         * @param camera OpenCVで使用するカメラのインデックス
         * @since 2011/11/21
         */
        public Builder(int camera) {
            this.camera = camera;
        }
        
        public Builder width(int val)       { width = val; return this; }
        public Builder height(int val)      { height = val; return this; }
        public Builder debug(boolean val)   { debug = val; return this; }
        
        /**
         * CvMainのインスタンスを生成する
         * @return CvMainのインスタンス
         * @since 2011/11/21
         */
        public CvMain build() {
            return new CvMain(this);
        }
    }
    
    /**
     * Builderクラスからパラメータを受け取りインスタンスを生成する
     * @param param Builderクラスのインスタンス
     * @since 2011/11/21
     */
    private CvMain(Builder param) {
        // カメラ設定
        capture = cvCreateCameraCapture(param.camera);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH, param.width);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT, param.height);
        src.setVisible(param.debug);
        debug = param.debug;
        
        while (true) {
            AnalyticProcess thread = new AnalyticProcess(_captureFrame(), debug);
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
        new CvMain.Builder(0).debug(true).build();
    }
}
