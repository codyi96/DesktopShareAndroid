package com.yie.desktopshare;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zou on 17-9-22.
 *
 */

public class RecorderManager {
    private static final String TAG = RecorderManager.class.getName();
    private static RecorderManager mRcdManager;
    private Context mContext;
    private final int MAX_CLIENT_COUNT = 10;
    private final static int PORT = 23133;
    private final static int MAX_BYTE_SIZE = 1024 * 30;

    public static final byte VERSION = 1;
    //绘制变量
    private Bitmap mDrawBitmap;
    private Canvas mCanvas = new Canvas();
    private View mRootView;

    //线程变量
    private String mCompressThreadName = "Compress-Thread";
    private Handler mCompressHandler;
    private HandlerThread mCompressHandlerThread;

    private String mClientThreadName = "Client-Thread";
    private HandlerThread mClientHandlerThread;
    private List<ClientHandler> mClientHandlers = new ArrayList<>();
    private Handler mUIHandler = new Handler(Looper.getMainLooper());


    private Runnable mDrawTask = new DrawTask();
    private Runnable mCompressTask = new CompressTask();
    private Runnable mListenTask = new ListenTask();

    private Thread mListenThread;
    private Socket socket;

    public static synchronized RecorderManager getInstance(Context context) {
        if (mRcdManager == null) {
            mRcdManager = new RecorderManager(context);
        }
        return mRcdManager;
    }

    private RecorderManager(Context context) {
        mContext = context.getApplicationContext();
        mCompressHandlerThread = new HandlerThread(mCompressThreadName) {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                mCompressHandler = new Handler();
            }
        };
        mCompressHandlerThread.start();
        startListen();
    }

    private void startListen() {
        mListenThread = new Thread(mListenTask);
        mListenThread.start();
    }

    public void stopRecorder() {
        mRootView = null;
        mUIHandler.removeCallbacks(mDrawTask);
        if (mCompressHandler != null) {
            mCompressHandler.getLooper().quit();
        }
        for (ClientHandler clientHandler : mClientHandlers) {
            clientHandler.getLooper().quit();
        }
        try {
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "socket close failed!");
        }
        mRcdManager = null;
    }

    public void startRecorder(final Context context, float scale) {
        Point point = getScreenSize(context);
        int exceptW = (int) (point.x * scale);
        int exceptH = (int) (point.y * scale);
        if (mDrawBitmap == null) {
            mDrawBitmap = Bitmap.createBitmap(exceptW, exceptH, Bitmap.Config.RGB_565);
        }
        if (mDrawBitmap.getWidth() != exceptW || mDrawBitmap.getHeight() != exceptH) {
            mDrawBitmap.recycle();
            mDrawBitmap = Bitmap.createBitmap(exceptW, exceptH, Bitmap.Config.RGB_565);
        }
        mCanvas.setBitmap(mDrawBitmap);
        mCanvas.scale(scale, scale);
        if (context instanceof Activity) {
            startRecorderActivity((Activity) context);
        } else {
            Log.i(TAG, "can't find activity!");
        }

        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new ActivityCallback() {
            @Override
            public void onActivityResumed(Activity activity) {
                startRecorderActivity(activity);
            }
        });
    }


    private static Point getScreenSize(Context context) {
        int w = context.getResources().getDisplayMetrics().widthPixels;
        int h = context.getResources().getDisplayMetrics().heightPixels;
        return new Point(w, h);
    }

    private void startRecorderActivity(Activity activity) {
        mRootView = activity.getWindow().getDecorView();
        mUIHandler.removeCallbacks(mDrawTask);
        mUIHandler.post(mDrawTask);
    }


    private class DrawTask implements Runnable {
        @Override
        public void run() {
            if (mRootView == null) {
                return;
            }
            mUIHandler.removeCallbacks(mDrawTask);
            mRootView.draw(mCanvas);
            mCompressHandler.removeCallbacks(mCompressTask);
            mCompressHandler.post(mCompressTask);
        }
    }

    private class CompressTask implements Runnable {
        ByteArrayPool mByteArrayPool = new ByteArrayPool(MAX_BYTE_SIZE);
        PoolingByteArrayOutputStream mByteArrayOutputStream =
                new PoolingByteArrayOutputStream(mByteArrayPool);
        @Override
        public void run() {
            try {
                mByteArrayOutputStream.reset();
                mDrawBitmap.compress(Bitmap.CompressFormat.JPEG, 60, mByteArrayOutputStream);
                byte[] jpgBytes = mByteArrayOutputStream.toByteArray();
                for (ClientHandler clientHandler : mClientHandlers) {
                    clientHandler.sendData(jpgBytes);
                    Log.i(TAG, "send jpg size=" + jpgBytes.length);
                }
                mUIHandler.post(mDrawTask);
            }catch (Exception e){
                Log.e(TAG, "compress and send data failed!");
            }
        }
    }

    private class ListenTask implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
                Log.i(TAG, "set port:" + PORT);
            } catch (IOException e) {
                Log.e(TAG, "Can't set port!");
            }
            for (int i = 0; i < MAX_CLIENT_COUNT; i++) {
                try {
                    if (serverSocket != null) {
                        socket = serverSocket.accept();
                        mClientHandlerThread = new HandlerThread(mClientThreadName) {
                            @Override
                            protected void onLooperPrepared() {
                                super.onLooperPrepared();
                                mClientHandlers.add(new ClientHandler(socket));
                            }
                        };
                        mClientHandlerThread.start();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Listen port failed!");
                    return;
                }
            }

        }

    }

}
