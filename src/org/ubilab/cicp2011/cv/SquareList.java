package org.ubilab.cicp2011.cv;

import java.util.ArrayList;
import static com.googlecode.javacv.cpp.opencv_core.CvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvLine;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.CV_AA;

/**
 * 抽出矩形の保持用リスト
 *
 * @author atsushi-o
 * @since 2011/12/03
 */
public class SquareList extends ArrayList<CvPoint> {
    private double widthAve;
    private double heightAve;
    private ArrayList<Integer> complementedSquare;

    /**
     * コンストラクタ
     * @since 2011/12/05
     */
    public SquareList() {
        super();
        widthAve = 0;
        heightAve = 0;
        complementedSquare = new ArrayList<Integer>();
    }

    /**
     * 抽出矩形を表す4点の配列を左上，右上，右下，左下の順番に並べ替えてリストの最後に追加する
     * @param e 要素を4つ持つCvPoint
     * @return true ({@link java.util.Collection#add(java.lang.Object)} で指定されているとおり)
     * @since 2011/12/03
     */
    @Override
    public boolean add(CvPoint e) {
        if (e == null) return false;
        if (contains(e)) return false;

        CvPoint ret = new CvPoint(4);
        java.util.ArrayList<CvPoint> list = new java.util.ArrayList<CvPoint>();

        for (int i = 0; i < 4; i ++) {
            list.add(new CvPoint(e.position(i)));
        }
        java.util.Collections.sort(list, new ManhattanComparator());

        // 左下と右下の順番を入れ替え
        list.set(2, list.set(3, list.get(2)));

        // 幅と高さの平均を計算
        int width = Math.abs(list.get(0).x() - list.get(1).x());
        int height = Math.abs(list.get(1).y() - list.get(2).y());
        widthAve = (widthAve * size() + width) / (size() + 1);
        heightAve = (heightAve * size() + height) / (size() + 1);

        int i = 0;
        for (CvPoint p : list) {
            ret.position(i++).set(p);
        }
        return super.add(ret);
    }

    /**
     * 抽出矩形のリストを左上から右下方向へソートする
     * @since 2011/12/03
     */
    public void sort() {
        java.util.Collections.sort(this, new SquareComparator());

        // 重複マスを除去
        java.util.Iterator<CvPoint> it = this.iterator();
        CvPoint prevPt = it.next();
        while (it.hasNext()) {
            CvPoint pt = it.next();
            if (cvPointDiff(pt, prevPt) < 30) {
                it.remove();
            } else {
                prevPt = pt;
            }
        }
    }

    /**
     * 2つの検出矩形の4点の座標の差の絶対値の和を返す
     * @param pt1 比較する矩形
     * @param pt2 比較する矩形
     * @return 4点の座標の差の絶対値の和
     * @deprecated オブジェクト指向の考え方からしてよろしくない実装
     * @since 2011/12/05
     */
    private int cvPointDiff(CvPoint pt1, CvPoint pt2) {
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            sum += Math.abs(pt1.position(i).x() - pt2.position(i).x()) + Math.abs(pt1.position(i).y() - pt2.position(i).y());
        }
        return sum;
    }

    /**
     * 指定されたindexの矩形を描画する
     * @param img 書き出し先のIplImage
     * @param index 書きだす矩形のindex
     * @since 2011/12/05
     */
    public void drawSquare(IplImage img, int index) {
        CvPoint pt = get(index);
        for (int i = 0; i < 4; i++) {
            cvLine(img, new CvPoint(pt.position(i)), new CvPoint(pt.position((i+1)%4)), CvScalar.GREEN, 2, CV_AA, 0);
        }
    }

    /**
     * 抽出矩形のリストを出力する
     *
     * @return 抽出矩形のリストの文字列表現
     * @since 2011/12/04
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (CvPoint p : this) {
            sb.append(String.format("(%d, %d), (%d, %d), (%d, %d), (%d, %d)\n",
                p.position(0).x(), p.position(0).y(),
                p.position(1).x(), p.position(1).y(),
                p.position(2).x(), p.position(2).y(),
                p.position(3).x(), p.position(3).y()));
        }

        sb.append("-----------------------------------\n");
        sb.append(String.format("平均幅: %f\n平均高: %f\n", widthAve, heightAve));

        return sb.toString();
    }

    /**
     * 抽出矩形のソート用比較クラス
     *
     * @author atsushi-o
     * @since 2011/12/03
     */
    private class SquareComparator implements java.util.Comparator<CvPoint> {
        @Override
        public int compare(CvPoint o1, CvPoint o2) {
            int diffY = o1.position(0).y() - o2.position(0).y();
            if (diffY > -(heightAve/2) && diffY < (heightAve/2)) {
                return o1.position(0).x() - o2.position(0).x();
            }

            return diffY;
        }
    }

    /**
     * CvPointのマンハッタン距離による比較クラス
     *
     * @author atsushi-o
     * @since 2011/12/04
     */
    public class ManhattanComparator implements java.util.Comparator<CvPoint> {
        @Override
        public int compare(CvPoint o1, CvPoint o2) {
            int o1m = o1.x() + o1.y();
            int o2m = o2.x() + o2.y();
            return o1m - o2m;
        }
    }
}
