package com.hackathon;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private HeadTracker mHeadTracker;
    private float[] headView;

    private GoogleApiClient mGoogleApiClient;
    private static final String X_AXIS = "com.hackathon.xaxis";
    private static final String Y_AXIS = "com.hackathon.yaxis";
    private static final String Z_AXIS = "com.hackathon.zaxis";

    private static final String HEAD_PRE = "com.hackathon.head";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //up vector cal
        mHeadTracker = HeadTracker.createFromContext(getApplicationContext());
        headView = new float[16];
    }

    void sendData(){
        mHeadTracker.getLastHeadView(headView, 0);
        SensorManager.remapCoordinateSystem(headView, SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_Y, headView);
        //x z
        //Matrix.rotateM(headView, 0, 90, 0, 0, 1);
        //Matrix.setRotateEulerM(headView, 0, 0, 0, 90);
        //Matrix.setRotateEulerM(headView, 0, 90, 0, 0);

        PutDataMapRequest req = PutDataMapRequest.create("/head");
        for(int i=0; i<16; ++i){
            req.getDataMap().putFloat(HEAD_PRE+i, headView[i]);
            Log.d("HEAD", "i: "+headView[i]);
        }
        req.getDataMap().putLong("time", new Date().getTime());
        Wearable.DataApi.putDataItem(mGoogleApiClient, req.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (dataItemResult.getStatus().isSuccess()) {
                            Log.d("TEST", "Data item set: " + dataItemResult.getDataItem().getUri());
                        } else if (dataItemResult.getStatus().isCanceled()) {
                            Log.d("TEST", "canceled");
                        } else if (dataItemResult.getStatus().isInterrupted()) {
                            Log.d("TEST", "interrupted");
                        }
                    }
                });
    }
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        Log.d("TEST", "updateDisplay");
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mSensorManager.unregisterListener(this);
        mGoogleApiClient.disconnect();
        mHeadTracker.stopTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.DataApi.addListener(mGoogleApiClient,this);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mGoogleApiClient.connect();
        this.mHeadTracker.startTracking();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //process heart rate.

        //Log.d("GYRO", "x="+axisX+" y="+axisY+" z="+axisZ);
        //Log.d("GYRO", "x="+axisX);
        //Log.d("GYRO", "x="+axisX+" y="+axisY+" z="+axisZ);
        //Log.d("GYRO", "x="+axisX+" y="+axisY+" z="+axisZ);

        PutDataMapRequest req = PutDataMapRequest.create("/head");
        req.getDataMap().putLong("time", new Date().getTime());
        Wearable.DataApi.putDataItem(mGoogleApiClient, req.asPutDataRequest())
            .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Log.d("TEST", "Data item set: " + dataItemResult.getDataItem().getUri());
                    } else if (dataItemResult.getStatus().isCanceled()) {
                        Log.d("TEST", "canceled");
                    } else if (dataItemResult.getStatus().isInterrupted()) {
                        Log.d("TEST", "interrupted");
                    }
                }
            });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TEST", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("TEST", "onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("TEST", "onConnectionFailed");

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d("TEST", "onDataChanged");
        for(DataEvent event : dataEventBuffer){
            if(event.getType() == DataEvent.TYPE_CHANGED){
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().compareTo("/headReq") == 0){
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if(dataMap.getInt("head")==1)
                        sendData();
                }
            }
        }
    }
}
