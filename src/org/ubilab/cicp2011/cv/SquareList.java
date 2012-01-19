package org.ubilab.cicp2011.cv;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

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
    private final int xSize;
    private final int ySize;
    private double  widthAve;
    private double  heightAve;
    private int     maxX;
    private int     minX;
    private int     maxY;
    private int     minY;
    private ArrayList<Integer> complementedSquare;

    /** 将棋盤の横マス数 */
    public static final int SHOGI_BOARD_X_SIZE = 9;
    /** 将棋盤の縦マス数 */
    public static final int SHOGI_BOARD_Y_SIZE = 9;

    private static final Logger LOG = Logger.getLogger(SquareList.class.getName());

    /**
     * コンストラクタ
     * <p>将棋盤のサイズが指定される</p>
     * @since 2011/12/05
     */
    public SquareList() {
        this(SHOGI_BOARD_X_SIZE, SHOGI_BOARD_Y_SIZE);
    }

    /**
     * 四角形の集合のサイズを指定してインスタンス化
     * @param xSize 横サイズ
     * @param ySize 縦サイズ
     * @since 2012/01/20
     */
    public SquareList(int xSize, int ySize) {
        super();
        this.xSize = xSize;
        this.ySize = ySize;
        widthAve = 0;
        heightAve = 0;
        maxX = 0;
        minX = 100000;
        maxY = 0;
        minY = 100000;
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

        // x, yそれぞれの最大値，最小値を求める
        if (list.get(0).x() < minX) minX = list.get(0).x();
        if (list.get(0).y() < minY) minY = list.get(0).y();
        if (list.get(2).x() > maxX) maxX = list.get(2).x();
        if (list.get(2).y() > maxY) maxY = list.get(2).y();

        int i = 0;
        for (CvPoint p : list) {
            ret.position(i++).set(p);
        }
        return super.add(ret);
    }

    /**
     * 指定された場所に補完点を挿入する
     * @param index 挿入位置
     * @param e 補完点
     */
    public void complementAdd(int index, CvPoint e) {
        if (e == null) return;
        if (contains(e)) return;

        LOG.log(Level.INFO,
                "Insert a complementary square at index {0}.\n({1}, {2}), ({3}, {4}), ({5}, {6}), ({7}, {8})",
                new Object[]{index,
                e.position(0).x(), e.position(0).y(),
                e.position(1).x(), e.position(1).y(),
                e.position(2).x(), e.position(2).y(),
                e.position(3).x(), e.position(3).y()});
        complementedSquare.add(index);
        super.add(index, e);
    }

    /**
     * インスタンス化時に指定したサイズの四角形列へと正規化する
     * @since 2012/01/20
     */
    public void normalize() {
        sort();

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

        complement();
    }

    /**
     * 抽出矩形のリストを左上から右下方向へソートする
     * @since 2011/12/03
     */
    public void sort() {
        java.util.Collections.sort(this, new SquareComparator());
    }

    /**
     * 四角形を補完する
     * <p>縦横に決められた数だけ格子状に四角形が並んでいる時，リストに格納された値に抜けがあればそれを検出し，補完を行う．</p>
     * @param xSize 横サイズ
     * @param ySize 縦サイズ
     * @since 2012/01/20
     */
    public void complement() {
        // すでにすべての四角形を検出している場合はなにもしない
        if (super.size() >= xSize*ySize) return;

        LOG.log(Level.INFO, "Detected square num < {0}. Complement squares.", xSize*ySize);
        for (int i = 0; i < ySize; i++) {
            for (int j = 0; j < xSize-1;) {
                CvPoint a = super.get(i*ySize+j);

                // 各列のはじめのマス
                if (j == 0) {
                    if (i > 0) {
                        // j=0,つまり各行のはじめのマスを判定する際は一つ前の行のマスを参照してチェックを行う
                        CvPoint c = super.get((i-1)*ySize+j);
                        if (Math.abs(c.position(0).x() - a.x()) > widthAve/2) {    // 上のマス目のx座標との差が幅平均の半分よりも大きければ検出できていないと判定
                            LOG.log(Level.INFO, "Oversight has been detected at index {0}", (i*ySize+j));
                            CvPoint n = new CvPoint(4);
                            n.position(0).set(new CvPoint(c.position(0).x(), c.position(0).y()+(int)heightAve));
                            n.position(1).set(new CvPoint(c.position(1).x(), c.position(1).y()+(int)heightAve));
                            n.position(2).set(new CvPoint(c.position(2).x(), c.position(2).y()+(int)heightAve));
                            n.position(3).set(new CvPoint(c.position(3).x(), c.position(3).y()+(int)heightAve));
                            complementAdd(i*ySize+j, n);
                            a = n;
                        }
                    } else if (i == 0) {
                        // 一番最初のマス目の判定は最小のX, Y座標に近いかどうかで判定
                        CvPoint c = a.position(0);
                        if (Math.abs(c.x() - minX) > widthAve/2) {    // 最小のX座標との差がマスの幅平均の半分よりも大きければ検出できていないと判定
                            LOG.log(Level.INFO, "Oversight has been detected at index {0}", (i*ySize+j));
                            CvPoint n = new CvPoint(4);
                            n.position(0).set(new CvPoint(minX, minY));
                            n.position(1).set(new CvPoint(minX + (int)widthAve, minY));
                            n.position(2).set(new CvPoint(minX + (int)widthAve, minY + (int)heightAve));
                            n.position(3).set(new CvPoint(minX, minY + (int)heightAve));
                            complementAdd(i*ySize+j, n);
                            a = n;
                        }
                    }
                }

                CvPoint b;
                try {
                    b = super.get(i*ySize+(++j));
                } catch (IndexOutOfBoundsException ex) {
                    // 最後のマス目が未検出の場合
                    LOG.log(Level.INFO, "Oversight has been detected at index {0}", (i*ySize+j));
                    CvPoint n = new CvPoint(4);
                    n.position(0).set(new CvPoint(maxX-(int)widthAve, maxY-(int)heightAve));
                    n.position(1).set(new CvPoint(maxX, maxY-(int)heightAve));
                    n.position(2).set(new CvPoint(maxX, maxY));
                    n.position(3).set(new CvPoint(maxX-(int)widthAve, maxY));
                    complementAdd(i*ySize+j, n);
                    continue;
                }

                // 矩形aと矩形bとの間が平均サイズの半分以上離れている場合はその間が検出されていないとみなす
                if (b.position(0).x() - a.position(1).x() > widthAve/2) {
                    LOG.log(Level.INFO, "Oversight has been detected at index {0}-{1}", new Object[]{(i*ySize+j-1), (i*ySize+j)});

                    // もう一つ前の矩形との距離を求め，その距離分だけ離れた場所に矩形を追加
                    int offset = a.position(0).x() - super.get(i*ySize+j-2).position(1).x();
                    CvPoint n = new CvPoint(4);
                    n.position(0).set(new CvPoint(a.position(1).x()+offset, a.position(1).y()));
                    n.position(1).set(new CvPoint(a.position(1).x()+offset+(int)widthAve, b.position(0).y()));
                    n.position(2).set(new CvPoint(a.position(2).x()+offset+(int)widthAve, b.position(3).y()));
                    n.position(3).set(new CvPoint(a.position(2).x()+offset, a.position(2).y()));
                    complementAdd(i*ySize+j, n);
                }
            }
        }
    }

    /**
     * 補完前の要素数
     * @return 保管前の要素数
     */
    public int getDetectedNum() {
        return size()-complementedSquare.size();
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
     * 指定された色でindexの矩形を描画する
     * @param img 書き出し先のIplImage
     * @param index 書きだす矩形のindex
     * @param color 線の色
     * @since 2012/01/20
     */
    public void drawSquare(IplImage img, int index, CvScalar color) {
        if (color == null) {
            if (complementedSquare.contains(index)) color = CvScalar.YELLOW;
            else                                    color = CvScalar.GREEN;
        }

        CvPoint pt = get(index);
        for (int i = 0; i < 4; i++) {
            cvLine(img, new CvPoint(pt.position(i)), new CvPoint(pt.position((i+1)%4)), color, 2, CV_AA, 0);
        }
    }

    /**
     * 指定されたindexの矩形を描画する
     * @param img 書き出し先のIplImage
     * @param index 書きだす矩形のindex
     * @since 2011/12/05
     */
    public void drawSquare(IplImage img, int index) {
        drawSquare(img, index, null);
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
        sb.append(String.format("左上: (%d, %d) 右下: (%d, %d)\n", minX, minY, maxX, maxY));
        sb.append("補完マス: \n");
        for (Integer i : complementedSquare) {
            CvPoint p = super.get(i);
            sb.append(String.format("(%d, %d), (%d, %d), (%d, %d), (%d, %d)\n",
                p.position(0).x(), p.position(0).y(),
                p.position(1).x(), p.position(1).y(),
                p.position(2).x(), p.position(2).y(),
                p.position(3).x(), p.position(3).y()));
        }

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
