package com.resin.wearplainscroll;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    String MESSAGE = "/message";

    GoogleApiClient mGoogleApiClient;

    int count = 0;
    boolean downed = false;
    boolean isAlive = false;
    boolean leftHand = true;
    float speed = 0.4f;

    float initD = 0, dx = initD, dy = initD;
    float initPos = 200, px = initPos, py = initPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Google Api Client connected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                }).build();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "hand changed");
                leftHand = !leftHand;
            }
        });
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress((int) (speed * 50));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // 0-100:0.0-2.0
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "speed: " + speed + ", progress: " + progress);
                speed = progress / 50.0f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        isAlive = true;
        adbCommand("adb", "shell", "getevent", "-lt", "/dev/input/event5"); // thread start

        final Handler handler = new Handler();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        count++;
//                        Log.d("timer", "" + count);

                        if (downed && count >= 2) { // time passes -> released
                            send("up," + px + "," + py);
                            px = initPos;
                            py = initPos;
                            dx = initD;
                            dy = initD;
                            downed = false;
                        }
                    }
                });
            }
        }, 0, 300); // delay, period
    }

    void adbCommand(String... command) {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            final Process process = processBuilder.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream iStream = process.getInputStream();
                    InputStreamReader isReader = new InputStreamReader(iStream);
                    BufferedReader bufferedReader = new BufferedReader(isReader);

                    while (isAlive) {
                        String line = null;

                        try {
                            line = bufferedReader.readLine();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (line == null) {   // ends this thread
                            Log.d(TAG, "stream is null");
                            return;
                        }


                        String[] results = line.split("    ");
//                        Log.d(TAG, line + ", " + results.length);
//
//                        String check = "___";
//                        for (String s: results) check += "___" + s;
//                        Log.d(TAG, check + "___");

                        if (results.length != 6) continue;

                        if (!downed) {
                            send("down," + px + "," + py);
                            count = 0;
                            downed = true;
                        }

                        String xy = results[results.length - 5].substring(3);
                        int d = (int) Long.parseLong(results[results.length - 1], 16);

//                            Log.d(TAG, xy + ": " + d);
                        if (xy.equals("REL_X")) {
                            dx = d;
                        } else if (xy.equals("REL_Y")) {
                            dy = d;
                        } else continue;

                        double rnd = Math.random();

                        if (rnd < 0.3) {
                            int direction;
                            if (leftHand) direction = 1;
                            else direction = -1;
//                            px += dx * speed * direction;
                            py += dy * speed * direction;
                            send("move," + px + "," + py);
                            count = 0;
                        }
                    }
                }
            }).start();

            Toast.makeText(MainActivity.this, "started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void send(final String str) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] data = str.getBytes();

                Collection<String> nodes = getNodes();

                for (String node : nodes) {
                    MessageApi.SendMessageResult result =
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node, MESSAGE, data).await();

                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
                Log.d("send", str);
            }
        }).start();
    }

    Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        isAlive = false;
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}