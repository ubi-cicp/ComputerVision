package org.ubilab.cicp2011.cv;

import org.ubilab.cicp2011.*;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

/**
 * Coreとの通信用クラス
 * @author atsushi-o
 * @since 2011/11/22
 */
public class CommCore extends Thread {
    private ServerSocket sock;
    private boolean runnable;
    private ShogiMove move;
    
    /**
     * ポート番号を指定してインスタンスを生成
     * @param port 待受ポート番号
     * @throws IOException ソケットを開いているときに入出力エラーが発生した場合
     * @since 2011/11/22
     */
    public CommCore(int port) throws IOException {
        super();
        sock = new ServerSocket(port);
    }
    
    /**
     * Coreへ送信するShogiMoveインスタンスをセットする
     * @param move Coreへ送信するShogiMoveインスタンス
     * @since 2011/11/23
     */
    public synchronized void setMove(ShogiMove move) {
        this.move = move;
    }
    
    @Override
    public void start() {
        runnable = true;
        super.start();
    }
    
    /**
     * 通信待受を停止する
     * @since 2011/11/22
     */
    public synchronized void halt() {
        runnable = false;
    }
    
    @Override
    public void run() {
        Socket s = null;
        while (runnable) {
            try {
                s = sock.accept();
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                String data = (String)ois.readObject();
                
                synchronized(this) {
                    try {
                        // 盤面情報取得完了まで待つ
                        wait();
                        
                        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                        if (move != null) {
                            oos.writeObject(move);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println(e);
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
        
        try {
            sock.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
