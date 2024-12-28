package com.app.autocrop;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements ObjectDetectorHelper.DetectorListener {
    public static final int OCR_KWH_RESULT_CODE = 666;
    public static final int OCR_KVAH_RESULT_CODE = 667;
    public static final int OCR_RMD_RESULT_CODE = 668;
    public static final int OCR_LT_RESULT_CODE = 669;
    //public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;
    String temp;
    private StringBuilder inputNumber = new StringBuilder();

    TextView lblStatus;
    public boolean editFlag = false, eFlag = false, meter_detect = false, check_meter_detect = false;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private ImageCapture imageCapture;
    private PreviewView previewView;
    private ImageView imageView;
    EditText txtResult;
    private Camera camera;
    private static final String TEMP_DIR_NAME = "spdcl";

    private static final String TAG = "Offline OCR";
    String textValue, valType, serviceId, RESULT_VALUE;
    ObjectDetectorHelper objectDetectorHelper;


    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblStatus = findViewById(R.id.lblStatus);
        previewView = findViewById(R.id.previewView);
        SeekBar zoomSlider = findViewById(R.id.zoomSlider);
        SeekBar exposureSlider = findViewById(R.id.exposureSlider);

        serviceId = getIntent().getStringExtra("SERVICE_ID") != null ? getIntent().getStringExtra("SERVICE_ID") : "default";
        valType = getIntent().getStringExtra("TYPE") != null ? getIntent().getStringExtra("TYPE") : "kWh";
        String temp = "Service:" + serviceId + "  Type: " + valType;
        lblStatus.setText(temp);

        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions already Granted");
            startCamera(zoomSlider, exposureSlider);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            startCamera(zoomSlider, exposureSlider);
        }


        try {
            objectDetectorHelper = new ObjectDetectorHelper(
                    0.35f,
                    ObjectDetectorHelper.MAX_RESULTS_DEFAULT,
                    ObjectDetectorHelper.DELEGATE_CPU,
                    "mds.tflite",
                    RunningMode.IMAGE,
                    getApplicationContext(),
                    this
            );
        } catch (IllegalArgumentException e) {
            Log.e("ObjectDetector", "Invalid argument: " + e.getMessage());
        } catch (Exception e) {
            Log.e("ObjectDetector", "Unexpected error: " + e.getMessage());
            lblStatus.setText(R.string.unexptected_error);
        }

    }

    private void startCamera(SeekBar zoomSlider, SeekBar exposureSlider) {
        // Bind the camera lifecycle
        ProcessCameraProvider.getInstance(this)
                .addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                        Preview preview = new Preview.Builder()
                                .build();

                        imageCapture = new ImageCapture.Builder()
                                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build();

                        CameraSelector cameraSelector = new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)  // Use the back camera
                                .build();

                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll();

                        // Bind use cases to the lifecycle
                        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                        // Set up camera controls
                        setupZoomControl(camera.getCameraControl(), zoomSlider);
                        setupExposureControl(camera.getCameraControl(), camera.getCameraInfo(), exposureSlider);

                        preview.setSurfaceProvider(previewView.getSurfaceProvider());


                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, ContextCompat.getMainExecutor(this));
    }


    private void setupZoomControl(CameraControl cameraControl, SeekBar zoomSlider) {
//        zoomSlider.setMax(10); // Max 10x zoom
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float zoomRatio = progress / 8.0f; // Convert progress to zoom ratio
                lblStatus.setText("Zoom:" + zoomRatio);
                cameraControl.setZoomRatio(zoomRatio);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupExposureControl(CameraControl cameraControl, CameraInfo cameraInfo, SeekBar exposureSlider) {
        int minExposure = cameraInfo.getExposureState().getExposureCompensationRange().getLower();
        int maxExposure = cameraInfo.getExposureState().getExposureCompensationRange().getUpper();

        exposureSlider.setMax(maxExposure - minExposure); // Difference determines SeekBar max
        exposureSlider.setProgress(-minExposure); // Default to middle position

        exposureSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int exposureValue = progress + minExposure; // Map progress to actual exposure range
                lblStatus.setText("Exposure:" + exposureValue);
                cameraControl.setExposureCompensationIndex(exposureValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void SendValues(Bitmap bitmap) {


        textValue = txtResult.getText().toString();

        String imagePath = createDirectoryAndSaveFile(bitmap, getImgFileName());

        Intent intent = new Intent();

        switch (valType) {
            case "KWH":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("KWH", textValue);
                if (editFlag) {
                    Log.d(TAG, "OCR value : Edited");
                    intent.putExtra("rFlag", "EDITED_KWH");

                } else {
                    Log.d(TAG, "OCR value : Extracted");

                    intent.putExtra("rFlag", "EXTRACTED_KWH");
                }
                break;
            case "KVAH":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("KVAH", textValue);
                if (editFlag) {
                    Log.d(TAG, "OCR value : Edited");
                    intent.putExtra("rFlag", "EDITED_KVAH");

                } else {
                    Log.d(TAG, "OCR value : Extracted");

                    intent.putExtra("rFlag", "EXTRACTED_KVAH");
                }
                break;
            case "RMD":
                intent.putExtra("RESULT_VALUE", imagePath);

                try {
                    double num = Double.parseDouble(textValue);
                    DecimalFormat df = new DecimalFormat("#.00");
                    String formattedNum = df.format(num);
                    intent.putExtra("RMD", formattedNum);

                } catch (NumberFormatException e) {
                    // p did not contain a valid double
                    intent.putExtra("RMD", "unknown");

                }
                if (editFlag) {
                    Log.d(TAG, "OCR value : Edited");
                    intent.putExtra("rFlag", "EDITED_RMD");

                } else {
                    Log.d(TAG, "OCR value : Extracted");

                    intent.putExtra("rFlag", "EXTRACTED_RMD");
                }

                break;
            case "LT":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("LT", textValue);
                break;
        }


//        if (editFlag) {
//            Log.d(TAG, "OCR value : Edited");
//            intent.putExtra("rFlag", "EDITED");
//
//        } else {
//            Log.d(TAG, "OCR value : Extracted");
//
//            intent.putExtra("rFlag", "EXTRACTED");
//        }


        switch (valType) {
            case "KWH":
                setResult(OCR_KWH_RESULT_CODE, intent);
                break;
            case "KVAH":
                setResult(OCR_KVAH_RESULT_CODE, intent);
                break;
            case "RMD":
                setResult(OCR_RMD_RESULT_CODE, intent);
                break;
            case "LT":
                setResult(OCR_LT_RESULT_CODE, intent);
                break;
        }


        finish();


    }

    private void toggleTorch() {
        if (camera != null) {
            CameraInfo cameraInfo = camera.getCameraInfo();
            CameraControl cameraControl = camera.getCameraControl();

            // Observe the current torch state
            Integer torchState = cameraInfo.getTorchState().getValue();
            if (torchState != null) {
                boolean isTorchOn = torchState == TorchState.ON; // Use TorchState.ON instead of a constant
                cameraControl.enableTorch(!isTorchOn); // Toggle torch state
            } else {
                Toast.makeText(this, "Torch state unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public String doInference(Bitmap bitmap) {
//        Bitmap bitmap = uriToBitmap(image_uri);
//        innerImage.setImageBitmap(bitmap);
        Bitmap mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBmp);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((mutableBmp.getWidth() / 95));

        Paint paintText = new Paint();
        paintText.setColor(Color.BLUE);
        paintText.setTextSize(mutableBmp.getWidth() / 10);

        List<DetectionData> detectionDataList = new ArrayList<>();

        ObjectDetectorHelper.ResultBundle resultBundle = objectDetectorHelper.detectImage(bitmap);
        if (resultBundle != null) {
            Log.d("tryRess", "results are not null");
            List<ObjectDetectionResult> resultList = resultBundle.getResults();
            Log.d(TAG, resultList.toString());
            for (ObjectDetectionResult singleResult : resultList) {
                List<Detection> detectionList = singleResult.detections();
                for (Detection detection : detectionList) {
                    float confidence = 0;
                    String objectName = "";
                    for (Category category : detection.categories()) {
                        if (category.score() > confidence) {
                            confidence = category.score();
                            objectName = category.categoryName();

                            if (category.categoryName().contentEquals("reading")) {
                                String displayScore = "Offline OCR (" + category.score() + " %)";
                                //txtTitle.setText(displayScore);
                            }
                        }
                    }
                    canvas.drawRect(detection.boundingBox(), paint);
                    canvas.drawText(objectName, detection.boundingBox().left, detection.boundingBox().top, paintText);
                    Log.d(TAG, objectName);
                    Log.d(TAG, String.valueOf(detection.boundingBox().left));
                    // Add detection data to the list
                    DetectionData data = new DetectionData(detection.boundingBox().left, objectName);
                    detectionDataList.add(data);

                    Collections.sort(detectionDataList, new Comparator<DetectionData>() {
                        @Override
                        public int compare(DetectionData o1, DetectionData o2) {
                            return Float.compare(o1.getLeft(), o2.getLeft());
                        }
                    });

                }
            }
            // image.setImageBitmap(mutableBmp);

            //imageView.setImageBitmap(mutableBmp);

            StringBuilder kwh = new StringBuilder();

            Log.d(TAG, "Sorted Values are:......");
            // Assuming detectionDataList is already sorted
            for (DetectionData data : detectionDataList) {
                Log.d(TAG, "ObjectName: " + data.getObjectName() + ", Left: " + data.getLeft());


                if (data.getObjectName().contentEquals("kwh")) {
                    Log.d(TAG, "Kwh removed");
                } else if (data.getObjectName().contentEquals("-")) {
                    kwh.append(".");
                } else {
                    kwh.append(data.getObjectName());
                }

            }

            textValue = "";
            Log.d(TAG, String.valueOf(kwh));

            textValue = kwh.toString();


            // txtResult.setText(kwh);

        } else {
            Log.d("tryRess", "results are null");
        }
        return textValue;
    }


    public Bitmap cropToAspectRatio(Bitmap originalBitmap, float aspectRatio) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // Calculate the new dimensions
        int cropWidth, cropHeight;
        if (originalWidth / (float) originalHeight > aspectRatio) {
            // Width is the limiting factor
            cropHeight = originalHeight;
            cropWidth = (int) (cropHeight * aspectRatio);
        } else {
            // Height is the limiting factor
            cropWidth = originalWidth;
            cropHeight = (int) (cropWidth / aspectRatio);
        }

        // Calculate the top-left coordinates to center the crop
        int cropLeft = (originalWidth - cropWidth) / 2;
        int cropTop = (originalHeight - cropHeight) / 2;

        // Crop the bitmap
        return Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight);
    }

    private void stopCamera() throws ExecutionException, InterruptedException {
        ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();  // Unbind all active camera use cases
        }

    }

    public String getImgFileName() {

        switch (valType) {
            case "KWH":
                return serviceId + "-kwh.jpg";
            case "KVAH":
                return serviceId + "-kvah.jpg";
            case "RMD":
                return serviceId + "-rmb.jpg";
            default:
                return "default.jpg";
        }

    }

    private String createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {


        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File tempDir = new File(root, TEMP_DIR_NAME);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                Log.e(TAG, "Failed to create temp directory: " + tempDir.getAbsolutePath());
                return null;
            }
        }


        File file = new File(tempDir, fileName);


        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.toString();
    }

    private Bitmap cropAndResizeImage(Bitmap originalBitmap) {
        // Define the rectangle area (left, top, right, bottom) you want to crop
        int left = 50;   // example x coordinate of the top-left corner
        int top = 50;    // example y coordinate of the top-left corner
        int right = originalBitmap.getWidth() - 50;  // example x coordinate of the bottom-right corner
        int bottom = originalBitmap.getHeight() / 2; // example y coordinate of the bottom-right corner

        return Bitmap.createScaledBitmap(Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top), 640, 640, true);


    }

    private void takePicture() {
        // Create a file to save the image
        File photoFile = new File(getExternalFilesDir(null), new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");

        OutputFileOptions options = new OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                bitmap = rotateBitmap(bitmap, GetAngle());
                showCapturedImageInDialog(cropAndResizeImage(bitmap));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(MainActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public Bitmap takeScreenshot(View view) {
        // Create a bitmap object to store the screenshot
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a canvas with the bitmap
        Canvas canvas = new Canvas(bitmap);

        // Draw the view's content onto the canvas
        view.draw(canvas);

        return bitmap;
    }


    private Bitmap rotateBitmap(Bitmap originalBitmap, float angle) {
        // Create a Matrix object to hold the rotation transformation
        Matrix matrix = new Matrix();

        // Apply the rotation transformation to the matrix
        matrix.postRotate(angle);

        // Create a new Bitmap by applying the matrix transformation to the original Bitmap

        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
    }

    private int GetAngle() {

        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);

        return (sharedPreferences.getInt("angle", 90));
    }

    // Java
    int dpToPx(int dp, Context context) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private void showCapturedImageInDialog(Bitmap imageBitmap) {
        // Create a custom dialog with an ImageView to show the captured image
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_view, null);
        builder.setView(dialogView);

        // Find ImageView in the dialog layout
        ImageView imageView = dialogView.findViewById(R.id.image);

        textValue = "";
        String temp = doInference(imageBitmap);

        txtResult = dialogView.findViewById(R.id.textResult);

        txtResult.setOnClickListener(v -> {
//            if (image_count >= 3 && meter_detect) {
//                //showNumericDialog();
//                showNumericBottomDialog();
//
//            }
            editFlag = true;
            showNumericBottomDialog();
        });

        txtResult.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                // Perform action when Enter key is pressed
                hideKeyboard();
                return true;
            }
            return false;
        });
        txtResult.setText(temp);

        //createDirectoryAndSaveFile(imageBitmap, getImgFileName());

        // Java
//        int dynamicWidth = dpToPx(300, this); // 100dp
//        int dynamicHeight = dpToPx(100, this); // 150dp
//
//
//        ViewGroup.LayoutParams params = imageView.getLayoutParams();
//        params.width = dynamicWidth;
//        params.height = dynamicHeight;
//        imageView.setLayoutParams(params);

        // Set the captured image to the ImageView
        imageView.setImageBitmap(imageBitmap);

        imageView.setOnClickListener(v -> {
            imageView.setImageBitmap(RotateByAngle(imageBitmap));

        });

        // Optionally, add "OK" button to dismiss the dialog
        builder.setPositiveButton("OK", (dialog, which) -> SendValues(imageBitmap));
        builder.setNegativeButton("Recapture", (dialog, which) -> dialog.dismiss());

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(txtResult.getWindowToken(), 0);
        }
    }

    void SaveAngle(int Angle) {
        // Save value
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("rotate", "true");
        editor.putInt("angle", Angle);
        editor.apply();

    }

    public Bitmap RotateByAngle(Bitmap bitmap) {


        int Angle = GetAngle();

        Log.d(TAG, "Saved Angle:" + Angle);


        Angle = Angle + 90;

        if (Angle > 360) {
            Angle = 0;
        } else {
            bitmap = rotateBitmap(bitmap, 90);

        }

        SaveAngle(Angle);
       temp = String.valueOf(Angle);
//        txtStatus.setText(myString);
        return bitmap;

    }


    private void showNumericBottomDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_numeric_keyboard, null);

        // Initialize BottomSheetDialog with the custom view
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();

        // Set up display TextView to show entered numbers
        TextView displayText = dialogView.findViewById(R.id.txtTitle);

        // Numeric buttons logic
        View.OnClickListener numberButtonListener = view -> {
            Button button = (Button) view;
            inputNumber.append(button.getText().toString());
            displayText.setText(inputNumber.toString());
        };

        // Assign listener to each number button
        dialogView.findViewById(R.id.button_0).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_1).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_2).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_3).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_4).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_5).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_6).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_7).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_8).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_9).setOnClickListener(numberButtonListener);
        dialogView.findViewById(R.id.button_decimal).setOnClickListener(numberButtonListener);

        // Clear button logic
        dialogView.findViewById(R.id.button_clear).setOnClickListener(v -> {
            inputNumber.setLength(0);
            displayText.setText("");
        });

        // Enter button logic
        dialogView.findViewById(R.id.button_enter).setOnClickListener(v -> {
            txtResult.setText(inputNumber.toString());
            // Process the entered value
            bottomSheetDialog.dismiss();
        });

    }

    // Handle permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // startCamera(zoomSlider, exposureSlider);
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Capture image on button click
    public void captureButtonClick(View view) {
        takePicture();
    }

    public void flashButtonClick(View view) {
        toggleTorch();
    }


    @Override
    public void onError(String var1, int var2) {

    }

    @Override
    public void onResults(ObjectDetectorHelper.ResultBundle var1) {

    }
}
