package com.sarvanam.opencvdemo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.Manifest;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import com.sarvanam.opencvdemo.databinding.ActivityMainBinding;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier faceCascade;
    private Mat mRgba;
    private Mat mGray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        if (!hasPermissions(PERMISSIONS)) {
            // Request permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSION);
        }else {
            if (OpenCVLoader.initDebug()) {
                Toast.makeText(getApplicationContext(),"OpenCV Successfully initialized",Toast.LENGTH_SHORT).show();
                faceCascade = new CascadeClassifier();
                try {
                    setFaceCascade();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                initCamera();
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
        mOpenCvCameraView.setCameraIndex(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mOpenCvCameraView.enableView();
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

        // Draw bounding boxes around detected faces with increased size
        for (Rect rect : faces.toArray()) {
            int increaseSize = 20;
            Rect enlargedRect = new Rect((int) (rect.tl().x - increaseSize),
                    (int) (rect.tl().y - increaseSize),
                    rect.width + 2 * increaseSize,
                    rect.height + 2 * increaseSize);
            Imgproc.rectangle(mRgba, enlargedRect.tl(), enlargedRect.br(), new Scalar(255, 0, 0), 3);
        }

        return mRgba;
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
            mOpenCvCameraView.enableView();
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
