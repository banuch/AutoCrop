package com.app.autocrop;

import static com.app.autocrop.MyUtl.createDirectoryAndSaveFile;
import static com.app.autocrop.MyUtl.cropAndResizeImage;
import static com.app.autocrop.MyUtl.getImageRotation;
import static com.app.autocrop.MyUtl.getImgFileName;
import static com.app.autocrop.MyUtl.rotateBitmap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
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
import android.widget.ToggleButton;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements ObjectDetectorHelper.DetectorListener {
    public static final int OCR_KWH_RESULT_CODE = 666;
    public static final int OCR_KVAH_RESULT_CODE = 667;
    public static final int OCR_RMD_RESULT_CODE = 668;
    public static final int OCR_LT_RESULT_CODE = 669;


    private static final String API_URL = "https://detect.roboflow.com/ocr_20_06_23-mkcpy/2?api_key=poc5LCfNbKUKhaWCfCpD";
    private static final MediaType MEDIA_TYPE = MediaType.get("application/x-www-form-urlencoded");

    private RectangleOverlay rectangleOverlay;
    String temp;
    private StringBuilder inputNumber = new StringBuilder();
    TextView lblStatus, lblTitile;
    public boolean editFlag = false, eFlag = false, meter_detect = false, check_meter_detect = false;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private ImageCapture imageCapture;
    private PreviewView previewView;

    EditText txtResult;
    private Camera camera;
    private ToggleButton toggleButton;
    private static final String TEMP_DIR_NAME = "spdcl";

    private static final String TAG = "Offline OCR";
    String textValue, valType, serviceId, RESULT_VALUE;
    ObjectDetectorHelper objectDetectorHelper;
    Boolean isOnline = false;


    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        toggleButton = findViewById(R.id.toggleButton);
//
//        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                // Toast.makeText(this, "Toggle is ON", Toast.LENGTH_SHORT).show();
//                isOnline = true;
//            } else {
//                //Toast.makeText(this, "Toggle is OFF", Toast.LENGTH_SHORT).show();
//                isOnline = false;
//            }
//        });


        // Set the allowed date range
        String startDate = "2025-02-01"; // Format: yyyy-MM-dd
        String endDate = "2025-02-28";

        if (MyUtl.isWithinDateRange(startDate, endDate)) {
            runStartCode();
        } else {
            lblTitile = findViewById(R.id.lblTitle);
            lblTitile.setText(String.format("%s\n\n Activation expired", getVersionName()));
        }


    }

    private void sendImageToAPI(String base64Image, EditText txtResult) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(base64Image, MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray predictions = jsonObject.getJSONArray("predictions");

                            // Convert JSONArray to ArrayList for sorting and filtering
                            ArrayList<JSONObject> predictionList = new ArrayList<>();
                            for (int i = 0; i < predictions.length(); i++) {
                                JSONObject prediction = predictions.getJSONObject(i);
                                if (!"kwh".equals(prediction.getString("class"))) {
                                    predictionList.add(prediction);
                                }
                            }

                            // Sort the predictions by x value
                            Collections.sort(predictionList, Comparator.comparingDouble(o -> {
                                try {
                                    return o.getDouble("x");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    return 0;
                                }
                            }));

                            StringBuilder resultText = new StringBuilder();
                            for (JSONObject prediction : predictionList) {
                                String detectedClass = prediction.getString("class");
                                double confidence = prediction.getDouble("confidence");
//                                resultText.append("Text: ").append(detectedClass)
//                                        .append(", Confidence: ").append(confidence).append("\n");
                                resultText.append("").append(detectedClass);


                            }
                            //lblTitile.setText(resultText.toString());
                            txtResult.setText(resultText.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    private void runStartCode() {

        lblStatus = findViewById(R.id.lblStatus);
        lblTitile = findViewById(R.id.lblTitle);
        previewView = findViewById(R.id.previewView);

        lblTitile.setText(getVersionName());

        SeekBar zoomSlider = findViewById(R.id.zoomSlider);
        SeekBar exposureSlider = findViewById(R.id.exposureSlider);

        serviceId = getIntent().getStringExtra("SERVICE_ID") != null ? getIntent().getStringExtra("SERVICE_ID") : "default";
        valType = getIntent().getStringExtra("TYPE") != null ? getIntent().getStringExtra("TYPE") : "kWh";
        String temp = "Service No:" + serviceId + "  Type: " + valType;
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

    private void stopCamera() {
        try {
            // Get the instance of ProcessCameraProvider
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

            // Unbind all use cases to stop the camera
            cameraProvider.unbindAll();

            // Optional: Provide feedback that the camera has stopped
            Toast.makeText(this, "Camera preview stopped.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Handle exceptions gracefully
            Toast.makeText(this, "Error stopping camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("CameraX", "Error stopping camera", e);
        }
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

        String imagePath = createDirectoryAndSaveFile(bitmap, getImgFileName(valType, serviceId));

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
                if (editFlag) {
                    Log.d(TAG, "OCR value : Edited");
                    intent.putExtra("rFlag", "EDITED_LT");

                } else {
                    Log.d(TAG, "OCR value : Extracted");

                    intent.putExtra("rFlag", "EXTRACTED_LT");
                }
                break;
        }


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

    private void takePicture() {
        // Create a file to save the image
        File photoFile = new File(getExternalFilesDir(null), new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");

        OutputFileOptions options = new OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                String imagePath = photoFile.getAbsolutePath();
                int rotation = getImageRotation(imagePath);
                System.out.println("Image rotation: " + rotation + " degrees");

                Bitmap rotateBitmap = rotateBitmap(bitmap, rotation);


                showCapturedImageInDialog(cropAndResizeImage(rotateBitmap, isOnline));

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(MainActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }


    // Combine captured image and overlay
//    private Bitmap combineBitmaps(Bitmap baseBitmap, Bitmap overlayBitmap) {
//        // Create a new bitmap with the same dimensions as the base (captured) bitmap
//        Bitmap combinedBitmap = Bitmap.createBitmap(baseBitmap.getWidth(), baseBitmap.getHeight(), baseBitmap.getConfig());
//        Canvas canvas = new Canvas(combinedBitmap);
//
//        // Draw the base (captured) bitmap
//        canvas.drawBitmap(baseBitmap, 0, 0, null);
//
//        // Scale the overlay to fit the base bitmap
//        float scaleX = (float) baseBitmap.getWidth() / overlayBitmap.getWidth();
//        float scaleY = (float) baseBitmap.getHeight() / overlayBitmap.getHeight();
//        Matrix scaleMatrix = new Matrix();
//        scaleMatrix.setScale(scaleX, scaleY);
//
//        // Apply the scaling matrix to the overlay bitmap
//        Bitmap scaledOverlay = Bitmap.createBitmap(overlayBitmap, 0, 0, overlayBitmap.getWidth(), overlayBitmap.getHeight(), scaleMatrix, true);
//
//        // Draw the scaled overlay bitmap on top of the captured bitmap
//        canvas.drawBitmap(scaledOverlay, 0, 0, null);
//
//        return combinedBitmap;
//    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void captureImageWithOverlay() {
        // Create a bitmap from the camera preview
        Bitmap bitmap = previewView.getBitmap();
        if (bitmap != null) {
            // Create a mutable bitmap
            Bitmap combinedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(combinedBitmap);

            // Draw the camera preview on the combined bitmap
            canvas.drawBitmap(bitmap, 0, 0, null);

            // Draw the overlay (rectangle)
            rectangleOverlay.draw(canvas);

            // Save the combined image
            String imagePath = createDirectoryAndSaveFile(combinedBitmap, getImgFileName(valType, serviceId));
        }
    }


//
//    private Bitmap rotateBitmap(Bitmap originalBitmap, float angle) {
//        // Create a Matrix object to hold the rotation transformation
//        Matrix matrix = new Matrix();
//
//        // Apply the rotation transformation to the matrix
//        matrix.postRotate(angle);
//
//        // Create a new Bitmap by applying the matrix transformation to the original Bitmap
//
//        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
//    }

    private int GetAngle() {

        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);

        return (sharedPreferences.getInt("angle", 90));
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


        txtResult = dialogView.findViewById(R.id.textResult);




        if (isOnline) {
            txtResult.setText(R.string.please_wait);
            sendImageToAPI(bitmapToBase64(imageBitmap), txtResult);
        } else {

            String temp = doInference(imageBitmap);
            txtResult.setText(temp);

        }


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


        // Set the captured image to the ImageView
        imageView.setImageBitmap(imageBitmap);

        imageView.setOnClickListener(v -> {
            imageView.setImageBitmap(RotateByAngle(imageBitmap));

        });

        // Optionally, add "OK" button to dismiss the dialog
        builder.setPositiveButton("Recapture", (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton("ok", (dialog, which) -> SendValues(imageBitmap));

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

    private String getVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return "Ebilly OCR ( Ver: " + packageInfo.versionName + " )";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Version not found";
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
            //  bitmap = rotateBitmap(bitmap, 90);

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

    private void takeScreenshot() {
        // Get the root view of the activity
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);

        // Create a bitmap of the root view
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);

        // Save the screenshot
        String imagePath = createDirectoryAndSaveFile(bitmap, getImgFileName(valType, serviceId));
        // saveBitmap(bitmap);
    }

    private void saveBitmap(Bitmap bitmap) {
        Locale locale = Locale.US; // or Locale.ENGLISH
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(new Date());

        String fileName = "Screenshot_" + timestamp + ".png";

        File directory = new File(Environment.getExternalStorageDirectory() + "/Screenshots");
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
        }

        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(this, "Screenshot saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving screenshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Capture image on button click
    public void captureButtonClick(View view) {
        takePicture();
        //takeScreenshot();
    }

    public void flashButtonClick(View view) {
        // stopCamera();
        toggleTorch();
    }

    @Override
    public void onError(String var1, int var2) {

    }

    @Override
    public void onResults(ObjectDetectorHelper.ResultBundle var1) {

    }
}
