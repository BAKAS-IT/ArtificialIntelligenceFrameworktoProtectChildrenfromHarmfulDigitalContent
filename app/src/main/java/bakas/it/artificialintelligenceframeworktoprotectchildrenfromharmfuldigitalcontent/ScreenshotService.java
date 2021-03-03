package bakas.it.artificialintelligenceframeworktoprotectchildrenfromharmfuldigitalcontent;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ScreenshotService extends Service {

    int mWidth ;//Screen width
    int mHeight ;//Screen height
    int mDensity ;//Screen density
    ImageReader mImageReader;//Image reader
    MediaProjection mProjection;//Media Projection variable for screenshot
    Handler screenshotHandler = new Handler();//Timer for screenshot timed to 10 sec
    private final IBinder mBinder = new LocalBinder();//Gets current service object

    public ScreenshotService() {//Empty constructor
    }

    //Returns current service object
    public class LocalBinder extends Binder {
        public ScreenshotService getService() {
            return ScreenshotService.this;
        }
    }
    //On service bound
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    /*public String readData() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return "hata";
        }
        mBluetoothGatt.readCharacteristic(mWDataCharacteristic);
        return okunanData;
    }
*/
    //Starts auto screenshot and takes a screenshot every 10 secs
    public boolean initialize(MediaProjection mProjection) {

        this.mProjection=mProjection;//Currently running media projection

        screenshotHandler.postDelayed(new Runnable() {//10 sec timer for screenshot
            @Override
            public void run() {
                startScreenshot();//Start taking screenshots
                screenshotHandler.postDelayed(this,10000);//creating loop with 10 secs delay
            }
        }, 10000);//10 secs delay

        return true;
    }

    //Gets a bitmap and compressing it to JPG and saving
    public void saveBitmap(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();//External Storage Path
        File myDir = new File(root + "/Parental_Control_Screenshots");//Adding our folder to path
        myDir.mkdirs();//Creating our folder if doesn't exist

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//Getting timestamp
        String fname = "Screenshot_"+ timeStamp +".jpg";//File name

        File file = new File(myDir, fname);//Creating file
        if (file.exists()) file.delete ();//Overwriting file if file already exist
        try {//Compress bitmap and write to file
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Gets media projection and placing it on image reader then saving it
    @SuppressLint("WrongConstant")
    public void startScreenshot(){
        final MediaProjection Projection=mProjection;//copying the projection
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);//Getting device window manager
        Display display = wm.getDefaultDisplay();//Display variable
        final DisplayMetrics metrics = new DisplayMetrics();//Metric values of current display
        display.getMetrics(metrics);//Getting metrics
        Point size = new Point();//Point variable to get real size
        display.getRealSize(size);//getting sizes of screen
        mWidth = size.x;//screen width
        mHeight = size.y;//screen height
        mDensity = metrics.densityDpi;//screen density

        mImageReader= ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);//Image reader to get image from media projection

        final Handler handler = new Handler();//Handler

        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;//Projection flags
        Projection.createVirtualDisplay("screen-mirror", mWidth, mHeight, mDensity, flags, mImageReader.getSurface(), null, handler);//Creating virtual display and saving it to mImageReader

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {//Saving an image on image eradder after calculations
            @Override
            public void onImageAvailable(ImageReader reader) {
                reader.setOnImageAvailableListener(null, handler);//Setting listener to null

                Image image = reader.acquireLatestImage();//Creating image

                final Image.Plane[] planes = image.getPlanes();//Planes of image
                final ByteBuffer buffer = planes[0].getBuffer();//Byte buffer that will be a bitmap

                //bitmap sizes calculations
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * metrics.widthPixels;
                // create bitmap
                Bitmap bmp = Bitmap.createBitmap(metrics.widthPixels + (int) ((float) rowPadding / (float) pixelStride), metrics.heightPixels, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);//getting pixels from buffer to bitmap

                image.close();
                reader.close();

                Bitmap realSizeBitmap = Bitmap.createBitmap(bmp, 0, 0, metrics.widthPixels, bmp.getHeight());//Getting real size bitmap
                bmp.recycle();

                saveBitmap(realSizeBitmap);//Saving bitmap
            }
        }, handler);
    }

    //On stop button pressed it stops screen capturing and drops screenshotHandler timer
    public void stopScreenshot(){
        screenshotHandler.removeCallbacksAndMessages(null);
        mProjection.stop();//Stopping screen capture
    }
}