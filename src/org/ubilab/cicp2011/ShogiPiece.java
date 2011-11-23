package org.ubilab.cicp2011;

import java.io.Serializable;

/**
 * 将棋の駒を表すクラス
 * @author atsushi-o
 * @since 2011/11/22
 */
public class ShogiPiece implements Serializable {
    /**
     * 将棋駒の種類を表す列挙型
     * <br>
     * <pre>
     * | 0| 1| 2| 3| 4|
     * | 5| 6| 7| 8| 9|
     * |10|11|駒|11|12|
     * |13|14|15|16|17|
     * |18|19|20|21|22|
     * </pre>
     * @author atsushi-o
     * @since 2011/11/22
     */
    public enum ShogiPieceType {
        /**
         * 王将，玉将
         */
        GYOKU   ("玉", "", (1<<6|1<<7|1<<8|1<<11|1<<12|1<<15|1<<16|1<<17), 0, false),
        /**
         * 金将
         */
        KIN     ("金", "", (1<<6|1<<7|1<<8|1<<11|1<<12|1<<16), 0, false),
        /**
         * 銀将
         */
        GIN     ("銀", "成銀", (1<<6|1<<7|1<<8|1<<15|1<<17), (1<<11|1<<12|1<<15|1<<16|1<<17), true),
        /**
         * 桂馬
         */
        KEI     ("桂", "成桂", (1<<1|1<<3), (1<<1|1<<3|1<<6|1<<7|1<<8|1<<11|1<<12|1<<16), true),
        /**
         * 香車
         */
        KYO     ("香", "成香", (1<<2|1<<7), (1<<2|1<<6|1<<8|1<<11|1<<12|1<<16), true),
        /**
         * 歩兵
         */
        FU      ("歩", "と", (1<<7), (1<<6|1<<8|1<<11|1<<12|1<<16), true),
        /**
         * 飛車
         */
        HISHA   ("飛", "竜", (1<<2|1<<7|1<<10|1<<11|1<<12|1<<13|1<<16|1<<21), (1<<6|1<<8|1<<15|1<<17), true),
        /**
         * 角行
         */
        KAKU    ("角", "馬", (1<<0|1<<4|1<<6|1<<8|1<<15|1<<17|1<<19|1<<23), (1<<7|1<<11|1<<12|1<<16), true);
        
        private final String chara;
        private final String promoteChara;
        private final int move;
        private final int promote;
        private final boolean promotable;
        
        private static final int[][] flagTable;
        
        static {
            flagTable = new int[5][5];
            int count = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    if (i == 2 && j == 2) {
                        flagTable[i][j] = 0;
                    } else {
                        flagTable[i][j] = 1<<count;
                        count++;
                    }
                }
            }
        }
        
        /**
         * 将棋駒の種類を表す列挙型を生成
         * @param move 通常時の駒の移動可能マス
         * @param promote 成時の駒の移動可能マスのxor
         * @param promotable 成れる駒かどうか
         * @since 2011/11/23
         */
        ShogiPieceType(String name, String promoteName, int move, int promote, boolean promotable) {
            this.chara = name;
            this.promoteChara = promoteName;
            this.move = move;
            this.promote = move ^ promote;
            this.promotable = promotable;
        }
        
        /**
         * 駒の通常時の移動可能マスを表すビット列を返す
         * @return 駒の通常時の移動可能マスを表すビット列
         * @since 2011/11/22
         */
        public int move()               { return move; };
        /**
         * 駒の成時の移動可能マスを表すビット列を返す
         * <br>
         * 成れない駒の場合は0を返す
         * @return 駒の成時の移動可能マスを表すビット列
         * @since 2011/11/22
         */
        public int promoteMove()        { return promote; };
        /**
         * 駒が成れるかどうか
         * @return 成れる場合はtrue, そうでない場合はfalse
         * @since 2011/11/22
         */
        public boolean isPromotable()   { return promotable; };
        /**
         * 駒の名前を返す
         * @param p 成っているかどうか
         * @return 駒の名前
         */
        public String getCharacter(boolean p) { return p?promoteChara:chara; }
        
        /**
         * 駒が与えられた場所から場所へ移動できるかどうかを判定する
         * <br>
         * 判定は，縦の位置は数が少ないほうが前方であるとして判定する
         * @param src 駒の現在位置
         * @param dst 駒の移動予定位置
         * @param promote 駒が成っているかどうか
         * @return 駒が移動できるか否か
         * @since 2011/11/22
         */
        public boolean isMovable(ShogiPos src, ShogiPos dst, boolean promote) {
            int flag = promote?this.promote:this.move;
            ShogiPos diff = src.diff(dst);
            
            // 座標の差が3以上の場合はその方向へ移動可能か判定
            if (Math.abs(diff.x()) >= 3 || Math.abs(diff.y()) >= 3) {
                // 両方3以上の場合（角行・竜馬）
                if (Math.abs(diff.x()) >= 3 && Math.abs(diff.y()) >= 3) {
                    return (this == ShogiPieceType.KAKU);
                }
                // どちらか一方が3以上の場合（飛車・竜王・香車）
                else {
                    if (this == ShogiPieceType.KYO)
                        return diff.y()<0?true:false;
                    else if (this == ShogiPieceType.HISHA)
                        return true;
                    else
                        return false;
                }
            }
            
            // それ以外の場合はフラグテーブルを使ってビット列と単純比較
            return ((flag & flagTable[diff.x()+2][diff.y()+2]) > 0)? true:false;
        }
    }
     
    private ShogiPieceType type;
    private boolean isPromote;
    private ShogiPlayer player;
    
    /**
     * 将棋駒をひとつ生成
     * @param type 駒の種類
     * @param player 駒の所有プレーヤ
     * @since 2011/11/23
     */
    public ShogiPiece(ShogiPieceType type, ShogiPlayer player) {
        this.type = type;
        this.isPromote = false;
        this.player = player;
    }
    
    /**
     * 駒の種類を取得
     * @return 駒の種類
     * @since 2011/11/23
     */
    public ShogiPieceType getType() { return this.type; }
    /**
     * 成っているかどうかを取得
     * @return 成っているかどうか
     * @since 2011/11/23
     */
    public boolean isPromote()      { return this.isPromote; }
    /**
     * 駒の所有プレーヤを取得
     * @return 駒の所有プレーヤ
     * @since 2011/11/23
     */
    public ShogiPlayer getPlayer()          { return this.player; }
    /**
     * 駒の所有プレーヤを変更
     * @param p 変更するプレーヤ
     * @since 2011/11/23
     */
    public void setPlayer(ShogiPlayer p)    { this.player = p; }
    /**
     * 駒の名前を取得
     * @return 駒の名前
     * @since 2011/11/23
     */
    public String getCharacter()    { return type.getCharacter(isPromote); }
    /**
     * 成る
     * @since 2011/11/23
     */
    public void promote()           { this.isPromote = true; }
}
