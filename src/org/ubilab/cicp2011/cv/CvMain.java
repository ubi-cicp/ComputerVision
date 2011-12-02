package org.ubilab.cicp2011.cv;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.JFrame;
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
    private boolean debug;
    private boolean useDummy;
    private static final HashMap<String, CanvasFrame> canvas;
    private static final Logger logger;
    private AnalyticProcess curThread = null;
    private CvController cController = null;
    private IplImage _dummyPic = null;
    
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
        private boolean useDummy= false;
        
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
        public Builder useDummy(boolean val){ useDummy = val; return this; }
        
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
            
            cController = CvController.getInstance();
            cController.setDelegate(this);
            cController.setVisible(true);
        }
        _setVisible(debug);
        useDummy = param.useDummy;
        
        logger.log(Level.INFO, "CvMain start: camera{0} ({1}x{2}) {3}", new Object[]{param.camera, param.width, param.height, debug?"DEBUG":""});
    }
         
    @Override
    public void capture() {
        if (cController != null) cController.clearText();
        _print("位置推定処理スレッドを開始...");
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    curThread = new AnalyticProcess(useDummy?_dummyFrame():_captureFrame(), debug, CvMain.this);
                    curThread.start();
                    // スレッドの実行が終了するまで待機
                    curThread.join();
                } catch (IllegalThreadStateException e) {
                } catch (InterruptedException e) {
                } finally {
                    curThread = null;
                }

                _print("メモリ解放処理...");
                // GCを強制呼び出し
                Runtime.getRuntime().gc();
                _print("完了\n");
                _print("=== 位置推定処理終了 ===\n");
            }
        });
        th.start();
    }

    @Override
    public void quit() {
        curThread = null;
        AnalyticProcess.releaseMemStorage();
        if (_dummyPic != null) cvReleaseImage(_dummyPic);
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
     * ダミー画像を読み込み，返す
     * <pre>
     * カメラがない環境でのデバッグ用．
     * </pre>
     * @return ダミー画像
     * @deprecated デバッグ用
     * @since 2011/12/03
     */
    private IplImage _dummyFrame() {
        if (_dummyPic == null) {
            _dummyPic = cvLoadImage("dummy.jpg", CV_LOAD_IMAGE_COLOR);
        }
        showImage("Source", _dummyPic);
        return _dummyPic;
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
    
    /**
     * デバッグ用出力関数
     * @param str 出力文字列
     * @since 2011/12/01
     */
    private void _print(String str) {
        if (cController != null) cController.addText(str);
        else System.out.print(str);
    }
    
    public static void main(String[] args) {
        new CvMain.Builder(0).debug(true).useDummy(true).build();
    }
}
