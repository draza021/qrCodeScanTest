package com.example.drago.barcodescanner.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;

import com.example.drago.barcodescanner.R;
import com.example.drago.barcodescanner.events.PhotoTakenEvent;
import com.example.drago.barcodescanner.model.Constants;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import butterknife.ButterKnife;

public class TakePhotoActivity extends AppCompatActivity {
    private static final String TAG = "TakePhotoActivity";
    private static final int TAKE_PHOTO_REQUEST = 1;
     private static final String ACTION_SEND_TO_AMAZON = "send_to_amazon";

    private String mCurrentPhotoPath;
    private String mImageName;
    private boolean mSendToAmazon = false;
    // Standard storage location for digital camera files
    private static final String CAMERA_DIR = "/dcim/";

    public File getAlbumStorageDir(String albumName) {
        return new File (
                Environment.getExternalStorageDirectory()
                        + CAMERA_DIR
                        + albumName
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                try {
                    mSendToAmazon = Boolean.valueOf(extras.getString(ACTION_SEND_TO_AMAZON));
                } catch (Exception e) {}
            }
        }

        setContentView(R.layout.activity_take_photo);
        ButterKnife.bind(this);
        onTakePhoto();
    }

    @Override
    public void onBackPressed() {
        checkBack();
    }

    private void checkBack() {
        // fire empty images event
        //EventBus.getDefault().postSticky(new PhotoTakenEvent());
        finish();
    }

    private void onTakePhoto() {
        startTakingPhoto();
    }

    private String getAlbumName() {
        return "barcodeScannerAlbum";
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.e("BarcodeScanner", "failed to create directory");
                        return null;
                    }
                }
            }
        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        File albumF = getAlbumDir();
        File imageF = new File(albumF, mImageName + Constants.IMAGE_TYPE_SUFFIX);
        return imageF;
    }

    /** creates image file in album by setname (uuid) **/
    public File setUpPhotoFile() throws IOException {
        return createImageFile();
    }

    // instead, now annotation is getting all images even the first one
    private void startTakingPhoto() {
        CreateFileTask createFileTask = new CreateFileTask();
        mImageName = UUID.randomUUID().toString();
        createFileTask.execute();
    }

    private void takePhotoWithPermission(File f) {
        if (f != null) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            mCurrentPhotoPath = f.getAbsolutePath();

            // for N
            //Uri photoURI = FileProvider.getUriForFile(this,
            //        getApplicationContext().getPackageName() + ".provider", f);
            //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST);
        } else {
            Log.e(TAG, "tandara mandara");
        }
    }

    private class CreateFileTask extends AsyncTask<Void, Void, File> {
        @Override
        protected File doInBackground(Void... nothing) {
            File file;
            try {
                file = setUpPhotoFile();
            } catch (IOException e) {
                //Crashlytics.logException(new Exception("TakePhotoActivity.CreateFileTask, file = null !"));
                mCurrentPhotoPath = null;
                e.printStackTrace();
                file = null;
            }
            return file;
        }
        @Override
        protected void onPostExecute(File result) {
            takePhotoWithPermission(result);
        }
    }

    private long findMediaId(String imageName){
        final String[] columns = { MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.TITLE
        };
        imageName += Constants.IMAGE_TYPE_SUFFIX;
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                MediaStore.Images.Media.DISPLAY_NAME + "=\"" + imageName+"\"",
                null,
                null);
        cursor.moveToFirst();
        long id = -1;
        if(cursor.getCount()>0){
            id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
        }
        cursor.close();
        return id;
    }

    /** adding to media gallery **/
    private void galleryAddPic(String path, final String imageName) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(path);
        Uri contentUri = Uri.fromFile(file);
        // for N
        //Uri contentUri = FileProvider.getUriForFile(this,
        //        getApplicationContext().getPackageName() + ".provider", file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        MediaScannerConnection.scanFile(
                getApplicationContext(),
                new String[]{file.getAbsolutePath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path1, Uri uri) {
                        long id = TakePhotoActivity.this.findMediaId(imageName);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PHOTO_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (mCurrentPhotoPath != null) {
                    galleryAddPic(mCurrentPhotoPath, mImageName);
                    EventBus.getDefault().postSticky(new PhotoTakenEvent(mImageName, mCurrentPhotoPath, null, mSendToAmazon));
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                    bitmap = Bitmap.createBitmap(bitmap);
                    decodeBarCode(bitmap);
                }
            } else {
                EventBus.getDefault().postSticky(new PhotoTakenEvent());
            }
        }
    }

    private void openWebPage(String urlString) {

    }

    private void decodeBarCode(Bitmap myBitmap) {
        BarcodeDetector detector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                        .build();
        if(!detector.isOperational()){
            //txtView.setText("Could not set up the detector!");
            return;
        } else {
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);
            if (barcodes.size() > 0) {
                Log.e(TAG, "success!");
                Barcode thisCode = barcodes.valueAt(0);
                openWebPage(thisCode.rawValue);
                finish();
            } else {
                Log.e(TAG, "need to take more photos");
                onTakePhoto();
            }
        }
    }

}

