package org.ubilab.cicp2011.cv;

import java.util.HashMap;
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
public class CvMain extends Thread implements AnalyticProcessDelegate {
    private CvCapture capture;
    private boolean runnable = true;
    private boolean debug;
    private static final HashMap<String, CanvasFrame> canvas;
    private AnalyticProcess curThread = null;
    
    static {
        canvas = new HashMap<String, CanvasFrame>();
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
        
        // デバッグ用設定
        debug = param.debug;
        if (debug) {
            CanvasFrame tmp = new CanvasFrame("Source");
            tmp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            tmp.addWindowListener(new WindowAdapter() {
                // ウィンドウが閉じるときに呼ばれる
                @Override
                public void windowClosing(WindowEvent e) {
                    halt();
                }
            });
            canvas.put("Source", tmp);
            
            createCanvas("Hough");
            createCanvas("ROI View");
        }
        _setVisible(debug);
    }
    
    @Override
    public void start() {
        runnable = true;
        super.start();
    }
    
    /**
     * 実行中のスレッドを停止する
     * @since 2011/11/22
     */
    public synchronized void halt() {
        runnable = false;
    }
    
    @Override
    public void run() {
        curThread = new AnalyticProcess(null, debug, this);
        while (runnable) {
            try {
                curThread.start(_captureFrame());
                // スレッドの実行が終了するまで待機
                curThread.join();
            } catch (IllegalThreadStateException e) {
                continue;
            } catch (InterruptedException e) {
                curThread = null;
                break;
            }
        }
        curThread = null;
        AnalyticProcess.releaseMemStorage();
    }

    @Override
    public final void createCanvas(String key) {
        synchronized(this) {
            if (!canvas.containsKey(key)) {
                CanvasFrame tmp = new CanvasFrame(key);
                tmp.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                canvas.put(key, tmp);
            }
        }
    }
    
    @Override
    public final void showImage(String key, IplImage image) {
        synchronized(this) {
            if (canvas.containsKey(key)) {
                CanvasFrame f = canvas.get(key);
                f.setSize(image.width(), image.height());
                f.showImage(image);
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
        IplImage capFrame = cvQueryFrame(capture);
        showImage("Source", capFrame);
        return capFrame;
    }
    
    /**
     * デバッグ用CanvasFrameの表示/非表示を切り替える
     * @param b 表示/非表示
     * @since 2011/11/21
     */
    private void _setVisible(boolean b) {
        for (CanvasFrame f : canvas.values()) {
            f.setVisible(b);
        }
    }
    
    public static void main(String[] args) {
        CvMain cv = new CvMain.Builder(0).debug(true).build();
        cv.start();
    }
}
