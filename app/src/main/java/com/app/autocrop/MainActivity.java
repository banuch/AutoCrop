package com.app.autocrop;

import android.Manifest;
import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ObjectDetectorHelper.DetectorListener {

    public static final int OCR_KWH_RESULT_CODE = 666;
    public static final int OCR_KVAH_RESULT_CODE = 667;
    public static final int OCR_RMD_RESULT_CODE = 668;
    public static final int OCR_LT_RESULT_CODE = 669;
    //public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;

    private static final String TEMP_DIR_NAME = "spdcl";

    private static final int REQUEST_CODE_PERMISSIONS = 10;


    ImageView image;
    TextView txtStatus,txtTitle;
    private Uri image_uri;
    Bitmap bitmap;

    int image_count = 0;

    Toolbar myToolbar;
    ObjectDetectorHelper objectDetectorHelper;
    EditText txtResult;

    private StringBuilder inputNumber = new StringBuilder();
    Button btnCamera, btnOK, btnRecap;
    // Declare the ActivityResultLauncher
    private ActivityResultLauncher<Intent> startActivityForResult;
    public boolean editFlag = false, eFlag = false, meter_detect = false;

    String imagePath, textValue, valType, serviceId, RESULT_VALUE;

    private static final String TAG = "Offline OCR";

    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions already Granted");
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


        imagePath = getIntent().getStringExtra("ImagePath");
        serviceId = getIntent().getStringExtra("SERVICE_ID");
        RESULT_VALUE = getIntent().getStringExtra("RESULT_VALUE");
        valType = getIntent().getStringExtra("TYPE");


        String appData = "Service id: " + serviceId + "\n";
        appData = appData + " Value Type: " + valType;


        image = findViewById(R.id.imgCapture);
        txtResult = findViewById(R.id.textResult);
        txtTitle=findViewById(R.id.txtTitle);
        txtResult.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        btnOK = findViewById(R.id.btn_ok);
//        btnRecap = findViewById(R.id.btn_recp);
        btnCamera = findViewById(R.id.btn_camera);
        txtStatus = findViewById(R.id.txtStatus);

//        myToolbar = findViewById(R.id.my_toolbar);
//        myToolbar.setTitle("Offline OCR");

        SeekBar seekBar = findViewById(R.id.seekBar);
        btnOK.setEnabled(true);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;


        int newWidth = (int) (width * 0.60);
        int newHeight = (int) (height * 0.36);


        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(newWidth, newHeight);
        image.setLayoutParams(layoutParams);

        image.requestLayout();


        // seekBar.setVisibility(View.INVISIBLE);


        seekBar.setProgress(20);

        seekBar.setScaleY(2.0f);


        txtStatus.setText(appData);


        txtResult.setText("");

        txtResult.setOnClickListener(v -> {
            if (image_count >= 3 && meter_detect) {
                //showNumericDialog();
                showNumericBottomDialog();

            }
        });

        txtResult.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                // Perform action when Enter key is pressed
                hideKeyboard();
                return true;
            }
            return false;
        });

        //txtStatus.setText(String.valueOf(GetAngle()));

        if (imagePath != null) {
            bitmap = BitmapFactory.decodeFile(imagePath);
            SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
            // Retrieve value
            String value = sharedPreferences.getString("rotate", null);

            if (Objects.equals(value, "true")) {

                Rotate(GetAngle());

                Log.d(TAG, "image rotated at Camera2 API");
            }

            image.setImageBitmap(bitmap);
            doInference();


        }

        image.setOnClickListener(v -> {
            RotateByAngle();
        });


//        btnOK.setOnClickListener(v -> Callback());
        btnOK.setOnClickListener(v -> SendValues());
//        btnRecap.setOnClickListener(v -> callMainScreen());
        btnCamera.setOnClickListener(v -> openCamera());


        // Show the keyboard when the EditText is focused
        txtResult.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    Log.d(TAG, "Focus Event fired");
                    ShowKeyboard(txtResult);
                }
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Handle progress change

                int val = seekBar.getProgress();

                float f = (float) val / 100;

                txtStatus.setText(String.format("Confidence Level: %s%%", val));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Handle the start of tracking touch
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


                if (bitmap != null) {

                    if (meter_detect) {

                        // Handle the stop of tracking touch
                        int val = seekBar.getProgress();
                        float f = (float) val / 100;
                        txtStatus.setText(String.format("Confidence Level: %s%%", val));

                        objectDetectorHelper.setCurrentModel("mds.tflite");
                        objectDetectorHelper.setThreshold(f);
                        objectDetectorHelper.setupObjectDetector();
                        Log.d(TAG, "offline_ocr model Selected ...");
                        doInference();
                        btnOK.setEnabled(true);
                    } else {
                        txtStatus.setText(R.string.no_meter_detected_on_image);
                    }
                } else {
                    txtStatus.setText(R.string.load_image_first);
                }


            }
        });

        objectDetectorHelper = new ObjectDetectorHelper(0.89f, ObjectDetectorHelper.MAX_RESULTS_DEFAULT, ObjectDetectorHelper.DELEGATE_CPU, "meter_detect.tflite", RunningMode.IMAGE, getApplicationContext(), this);

        if (bitmap != null) {
            doInference();
            btnOK.setEnabled(true);
        }

        // Initialize the ActivityResultLauncher
        startActivityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();

                        if (data != null) {
                            bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                        }

                        Rotate(GetAngle());

                        image.setImageBitmap(bitmap);

                        txtResult.setText("");
                        objectDetectorHelper.setCurrentModel("meter_detect.tflite");
                        objectDetectorHelper.setThreshold(0.70f);
                        objectDetectorHelper.setupObjectDetector();

                        doInference();

                        if (txtResult.getText().toString().contentEquals("reading")) {
                            meter_detect = true;
                            txtResult.setText("");
                            objectDetectorHelper.setCurrentModel("mds.tflite");

                            if (image_count == 1) {
                                objectDetectorHelper.setThreshold(0.4f);
                            } else if (image_count == 2) {
                                objectDetectorHelper.setThreshold(0.35f);
                            } else if (image_count == 3) {
                                objectDetectorHelper.setThreshold(0.30f);
                            } else {
                                objectDetectorHelper.setThreshold(0.4f);
                            }

                            objectDetectorHelper.setupObjectDetector();
                            txtStatus.setText(R.string.meter_detected);
                            doInference();

                            image_count = image_count + 1;
                            String temp=txtStatus.getText().toString();
                            temp=temp+"( Image count: "+image_count+" )";
                            txtStatus.setText(temp);
                            if (image_count >= 3) {
                                //showNumericDialog();
                                showNumericBottomDialog();

                            }

                        } else {
                            txtStatus.setText(R.string.no_meter_found);
                            meter_detect = false;
                        }

                    }
                }
        );


    }

    void Rotate(int Angle) {
        bitmap = rotateBitmap(bitmap, Angle);
        image.setImageBitmap(bitmap);

    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(txtResult.getWindowToken(), 0);
        }
    }

    public void ShowKeyboard(EditText editText) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }

    }

    public void enableEditing() {

        if (!eFlag) {
            try {
                Log.d(TAG, "Editing Enabled");

                txtResult.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                txtResult.setFocusable(true);
                txtResult.setFocusableInTouchMode(true);
                txtResult.setCursorVisible(true);
                txtResult.setClickable(true);
                txtResult.requestFocus();

                editFlag = true;
                //Toast.makeText(MainActivity.this, "Editing Enabled", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Unable to Enable Editing", e);
            }
        }

//
//        if (!eFlag) {
//            try {
//                Log.d(TAG, "Editing Enabled");
//                txtResult.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
//                txtResult.setFocusable(true);
//                txtResult.setFocusableInTouchMode(true);
//                txtResult.setCursorVisible(true);
//                txtResult.setClickable(true);
//                txtResult.requestFocus(); // Optional, sets focus to the EditText
//                editFlag = true;
//                eFlag = true;
//                Toast.makeText(MainActivity.this, "Edited Enabled", Toast.LENGTH_SHORT).show();
//
//            } catch (Exception e) {
//
//                System.out.println(e.toString());
//                Log.d(TAG, "Unable to Enable Editing");
//            }
//        }


    }

    int GetAngle() {
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);

        return (sharedPreferences.getInt("angle", 0));
    }


    private void openCamera() {

        meter_detect = false;
        txtTitle.setText("Offline OCR");
        txtResult.setText("");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult.launch(cameraIntent);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
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

    void SaveAngle(int Angle) {
        // Save value
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("rotate", "true");
        editor.putInt("angle", Angle);
        editor.apply();

    }

    ;

    public void RotateByAngle() {


        int Angle = GetAngle();

        Log.d(TAG, "Saved Angle:" + Angle);


        Angle = Angle + 90;

        if (Angle > 360) {
            Angle = 0;
        } else {
            bitmap = rotateBitmap(bitmap, 90);
            image.setImageBitmap(bitmap);
        }

        SaveAngle(Angle);
        String myString = String.valueOf(Angle);
        txtStatus.setText(myString);


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
    private void showNumericDialog() {
        // Inflate custom layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_numeric_keyboard, null);



        // Initialize AlertDialog with custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;

        dialog.show();



        Log.d(TAG,"Gravity Set Bottom");

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
            dialog.dismiss();
        });
    }

    public void callMainScreen() {
        Intent intent = new Intent(getApplicationContext(), Camera.class);
        startActivity(intent);

    }

    public String getImgFileName() {

        if (valType.equals("KWH")) {
            return serviceId + "-kwh.jpg";
        } else if (valType.equals("KVAH")) {
            return serviceId + "-kvah.jpg";
        } else if (valType.equals("RMD")) {
            return serviceId + "-rmb.jpg";
        } else {
            return "default.jpg";
        }

    }

    void Callback() {

        textValue = txtResult.getText().toString();

        String imagePath = createDirectoryAndSaveFile(bitmap, getImgFileName());

        Intent intent = new Intent();
        intent.putExtra("RESULT_VALUE", imagePath);
        intent.putExtra("KWH", textValue);
        intent.putExtra("rFlag", "EXTRACTED");
        setResult(OCR_KWH_RESULT_CODE, intent);

        finish();
        // Display success message
        Toast.makeText(MainActivity.this, "Data Received", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Value: " + textValue + " imagepath: " + imagePath);
    }

    public void SendValues() {


        textValue = txtResult.getText().toString();

        String imagePath = createDirectoryAndSaveFile(bitmap, getImgFileName());

        Intent intent = new Intent();

        switch (valType) {
            case "KWH":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("KWH", textValue);
                break;
            case "KVAH":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("KVAH", textValue);
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

                break;
            case "LT":
                intent.putExtra("RESULT_VALUE", imagePath);
                intent.putExtra("LT", textValue);
                break;
        }


        if (editFlag) {
            Log.d(TAG, "OCR value : Edited");
            intent.putExtra("rFlag", "EDITED");

        } else {
            Log.d(TAG, "OCR value : Extracted");

            intent.putExtra("rFlag", "EXTRACTED");
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


    public Bitmap rotateBitmap(Bitmap sourceBitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
    }

    @Override
    public void onError(String var1, int var2) {

    }

    @Override
    public void onResults(ObjectDetectorHelper.ResultBundle var1) {

    }


    public void doInference() {
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
                                txtTitle.setText(displayScore);
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
            image.setImageBitmap(mutableBmp);

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


            Log.d(TAG, String.valueOf(kwh));

            txtResult.setText(kwh);

        } else {
            Log.d("tryRess", "results are null");
        }
    }

}