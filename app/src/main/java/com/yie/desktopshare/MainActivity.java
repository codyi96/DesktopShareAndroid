package com.yie.desktopshare;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecorderManager rm = RecorderManager.getInstance(getApplicationContext());
        rm.startRecorder(getApplicationContext(), 0.5f);
    }
}
