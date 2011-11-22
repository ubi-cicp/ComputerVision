package org.ubilab.cicp2011;

import java.io.Serializable;

/**
 * 将棋盤上の位置情報を表すクラス
 * <br>
 * 座標系は棋譜と同じく，右上を起点としたものとする．
 * @author atsushi-o
 * @since 2011/11/23
 */
public class ShogiPos implements Serializable {
    private int x;
    private int y;
    
    /**
     * 漢数字とアラビア数字を相互変換する列挙型
     * @author atsushi-o
     * @since 2011/11/23
     */
    public enum KanSuji {
        一 (1),
        二 (2),
        三 (3),
        四 (4),
        五 (5),
        六 (6),
        七 (7),
        八 (8),
        九 (9),
        〇 (0);
        
        private final int arabicNum;
        
        KanSuji(int arabic) {
            this.arabicNum = arabic;
        }
        
        /**
         * 漢数字をアラビア数字に変換する
         * @return アラビア数字
         * @since 2011/11/23
         */
        public int toArabic() { return this.arabicNum; }
        
        /**
         * アラビア数字を漢数字に変換する
         * @param arabic 変換するアラビア数字
         * @return 漢数字
         * @throws IllegalArgumentException 1桁以上の数字が入力された場合
         * @since 2011/11/23
         */
        public static String toKanSuji(final int arabic) {
            return KanSuji.valueOf(arabic).name();
        }
        
        /**
         * アラビア数字に対応するKanSujiを返す
         * @param arabic 変換するアラビア数字
         * @return 対応するKanSuji
         * @throws IllegalArgumentException 1桁以上の数字が入力された場合
         * @since 2011/11/23
         */
        public static KanSuji valueOf(final int arabic) {
            for (KanSuji k : values()) {
                if (k.toArabic() == arabic) {
                    return k;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    /**
     * 将棋盤上の位置情報を表すクラスを生成する
     * @param x 横方向座標
     * @param y 縦方向座標
     * @since 2011/11/23
     */
    public ShogiPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * 横方向の座標を返す
     * @return 横方向の座標
     * @since 2011/11/23
     */
    public int x() { return x; }
    /**
     * 縦方向の座標を返す
     * @return 縦方向の座標
     * @since 2011/11/23
     */
    public int y() { return y; }
    
    /**
     * 座標との差を求める
     * <br>
     * pos - this
     * @param pos 比較対象の座標
     * @return 差を表すShogiPos
     * @since 2011/11/23
     */
    public ShogiPos diff(ShogiPos pos) {
        int x = pos.x() - this.x;
        int y = pos.y() - this.y;
        return ShogiPos.valueOf(x, y);
    }

    /**
     * int型の座標情報からShogiPosを生成する
     * @param x 横方向の座標
     * @param y 縦方向の座標
     * @return 与えられた座標を表すShogiPos
     * @since 2011/11/23
     */
    public static ShogiPos valueOf(int x, int y) {
        return new ShogiPos(x, y);
    }
    
    /**
     * 棋譜形式の座標文字列をShogiPosに変換
     * <br>
     * 1文字目は半角数字，2文字目は漢数字で表す形式
     * @param pos 棋譜形式の座標文字列
     * @return 与えられた座標のShogiPos
     * @throws NumberFormatException 文字列が2文字でない場合
     * @throws IllegalArgumentException 与えられた文字列が正しい座標文字列でなかった場合
     * @since 2011/11/23
     */
    public static ShogiPos valueOf(String pos) throws NumberFormatException, IllegalArgumentException {
        // 与えられた文字列が2文字でないならエラー
        if (pos.length() != 2) throw new NumberFormatException();
        
        // 1文字目を解析
        int x = Integer.parseInt(""+pos.charAt(0));
        if (x < 1 || x > 9) throw new IllegalArgumentException();
        
        // 2文字目を解析
        KanSuji y = KanSuji.valueOf(""+pos.charAt(1));
        if (y == KanSuji.〇) throw new IllegalArgumentException();
        
        // 結果を返す
        return new ShogiPos(x, y.toArabic());
    }
    
    /**
     * 棋譜形式の座標文字列に変換する
     * @return 棋譜形式の座標文字列
     * @since 2011/11/23
     */
    @Override
    public String toString() {
        return String.valueOf(x)+KanSuji.toKanSuji(y);
    }
}
