package com.yie.desktopshare;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by zou on 17-9-22.
 *
 */

public class ClientHandler extends Handler {
    private final static String TAG = "ClientHandler";
    private BufferedOutputStream mOutputStream;
    private int msgId = 0x001;
    private int osSize = 1024 * 200;

    private void writeInt(OutputStream outputStream, int v) throws IOException {
        outputStream.write(v >> 24);
        outputStream.write(v >> 16);
        outputStream.write(v >> 8);
        outputStream.write(v);
    }

    public void sendData(byte[] datas) {
        removeMessages(msgId);
        Message message = obtainMessage();
        message.what = msgId;
        message.obj = datas;
        sendMessage(message);
        Log.i(TAG, "send... size=" + datas.length);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (mOutputStream != null) {
            try {
                byte[] data = (byte[]) msg.obj;
                mOutputStream.write(RecorderManager.VERSION);
                writeInt(mOutputStream, data.length);
                mOutputStream.write(data);
                mOutputStream.flush();
            } catch (IOException e) {
                try {
                    mOutputStream.close();
                } catch (IOException e1) {
                    Log.e(TAG, "outputStream close failed!");
                }
                mOutputStream = null;
                Log.e(TAG, "Socket send failed!");
            } finally {

            }
        }
    }

    public ClientHandler(Socket socket) {
        try {
            mOutputStream = new BufferedOutputStream(socket.getOutputStream(), osSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
