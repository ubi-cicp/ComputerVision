package org.ubilab.cicp2011.cv;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
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
public class CvMain implements AnalyticProcessDelegate, CvControllerDelegate {
    private CvCapture capture;
    private boolean runnable = true;
    private boolean debug;
    private static final HashMap<String, CanvasFrame> canvas;
    private static final Logger logger;
    private AnalyticProcess curThread = null;
    private CvController cController = null;
    
    static {
        canvas = new HashMap<String, CanvasFrame>();
        logger = Logger.getLogger(CvMain.class.getName());
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
            createCanvas("Source");
            createCanvas("Hough");
            createCanvas("ROI View");
            
            cController = new CvController();
            cController.setDelegate(this);
        }
        _setVisible(debug);
        
        logger.log(Level.INFO, "CvMain start: camera{0} ({1}x{2}) {3}", new Object[]{param.camera, param.width, param.height, debug?"DEBUG":""});
    }
         
    @Override
    public void capture() {
        try {
            curThread = new AnalyticProcess(_captureFrame(), debug, this);
            curThread.start();
            // スレッドの実行が終了するまで待機
            curThread.join();
        } catch (IllegalThreadStateException e) {
        } catch (InterruptedException e) {
        } finally {
            curThread = null;
        }
        
        // GCを強制呼び出し
        Runtime.getRuntime().gc();
    }

    @Override
    public void quit() {
        AnalyticProcess.releaseMemStorage();
        disposeAllCanvas();
        cController.dispose();
        System.exit(0);
    }

    @Override
    public final void createCanvas(String key) {
        logger.log(Level.INFO, "Create New CanvasFrame: {0}", key);
        synchronized(this) {
            if (!canvas.containsKey(key)) {
                CanvasFrame tmp = new CanvasFrame(key);
                tmp.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                canvas.put(key, tmp);
            }
        }
    }
    
    /**
     * すべてのCanvasFrameを閉じてリソースを解放する
     * @since 2011/11/30
     */
    private synchronized void disposeAllCanvas() {
        logger.info("Close all CanvasFrame");
        for (CanvasFrame cf : canvas.values()) {
            cf.setVisible(false);
            cf.dispose();
        }
        canvas.clear();
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
        new CvMain.Builder(0).debug(true).build();
    }
}
