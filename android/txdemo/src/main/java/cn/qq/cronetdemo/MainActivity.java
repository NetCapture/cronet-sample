package cn.qq.cronetdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.CronetProvider;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "sanbo.MainActivity";
    private static CronetEngine cronetEngine;
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        testInThread();
        loadInThread();


    }

    private void testInThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<CronetProvider> cps = CronetProvider.getAllProviders(mActivity);
                    if (cps!=null&&cps.size()>0){
                        Log.i(TAG,"测试getAllProviders，结果:" +cps.toString());
                    }
                    List<CronetProvider> providers =
                            new ArrayList<>(CronetProvider.getAllProviders(mActivity));
                    Class c = CronetEngine.Builder.class;
                    Method m = c.getDeclaredMethod("getEnabledCronetProviders", Context.class, List.class);
                    if (m == null) {
                        Log.e(TAG, "method is null!");
                        return;
                    }
                    m.setAccessible(true);
                    Object o = m.invoke(null, mActivity, providers);
                    if (o == null) {
                        Log.e(TAG, "invoke result is null!");
                        return;
                    }
                    List<CronetProvider> ss = (List<CronetProvider>) o;
                    if (ss==null||ss.size()==0){
                            Log.e(TAG, "ss result is null!");
                            return;

                    }else{
                        Log.i(TAG,"sss result:" +ss.toString());
                    }
                    //                    getEnabledCronetProviders(mActivity, lcps);
                } catch (Throwable e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    private void loadInThread() {
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
        // Create an executor to execute the request
        Executor executor = Executors.newSingleThreadExecutor();
        ImageView iv = (ImageView) findViewById(R.id.cronet_image);
        UrlRequest.Callback callback = new SimpleUrlRequestCallback(iv);
        UrlRequest.Builder builder = getCronetEngine().newUrlRequestBuilder(
                getImage(), callback, executor);
        // Measure the start time of the request so that
        // we can measure latency of the entire request cycle
        ((SimpleUrlRequestCallback) callback).start = System.nanoTime();
        // Start the request
        builder.build().start();
    }

    /**
     * Use this class for create a request and receive a callback once the request is finished.
     */
    class SimpleUrlRequestCallback extends UrlRequest.Callback {

        private ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
        private WritableByteChannel receiveChannel = Channels.newChannel(bytesReceived);
        private ImageView imageView;
        public long start;
        private long stop;

        SimpleUrlRequestCallback(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public void onRedirectReceived(
                UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            android.util.Log.i(TAG, "****** onRedirectReceived ******");
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            android.util.Log.i(TAG, "****** Response Started ******");
            android.util.Log.i(TAG, "*** Headers Are *** " + info.getAllHeaders());

            request.read(ByteBuffer.allocateDirect(32 * 1024));
        }

        @Override
        public void onReadCompleted(
                UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            android.util.Log.i(TAG, "****** onReadCompleted ******" + byteBuffer);
            byteBuffer.flip();
            try {
                receiveChannel.write(byteBuffer);
            } catch (Exception e) {
                android.util.Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            }
            byteBuffer.clear();
            request.read(byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {

            stop = System.nanoTime();
            android.util.Log.i(TAG,
                    "****** Cronet Request Completed, the latency is " + (stop - start));

            android.util.Log.i(TAG,
                    "****** Cronet Request Completed, status code is " + info.getHttpStatusCode()
                            + ", total received bytes is " + info.getReceivedByteCount());
//            // Set the latency
//            ((MainActivity) context).addCronetLatency(stop - start, 0);

            byte[] byteArray = bytesReceived.toByteArray();
            final Bitmap bimage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    imageView.setImageBitmap(bimage);
                    imageView.getLayoutParams().height = bimage.getHeight();
                    imageView.getLayoutParams().width = bimage.getWidth();
                }
            });
        }

        @Override
        public void onFailed(UrlRequest var1, UrlResponseInfo var2, CronetException var3) {
            android.util.Log.i(TAG, "****** onFailed, error is: " + var3.getMessage());
        }
    }


    private synchronized CronetEngine getCronetEngine() {
        // Lazily create the Cronet engine.
        if (cronetEngine == null) {
            // 不支持google play 设备 报错
            // java.lang.RuntimeException: All available Cronet providers are disabled. A provider should be enabled before it can be used.
            //        at org.chromium.net.CronetEngine$Builder.getEnabledCronetProviders(CronetEngine.java:365)
            //        at org.chromium.net.CronetEngine$Builder.createBuilderDelegate(CronetEngine.java:327)
            //        at org.chromium.net.CronetEngine$Builder.<init>(CronetEngine.java:75)
            //        at cn.qq.cronetdemo.MainActivity.getCronetEngine(MainActivity.java:142)
            //        at cn.qq.cronetdemo.MainActivity.loadImg(MainActivity.java:56)
            //        at cn.qq.cronetdemo.MainActivity.access$000(MainActivity.java:23)
            //        at cn.qq.cronetdemo.MainActivity$1.run(MainActivity.java:43)
            //        at java.lang.Thread.run(Thread.java:919)
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(mActivity);
            // Enable caching of HTTP data and
            // other information like QUIC server information, HTTP/2 protocol and QUIC protocol.
            cronetEngine = myBuilder
                    .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISABLED, 100 * 1024)
                    .addQuicHint("storage.googleapis.com", 443, 443)
                    //.enableHttp2(true)
                    .enableQuic(true)
                    .build();
            //    .setUserAgent("clb_quic_demo")
        }
        return cronetEngine;
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
        return "https://storage.googleapis.com/cronet/moka.jpg";
    }
//    public static String getImage() {
//        int index = new Random(System.nanoTime()).nextInt(imageUrls.length - 1);
//        return imageUrls[index];
//    }
}