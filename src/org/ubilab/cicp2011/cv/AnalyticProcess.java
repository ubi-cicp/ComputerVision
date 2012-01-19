package org.ubilab.cicp2011.cv;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.googlecode.javacpp.Pointer;
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
    private SquareList squares;
    private boolean debug = false;
    private AnalyticProcessDelegate delegate = null;
    private CvController cController = null;

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
        squares = new SquareList();

        // デリゲートクラスのインスタンスを保持
        delegate = instance;

        if (db) cController = CvController.getInstance();
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
        _print("完了\n");
        // 盤検出
        roiRect = getROI(src);

        _print(String.format("* 検出ROI領域: (%d, %d), (%d, %d)\n",
                roiRect.x(), roiRect.y(), roiRect.x()+roiRect.width(), roiRect.y()+roiRect.height()));

        if (roiRect.width() * roiRect.height() > 0) {
            // ROI領域切り出し
            IplImage roiFrame = getROIView(src, roiRect);

            // マス検出
            getRects(roiFrame);

            // フレーム間差分を取る
            IplImage prev = delegate.getPrevFrame();
            if (prev != null) {
                frameDiff(src, prev);
            }
            delegate.setPrevFrame(src);

            cvReleaseImage(roiFrame);
        }

        cvClearMemStorage(storage);

        _print("位置推定処理スレッドを終了...");
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

        _print("ROI領域検出処理...\n");
        /*
         * 矩形領域検出
         */
        // グレースケールに変更
        _print("    - グレースケール変換...");
        cvCvtColor(input, tmp, CV_RGB2GRAY);
        _print("完了\n");

        // 単純平滑化
        _print("    - 単純平滑化処理...");
        cvSmooth(tmp, tmp, CV_BLUR, 2);
        _print("完了\n");

        // Canny
        _print("    - エッジ検出処理...");
        cvCanny(tmp, canny, 50.0, 200.0, 3);
        _print("完了\n");

        // 2値化
        _print("    - 二値化処理...");
        cvThreshold(canny, canny, 128, 255, CV_THRESH_BINARY);
        _print("完了\n");

        // 確率的Hough変換
        _print("    - 確率的Hough変換処理...");
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
        _print("完了\n");

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

        _print("完了\n");
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

        _print("ノイズ除去処理...");
        cvPyrDown(input, tmp, CV_GAUSSIAN_5x5);
        cvPyrUp(tmp, input, CV_GAUSSIAN_5x5);

        cvReleaseImage(tmp);

        _print("完了\n");
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

        // オリジナルを保持
        cvCopy(input, orig);

        _print("マス目検出処理...\n");
        // 各チャンネル処理
        for (int i = 1; i <= 3; i++) {
            _print(String.format("    - チャンネル %d 処理...\n", i));
            // COI設定・切り出し処理
            cvSetImageCOI(orig, i);
            cvCopy(orig, tmp1);

            // エッジ検出
            _print("        - エッジ検出処理...");
            cvCanny(tmp1, tmp2, 80.0, 300.0, 3);
            _print("完了\n");

            // エッジ強調
            _print("        - エッジ強調処理...");
            cvDilate(tmp2, tmp2, null, 1);
            _print("完了\n");

            // 輪郭検出
            _print("        - 輪郭抽出処理...");
            CvSeq contours = new CvSeq(null);
            cvFindContours(tmp2, contoursStorage, contours, sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
            _print("完了\n");

            // 検出された輪郭点群を一つ一つ取り出す
            _print("        - 輪郭端点検出・条件判定処理...");
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
                        // 矩形候補を保存
                        CvPoint square = new CvPoint(4);
                        for (int j = 0; j < poly.total(); j++) {
                            square.position(j).set(new CvPoint(cvGetSeqElem(poly, j)));
                        }
                        squares.add(square);
                    }
                }
            }
            _print("完了\n");

            cvClearMemStorage(contoursStorage);

            _print(String.format("    - チャンネル %d 処理完了\n", i));
        }

        // 抽出された矩形ごとの処理
        squares.sort();
        _print(String.format("* 検出されたマス目の数: %d\n", squares.size()));
        _print(squares.toString());
        for (int i = 0; i < squares.size(); i++) {
            squares.drawSquare(input, i);
        }

        // 結果を出力
        showImage("ROI", input);

        cvReleaseImage(orig);
        cvReleaseImage(tmp1);
        cvReleaseImage(tmp2);
        cvReleaseMemStorage(contoursStorage);

        _print("完了\n");
    }

    /**
     * フレーム間差分から変化のあったマスを検出する
     * @param src 現在のフレーム
     * @since 2012/01/18
     */
    public void frameDiff(IplImage now, IplImage prev) {
        _print("フレーム間差分取得処理...\n");
        CvSize roiSize = cvGetSize(now);
        IplImage diffImage = cvCreateImage(roiSize, IPL_DEPTH_8U, 3);

        // フレーム間差分を取る
        cvAbsDiff(now, prev, diffImage);
        // 2値化
        cvThreshold(diffImage, diffImage, 15, 255, CV_THRESH_BINARY);
        // ノイズ除去（第四引数の数で強度を変更）
        cvErode(diffImage, diffImage, null, 3);

        // マス描画用にROIを設定
        cvSetImageROI(diffImage, roiRect);
        for (int i = 0; i < squares.size(); i++) {
        squares.drawSquare(diffImage, i);
        }

        showImage("Diff", diffImage);
        _print("完了\n");
    }

    /**
     * デバッグ用出力関数
     * @param str 出力文字列
     * @since 2011/12/01
     */
    private void _print(String str) {
        if (cController != null) cController.addText(str);
        logger.fine(str.replaceAll("\n", ""));
    }
}
