/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ubilab.cicp2011.cv;

import javax.swing.JFrame;
import com.googlecode.javacv.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * テスト用のクラス
 * @author atsushi-o
 * @since 2011/11/17
 */
public class test {
    public static void main(String[] args) {
        CvCapture capture;
        IplImage capFrame;
        
        capture = cvCreateCameraCapture(0);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH, 1280);
        cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT, 960);
        
        CanvasFrame camera = new CanvasFrame("camera");
        camera.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        while(true) {
            capFrame = cvQueryFrame(capture);
            camera.showImage(capFrame);
        }
    }
}
