package com.google.samples.cronet_sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Fa extends Activity {

    private static final String TAG = "sanbo.Fa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        // When debugging, the net log (https://www.chromium.org/developers/design-documents/network-stack/netlog)
//        // is an extremely useful tool to figure out what's going on in the network stack. However,
//        // because it's a JSON file, it's quite sensitive to correct formatting,so we must ensure
//        // that it's always closed properly.
//        startNetLog();
//        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
//            stopNetLog();
//        });
        setContentView(R.layout.image_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    loadImg();
                } catch (Throwable e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    private void loadImg() {

        // UrlRequest and UrlRequest.Callback are the core of Cronet operations. UrlRequest is used
        // to issue requests, UrlRequest.Callback specifies how the application reacts to the server
        // responses.

        // Set up a callback which, on a successful read of the entire response, interprets
        // the response body as an image. By default, Cronet reads the body in small parts, having
        // the full body as a byte array is application specific logic. For more details about
        // the callbacks please see implementation of ReadToMemoryCronetCallback.
        CronetCallback callback = new CronetCallback() {
            @Override
            void onSucceeded(UrlRequest request, UrlResponseInfo info, byte[] bodyBytes,
                             long latencyNanos) {
                // Contribute the request latency
//                onCronetImageLoadSuccessful(latencyNanos);
                Log.i(TAG, "callback  onSuccessed latencyNanos:" + latencyNanos);

                // Send image to layout
                final Bitmap bimage = BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.length);
                runOnUiThread(() -> {
                    setImageBitmap(bimage);
                });
            }
        };
        ExecutorService ex = Executors.newSingleThreadExecutor();
        // The URL request builder allows you to customize the request.
        UrlRequest.Builder builder = getCronetEngine()
                .newUrlRequestBuilder(
                        getImage(),
                        callback,
                        ex)
//                        cronetApplication.getCronetCallbackExecutorService())
                // You can set arbitrary headers as needed
                .addHeader("x-my-custom-header", "Hello-from-Cronet")
                // Cronet supports QoS if you specify request priorities
                .setPriority(UrlRequest.Builder.REQUEST_PRIORITY_IDLE);

        // Start the request
        builder.build().start();
    }

    private void setImageBitmap(Bitmap bimage) {
        ImageView iv = (ImageView) findViewById(R.id.cronet_image);
        iv.getLayoutParams().height = bimage.getHeight();
        iv.getLayoutParams().width = bimage.getWidth();
        Log.i(TAG, "image:" + iv);
        Log.i(TAG, "bimage:" + bimage);
        iv.setImageBitmap(bimage);
    }


    /**
     * Method to start NetLog to log Cronet events.
     * Find more info about Netlog here:
     * https://www.chromium.org/developers/design-documents/network-stack/netlog
     */
    private void startNetLog() {
        File outputFile;
        try {
            outputFile = File.createTempFile("cronet", "log",
                    this.getExternalFilesDir(null));
            getCronetEngine().startNetLogToFile(outputFile.toString(), false);
        } catch (Exception e) {
            android.util.Log.e(TAG, e.toString());
        }
    }

    /**
     * Method to properly stop NetLog
     */
    private void stopNetLog() {
        getCronetEngine().stopNetLog();
    }

    private CronetEngine getCronetEngine() {
        return getCronetApplication().getCronetEngine();
    }


    CronetApplication getCronetApplication() {
        return ((CronetApplication) getApplication());
    }

    private static String[] imageUrls = {
            "https://storage.googleapis.com/cronet/sun.jpg",
            "https://storage.googleapis.com/cronet/flower.jpg",
            "https://storage.googleapis.com/cronet/chair.jpg",
            "https://storage.googleapis.com/cronet/white.jpg",
            "https://storage.googleapis.com/cronet/moka.jpg",
            "https://storage.googleapis.com/cronet/walnut.jpg"
    };


    public static String getImage() {
        int index = new Random(System.nanoTime()).nextInt(imageUrls.length - 1);
        return imageUrls[index];
    }
}