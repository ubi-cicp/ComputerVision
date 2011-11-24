package org.ubilab.cicp2011.cv;

import org.ubilab.cicp2011.*;
import java.util.logging.Logger;
import java.util.logging.Level;
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
    
    private static final Logger logger;
    
    static {
        logger = Logger.getLogger(CommCore.class.getName());
    }
    
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
        logger.log(Level.INFO, "Set ShogiDiff data: {0}", move);
        this.move = move;
    }
    
    @Override
    public void start() {
        logger.info("Start listening socket");
        runnable = true;
        super.start();
    }
    
    /**
     * 通信待受を停止する
     * @since 2011/11/22
     */
    public synchronized void halt() {
        logger.info("Stop listening socket");
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
                logger.log(Level.INFO, "Object recieved: {0}", data);
                
                synchronized(this) {
                    try {
                        // 盤面情報取得完了まで待つ
                        logger.info("Thread wait until complete the localization process");
                        wait();
                        
                        logger.info("Thread resumed");
                        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                        if (move != null) {
                            oos.writeObject(move);
                            logger.log(Level.INFO, "Object sended: {0}", move);
                        }
                    } catch (InterruptedException e) {
                        logger.log(Level.INFO, "Interrupt occurred.", e);
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, null, e);
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Closing Socket failed.", e);
                }
            }
        }
        
        try {
            sock.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Closing ServerSocket failed.", e);
        }
    }
}
