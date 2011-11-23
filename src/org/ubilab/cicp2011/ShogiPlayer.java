package org.ubilab.cicp2011;

/**
 * 将棋のプレーヤを表す列挙型
 * @author atsushi-o
 * @since 2011/11/23
 */
public enum ShogiPlayer {
    /**
     * 先手
     */
    SENTE   ("▲"),
    /**
     * 後手
     */
    GOTE    ("△");

    private final String character;

    private ShogiPlayer(String chara) {
        this.character = chara;
    }

    /**
     * プレーヤを表す記号を返す
     * @return プレーヤを表す記号
     * @since 2011/11/23
     */
    public String getCharacter() { return this.character; }
    
    /**
     * プレーヤの記号をShogiPlayerに変換する
     * @param str プレーヤの記号
     * @return 対応するShogiPlayer
     * @throws IllegalArgumentException 正しくない記号が与えられた場合
     */
    public static ShogiPlayer parse(final String str) {
        for (ShogiPlayer s : values()) {
            if (s.getCharacter().equals(str)) return s;
        }
        
        throw new IllegalArgumentException();
    }
}
