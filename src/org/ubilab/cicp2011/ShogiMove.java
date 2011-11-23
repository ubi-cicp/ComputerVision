package org.ubilab.cicp2011;

import java.io.Serializable;

/**
 * 将棋駒の移動軌跡を表すクラス
 * @author atsushi-o
 */
public class ShogiMove implements Serializable {
    private ShogiPiece piece;
    private ShogiPos dst;
    private boolean promote;
    
    /**
     * 移動軌跡を新規作成
     * @param src 移動対象駒
     * @param dst 移動先
     * @param promote 移動先で成るかどうか
     * @since 2011/11/23
     */
    public ShogiMove(ShogiPiece src, ShogiPos dst, boolean promote) {
        this.piece = src;
        this.dst = dst;
        this.promote = promote;
    }
    
    /**
     * 棋譜形式の文字列で出力
     * @return 棋譜形式の文字列
     * @since 2011/11/23
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(piece.getPlayer().getCharacter());
        sb.append(dst.toString());
        sb.append(piece.getType().getCharacter(piece.isPromote()|promote));
        sb.append(promote?"成":"不成");
        return sb.toString();
    }
}
