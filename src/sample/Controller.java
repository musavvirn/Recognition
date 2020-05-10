package sample;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    private final String HAAR_PATH = "D:/Coding/GitHub/Recognition/src/resources/haarcascades/haarcascade_frontalface_alt.xml";
    private final String LBP_PATH = "D:/Coding/GitHub/Recognition/src/resources/lbpcascades/lbpcascade_frontalface.xml";

    private CascadeClassifier faceCascade = new CascadeClassifier();
    private int absoluteFaceSize = 0;
    Rect[] facesArray;


    @FXML
    private Button detect_btn;
    private boolean detectorON = false;

    @FXML
    private Button camera_btn;
    private boolean cameraActive = false;

    @FXML
    private Label label_box;
    private boolean labelActive = false;


    @FXML
    private ImageView currentFrame;
    private VideoCapture capture = new VideoCapture();
    private ScheduledExecutorService timer;
    private static int cameraId = 0;

    private void loadCascade()
    {
        // load the classifier(s)
        this.faceCascade.load(HAAR_PATH);

    }

    @FXML
    protected void detectObj() {
        this.detectorON = !this.detectorON;
        if (this.detectorON) this.loadCascade();
        this.grabframe();
        this.changeDetectBtnLabel();

    }



    private void changeDetectBtnLabel() {
        this.detect_btn.setText((this.detectorON) ? "Detector OFF" : "Detector ON");
    }

    private void changeCameraBtnLabel() {
        this.camera_btn.setText((this.cameraActive) ? "Stop Camera" : "Start Camera");
    }


    @FXML
    protected void markObj() {

    }

    @FXML
    protected void getLabels() {
        this.labelActive = !this.labelActive;
        if (!this.labelActive) {
            this.label_box.setText(null);
        }

    }


    @FXML
    protected void startCamera(ActionEvent event) {
        if (!this.cameraActive) {
            this.capture.open(cameraId);

            if (this.capture.isOpened()) {
                this.cameraActive = true;
                this.changeCameraBtnLabel();

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



            } else {
                System.err.println("Impossible to open camera...");
            }
        } else {
            this.cameraActive = false;
            this.changeCameraBtnLabel();
            this.stopAcquisition();
        }


    }

    private void detectAndDisplay(Mat frame)
    {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces & puts each face coordinates of rectangle in list
        // rectangle coordinates consists of two (x,y) points (of opposite vertices)
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());



        // each rectangle in faces is a face: draw them!
        this.facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(255, 0, 0), 3);

            //System.out.println("Coordinates: " + facesArray[i].tl() + ", " + facesArray[i].br());


        }

        // display face coordinates as text on label_box
        // runLater: to do with current thread
        if (this.labelActive) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    String text = "";
                    for (int i=0; i<facesArray.length; i++) {
                        text += facesArray[i].tl() + ", " + facesArray[i].br() + " ";
                    }

                    label_box.setText(text);
                }
            });
        }



    }

    /* Set cascade classifier path to either Haar or LBP in /resources */



    private Mat grabframe() {
        Mat frame = new Mat();

        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
                if (!frame.empty()) {
                    if (this.detectorON) {
                        this.detectAndDisplay(frame);

                    }
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
            this.cameraActive = false;

        }
    }

    protected void setClosed() {
        this.stopAcquisition();
    }
}
