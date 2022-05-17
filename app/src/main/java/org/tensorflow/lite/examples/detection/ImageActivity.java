package org.tensorflow.lite.examples.detection;

import static android.os.Environment.DIRECTORY_DCIM;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Locale;


public class ImageActivity extends AppCompatActivity {

    private static final String TAG = "AndroidOpenCv";
    private static final int REQ_CODE_SELECT_IMAGE = 100;
    private Bitmap mInputImage;
    private Bitmap mOriginalImage;
    private Bitmap mDesImage;
    private ImageView mImageView;
    private ImageView mEdgeImageView;
    private ImageView pickImageView;
    private TextView matchText;
    private boolean mIsOpenCVReady = false;
    private static final int REQUEST_IMAGE_CAPTURE = 672;
    private String imageFilePath;
    private Uri photoUri;

    private MediaScanner mMediaScanner; // 사진 저장 시 갤러리 폴더에 바로 반영사항을 업데이트 시켜주려면 이 것이 필요하다(미디어 스캐닝)



    public native String orbFeatureJNI2(long inputImage, long outputImage);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }



    public void orbFeatureUsingJNI() {
        if (!mIsOpenCVReady) {
            return;
        }
        Mat src = new Mat();
        Utils.bitmapToMat(mInputImage, src);
        Mat dst = new Mat();
        Utils.bitmapToMat(mDesImage, dst);

        String a = orbFeatureJNI2(src.getNativeObjAddr(), dst.getNativeObjAddr());
        matchText.setText(a);
        Utils.matToBitmap(dst, mDesImage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image);
        mImageView = findViewById(R.id.origin_iv);
        pickImageView = findViewById(R.id.pivk_iv);
        matchText = findViewById(R.id.matchText);
        mMediaScanner = MediaScanner.getInstance(getApplicationContext());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mIsOpenCVReady = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    String path = getImagePathFromURI(data.getData());
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    mOriginalImage = BitmapFactory.decodeFile(path, options);
                    mInputImage = BitmapFactory.decodeFile(path, options);
                    if (mInputImage != null) {
                        mImageView.setImageBitmap(mOriginalImage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else if(requestCode ==0 && resultCode ==RESULT_OK){    //카메라로 찍어온것.
            Bundle extras = data.getExtras();
            mDesImage =  (Bitmap) extras.get("data");
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            pickImageView.setImageBitmap(mDesImage);

            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

                    + "/test"; //파일 위치

            File f = new File(path);
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase(Locale.US).endsWith(".jpg"); //확장자
                }
            });


            orbFeatureUsingJNI();
        }
    }

    public void onDestroy() {
        super.onDestroy();

        mInputImage.recycle();
        if (mInputImage != null) {
            mInputImage = null;
        }
    }

    public void onButtonClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //Toast myToast = Toast.makeText(this.getApplicationContext(), "여기가 언제 실행되는 onButtonClicked",Toast.LENGTH_LONG);
        //myToast.show();
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
    }



    public void onPickButtonClicked(View view){
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera,0);
    }


    // permission
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE"};


    private boolean hasPermissions(String[] permissions) {
        int result;
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public String getImagePathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String imgPath = cursor.getString(idx);
            cursor.close();
            return imgPath;
        }
    }

    // permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("실행을 위해 권한 허가가 필요합니다.");
                }
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(ImageActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


    public static Uri getMediaUriFromPath(Context context, String filePath) {
        File imageFile = new File(filePath);
        Cursor cursor = context.getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ", new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor .getColumnIndex(MediaStore.MediaColumns._ID)); Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) { ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath); return context.getContentResolver().insert( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else { return null; }
        }
    }

}