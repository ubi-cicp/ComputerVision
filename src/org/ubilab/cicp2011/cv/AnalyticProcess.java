package org.ubilab.cicp2011.cv;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacpp.Loader.*;

/**
 * メインの画像処理を行うクラス
 * <pre>
 * メインの画像処理（ROI検出・枡検出・駒検出）を行う．
 * CvMainクラスから別スレッドとして実行
 * </pre>
 * @author atsushi-o
 * @since 2011/11/17
 */
public class AnalyticProcess extends Thread {
    private static final CvMemStorage storage;
    private static final Logger logger;
    private IplImage src = null;
    private CvRect roiRect = null;
    private boolean debug = false;
    private AnalyticProcessDelegate delegate = null;

    static {
        storage = CvMemStorage.create();
        logger = Logger.getLogger(AnalyticProcess.class.getName());
    }

    /**
     * メイン画像処理スレッドのインスタンスを生成する
     * @param input 処理対象のフレーム
     * @since 2011/11/17
     */
    public AnalyticProcess(IplImage input) {
        this(input, false, null);
    }

    /**
     * メイン画像処理スレッドのインスタンスをdelegateクラスを指定して生成する（デバッグフラグON）
     * @param input 処理対象のフレーム
     * @param instance delegateクラスのインスタンス
     * @since 2011/11/21
     */
    public AnalyticProcess(IplImage input, AnalyticProcessDelegate instance) {
        this(input, true, instance);
    }

    /**
     * メイン画像処理スレッドのインスタンスをデバッグフラグとdelegateクラスを指定して生成する
     * @param input 処理対象のフレーム
     * @param db デバッグフラグ
     * @param instance delegateクラスのインスタンス
     * @since 2011/11/21
     */
    public AnalyticProcess(IplImage input, boolean db, AnalyticProcessDelegate instance) {
        super();
        src = input;
        debug = db;

        // デリゲートクラスのインスタンスを保持
        delegate = instance;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        //if (src != null) cvReleaseImage(src);
    }

    /**
     * 主メモリストレージを解放する
     */
    public static synchronized final void releaseMemStorage() {
        logger.info("Release main memory storage.");
        cvReleaseMemStorage(storage);
    }

    /**
     * Imageを指定したkeyのCanvasFrameに表示する
     * @param key CanvasFrame名
     * @param image 表示する画像
     * @see AnalyticProcessDelegate#showImage(java.lang.String, com.googlecode.javacv.cpp.opencv_core.IplImage)
     * @since 2011/11/22
     */
    private void showImage(String key, IplImage image) {
        if (debug && delegate != null) {
            logger.log(Level.FINER, "Call delegate method (showImage) at {0}.", delegate);
            delegate.showImage(key, image);
        }
    }

    /**
     * 画像処理本体
     * @since 2011/11/17
     */
    @Override
    public void run() {
        // 盤検出
        roiRect = getROI(src);

        if (roiRect.width() * roiRect.height() > 0) {
            // ROI領域切り出し
            IplImage roiFrame = getROIView(src, roiRect);

            // ます検出
            getRects(roiFrame);

            cvReleaseImage(roiFrame);
        }

        cvClearMemStorage(storage);
    }

    /**
     * ROIを検出する
     * @param input 入力画像
     * @return 検出されたROIを表すCvRect
     * @since 2011/11/17
     */
    public CvRect getROI(IplImage input) {
        CvSize srcSize = cvGetSize(input);
        IplImage colorDst = cvCreateImage(srcSize, IPL_DEPTH_8U, 3);
        //colorDst = IplImage.create(srcSize, IPL_DEPTH_8U, 3);
        IplImage canny = cvCreateImage(srcSize, IPL_DEPTH_8U, 1);
        IplImage tmp = cvCreateImage(srcSize, IPL_DEPTH_8U, 1);
        CvMemStorage houghStorage = cvCreateChildMemStorage(storage);
        CvMemStorage pointsStorage = cvCreateChildMemStorage(storage);
        CvSeq lines, points;

        /*
         * 矩形領域検出
         */
        // グレースケールに変更
        cvCvtColor(input, tmp, CV_RGB2GRAY);

        // 単純平滑化
        cvSmooth(tmp, tmp, CV_BLUR, 2);

        // Canny
        cvCanny(tmp, canny, 50.0, 200.0, 3);

        // 2値化
        cvThreshold(canny, canny, 128, 255, CV_THRESH_BINARY);

        // 確率的Hough変換
        cvCvtColor(canny, colorDst, CV_GRAY2BGR);
        points = cvCreateSeq(CV_SEQ_ELTYPE_POINT, sizeof(CvSeq.class), sizeof(CvPoint.class), pointsStorage);
        lines = cvHoughLines2(canny, houghStorage, CV_HOUGH_PROBABILISTIC, 1, Math.PI/180, 50, 100, 15);
        for (int i = 0; i < lines.total(); i++) {
            Pointer line = cvGetSeqElem(lines, i);
            CvPoint pt1 = new CvPoint(line).position(0);
            CvPoint pt2 = new CvPoint(line).position(1);
            cvSeqPush(points, pt1);
            cvSeqPush(points, pt2);
            cvLine(colorDst, pt1, pt2, CV_RGB(255, 0, 0), 1, 8, 0);
        }

        // ROI矩形領域検出
        CvRect roiRect = cvBoundingRect(points, 0);

        cvRectangle(colorDst, cvPoint(roiRect.x(), roiRect.y()), cvPoint(roiRect.x()+roiRect.width(), roiRect.y()+roiRect.height()), CV_RGB(0, 255, 0), 2, CV_AA, 0);
        showImage("Hough", colorDst);

        // 後処理
        cvReleaseImage(tmp);
        cvReleaseImage(canny);
        cvReleaseImage(colorDst);
        cvClearSeq(lines);
        cvClearSeq(points);
        cvReleaseMemStorage(houghStorage);
        cvReleaseMemStorage(pointsStorage);

        return roiRect;
    }

    /**
     * 計算済みのROIを取得する
     * @return 既に計算済みの場合はそのCvRectを．そうでない場合はnullを返す
     * @since 2011/11/17
     */
    public synchronized CvRect getROI() {
        return roiRect;
    }

    /**
     * 指定されたROI領域を切り出して返す
     * @param input 入力画像
     * @param roi ROI領域
     * @return ROI領域の画像
     * @since 2011/11/21
     */
    public IplImage getROIView(IplImage input, CvRect roi) {
        CvSize srcSize = cvGetSize(input);
        CvSize roiSize = cvSize(roi.width(), roi.height());
        IplImage tmp = cvCreateImage(srcSize, IPL_DEPTH_8U, 3);
        IplImage roiImage = cvCreateImage(roiSize, IPL_DEPTH_8U, 3);

        cvCopy(input, tmp);
        cvSetImageROI(tmp, roi);
        cvCopy(tmp, roiImage);

        cvReleaseImage(tmp);

        return roiImage;
    }

    /**
     * 画像のダウン・アップサンプリングを行いノイズを除去する
     * @param input 入力画像
     * @return 処理済みの画像
     * @since 2011/11/22
     */
    public IplImage resamplingImage(IplImage input) {
        CvSize srcSize = cvGetSize(input);
        CvSize half = cvSize(srcSize.width()/2, srcSize.height()/2);
        IplImage tmp = cvCreateImage(half, IPL_DEPTH_8U, 3);

        cvPyrDown(input, tmp, CV_GAUSSIAN_5x5);
        cvPyrUp(tmp, input, CV_GAUSSIAN_5x5);

        cvReleaseImage(tmp);

        return input;
    }

    /**
     * マス目を検出する
     * @param input 入力画像
     * @since 2011/11/17
     */
    public void getRects(IplImage input) {
        CvSize srcSize = cvGetSize(input);
        IplImage orig = cvCreateImage(srcSize, IPL_DEPTH_8U, 3);
        IplImage tmp1 = cvCreateImage(srcSize, IPL_DEPTH_8U, 1);
        IplImage tmp2 = cvCreateImage(srcSize, IPL_DEPTH_8U, 1);
        CvMemStorage contoursStorage = cvCreateChildMemStorage(storage);
        CvMemStorage squaresStorage  = cvCreateChildMemStorage(storage);
        CvSeq squares = cvCreateSeq(CV_SEQ_ELTYPE_POINT, sizeof(CvSeq.class), sizeof(CvPoint.class), squaresStorage);
        
        // オリジナルを保持
        cvCopy(input, orig);

        int count = 0;
        // 各チャンネル処理
        for (int i = 1; i <= 3; i++) {
            // COI設定・切り出し処理
            cvSetImageCOI(orig, i);
            cvCopy(orig, tmp1);

            // エッジ検出
            cvCanny(tmp1, tmp2, 80.0, 300.0, 3);

            // エッジ強調
            cvDilate(tmp2, tmp2, null, 1);

            // 輪郭検出
            CvSeq contours = new CvSeq(null);
            cvFindContours(tmp2, contoursStorage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
            
            // 検出された輪郭点群を一つ一つ取り出す
            for (;contours != null && !contours.isNull(); contours = contours.h_next()) {
                if (contours.elem_size() > 0) {
                    // ポリライン近似
                    CvSeq poly = cvApproxPoly(contours, sizeof(CvContour.class), contoursStorage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);

                    // 点数が4点以外の矩形は除外
                    if (poly.total() != 4) continue;
                    
                    // 伸長度計算
                    double area      = Math.abs(cvContourArea(poly, CV_WHOLE_SEQ, 0));
                    double perimeter = cvArcLength(poly, CV_WHOLE_SEQ, -1);
                    double extension = Math.pow(perimeter, 2) / area;
                    
                    // 閾値によるマス目判定
                    if (area > 1050*4 && area < 2100*4){
                        // 輪郭端点表示用：輪郭
                        cvDrawContours(input, poly, CvScalar.RED, CvScalar.GREEN, -1, 2, CV_AA, cvPoint(0, 0));
                        
                        // 矩形候補を保存
                        for (int j = 0; j < poly.total(); j++) {
                            cvSeqPush(squares, cvGetSeqElem(poly, j));
                        }
                        
                        count++;
                    }
                }
            }
            
            cvClearMemStorage(contoursStorage);
        }

        logger.log(Level.FINE, "検出されたマス目の数: {0}", count);

        // 結果を出力
        showImage("ROI View", input);

        cvReleaseImage(tmp1);
        cvReleaseImage(tmp2);
        cvClearSeq(squares);
        cvReleaseMemStorage(contoursStorage);
        cvReleaseMemStorage(squaresStorage);
    }
}
