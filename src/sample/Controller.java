package sample;

import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {

    @FXML
    private Button button;
    @FXML
    private ImageView currentFrame;
    private VideoCapture capture = new VideoCapture();
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private static int cameraId = 0;

    @FXML
    protected void detectObj() {

    }
    @FXML
    protected void markObj() {

    }

    @FXML
    protected void getDetails() {

    }


    @FXML
    protected void startCamera(ActionEvent event) {
        if (!this.cameraActive) {
            this.capture.open(cameraId);

            if (this.capture.isOpened()) {
                this.cameraActive = true;

                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        Mat frame = grabframe();
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(currentFrame, imageToShow);

                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
                this.button.setLabel("Stop Camera");

            } else {
                System.err.println("Impossible to open camera...");
            }
        } else {
            this.cameraActive = false;
            this.button.setLabel("Start Camera");
            this.stopAcquisition();
        }


    }

    private Mat grabframe() {
        Mat frame = new Mat();

        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
                if (!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BayerRG2GRAY);

                }

            } catch (Exception e) {
                System.err.println("Exception: Image elaboration.." + e);
            }
        }

        return frame;
    }

    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                System.err.println("Exception in stopping frame capture.. " + e);
            }
        }

        if (this.capture.isOpened()) {
            this.capture.release();
        }
    }

    protected void setClosed() {
        this.stopAcquisition();
    }
}
