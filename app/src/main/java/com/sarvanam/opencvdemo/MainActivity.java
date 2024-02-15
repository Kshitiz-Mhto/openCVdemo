package com.sarvanam.opencvdemo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.Manifest;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.sarvanam.opencvdemo.data.AttendenceDatabase;
import com.sarvanam.opencvdemo.data.dao.FaceImageDao;
import com.sarvanam.opencvdemo.data.modal.FaceImage;
import com.sarvanam.opencvdemo.databinding.ActivityMainBinding;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final double SIMILARITY_THRESHOLD = 0.9;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier faceCascade;
    private Mat mRgba;
    private Mat mGray;
    private int cameraMode;
    AttendenceDatabase database;
    FaceImageDao dao;
    ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        initDatabaseRequirement();
        cameraMode = Camera.CameraInfo.CAMERA_FACING_BACK;
        if (!hasPermissions(PERMISSIONS)) {
            // Request permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSION);
        }else {
            if (OpenCVLoader.initDebug()) {
                faceCascade = new CascadeClassifier();
                try {
                    setFaceCascade();
                    initCamera();
                    Toast.makeText(getApplicationContext(),"OpenCV Successfully initialized",Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                binding.btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switchCamera();
                    }
                });
            } else {
                Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private void setFaceCascade() throws IOException {
        InputStream is = this.getResources().openRawResource(R.raw.frontal);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File mCascade = new File(cascadeDir, "frontal.xml");
        FileOutputStream os = new FileOutputStream(mCascade);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while((bytesRead = is.read(buffer))!= -1){
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        faceCascade = new CascadeClassifier(mCascade.getAbsolutePath());
    }
    private void initCamera() {
        mOpenCvCameraView = binding.camView;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setCameraIndex(cameraMode);
        mOpenCvCameraView.enableView();
    }

    private void initDatabaseRequirement(){
        database = AttendenceDatabase.getDatabase(getApplicationContext());
        dao = database.faceImageDao();
    }
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        // Perform face detection
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(mGray, faces);

        // If only one face is detected, capture and save the image
        if (faces.toArray().length == 1) {
            // Capture the image
            Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bitmap);

            // Convert bitmap to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] imageData = stream.toByteArray();

            // Save the image to Room database
            FaceImage faceImage = new FaceImage();
            faceImage.imageData = imageData;

            binding.btnCaptureImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    databaseExecutor.execute(() -> {
                        dao.insert(faceImage);
                    });
                }
            });
            databaseExecutor.execute(() -> {
                recognizeFace(imageData);
            });
        }

        // Get the orientation of the camera frame
        int orientation = inputFrame.rgba().rows() > inputFrame.rgba().cols() ? 90 : 0;

        // Draw bounding boxes around detected faces with increased size
        for (Rect rect : faces.toArray()) {
            // Increase the size of the rectangle
            int increaseSize = 20; // You can adjust this value to increase/decrease the size
            // Adjust coordinates based on orientation
            Rect adjustedRect;
            if (orientation == 90) {
                adjustedRect = new Rect((int) (rect.tl().y - increaseSize),
                        (int) (mRgba.cols() - rect.br().x - increaseSize),
                        rect.height + 2 * increaseSize,
                        rect.width + 2 * increaseSize);
            } else {
                adjustedRect = new Rect((int) (rect.tl().x - increaseSize),
                        (int) (rect.tl().y - increaseSize),
                        rect.width + 2 * increaseSize,
                        rect.height + 2 * increaseSize);
            }

            Imgproc.rectangle(mRgba, adjustedRect.tl(), adjustedRect.br(), new Scalar(255, 0, 0), 3);
        }

        return mRgba;
    }
    public void recognizeFace(byte[] capturedImageData) {
        // Retrieve all faces from the database
        List<FaceImage> faceImages = dao.getAllFaceImages();

        // Convert captured image data to a Bitmap
        Bitmap capturedBitmap = BitmapFactory.decodeByteArray(capturedImageData, 0, capturedImageData.length);

        // Convert the Bitmap to a Mat for processing
        Mat capturedMat = new Mat();
        Utils.bitmapToMat(capturedBitmap, capturedMat);

        // Perform face detection on the captured image
        MatOfRect capturedFaces = new MatOfRect();
        faceCascade.detectMultiScale(capturedMat, capturedFaces);

        // If a face is detected in the captured image
        if (capturedFaces.toArray().length > 0) {
            // Loop through all faces in the database for comparison
            for (FaceImage faceImage : faceImages) {
                // Convert the byte array from the database to a Bitmap
                Bitmap dbBitmap = BitmapFactory.decodeByteArray(faceImage.imageData, 0, faceImage.imageData.length);

                // Convert the Bitmap to a Mat for processing
                Mat dbMat = new Mat();
                Utils.bitmapToMat(dbBitmap, dbMat);

                // Perform face detection on the database image
                MatOfRect dbFaces = new MatOfRect();
                faceCascade.detectMultiScale(dbMat, dbFaces);

                // Compare the detected faces in the captured image with the faces in the database
                for (Rect capturedRect : capturedFaces.toArray()) {
                    for (Rect dbRect : dbFaces.toArray()) {
                        // Perform some comparison method here, e.g., comparing distances or using a recognition algorithm
                        double similarityScore = calculateSimilarity(capturedMat, capturedRect, dbMat, dbRect);

                        // If the similarity score is above a certain threshold, consider it a match
                        if (similarityScore > SIMILARITY_THRESHOLD) {
                            System.out.println("FaceRecognitionFacerecognized!");
                            return;
                        }
                    }
                }
            }
        }
        System.out.println("FaceRecognitionNomatchfound.");
    }

    private double calculateSimilarity(Mat capturedMat, Rect capturedRect, Mat dbMat, Rect dbRect) {
        // Extract the region of interest (ROI) for the captured face
        Mat capturedFace = new Mat(capturedMat, capturedRect);

        // Extract the region of interest (ROI) for the database face
        Mat dbFace = new Mat(dbMat, dbRect);

        // Convert the faces to grayscale
        Imgproc.cvtColor(capturedFace, capturedFace, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(dbFace, dbFace, Imgproc.COLOR_RGBA2GRAY);

        // Resize the faces to a fixed size
        Size size = new Size(128, 128);
        Imgproc.resize(capturedFace, capturedFace, size);
        Imgproc.resize(dbFace, dbFace, size);

        // Compute HOG descriptors for the faces
        MatOfFloat capturedDescriptors = new MatOfFloat();
        MatOfFloat dbDescriptors = new MatOfFloat();
        HOGDescriptor hog = new HOGDescriptor();
        hog.compute(capturedFace, capturedDescriptors);
        hog.compute(dbFace, dbDescriptors);

        // Perform SVM classification (or other classification method) using the HOG descriptors
        // Here you would use a trained SVM model or other classifier to classify the faces and return a similarity score
        // For simplicity, let's assume a dummy similarity score for demonstration purposes
        double similarityScore = 0.75; // Dummy score

        return similarityScore;
    }



    public void switchCamera() {
        cameraMode = (cameraMode + 1) % 2; // toggle between 0 and 1
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraMode);
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mOpenCvCameraView != null)
            switchCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Handle permission request response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSION);
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
