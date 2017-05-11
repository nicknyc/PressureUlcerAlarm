package com.pkstudio.pressureulceralarm;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    //UI part
    TextView gyroStatus;
    TextView accelStatus;
    TextView output;

    TextView timeLeft;
    Button timerButton;

    //Sensor part
    SensorManager sensorManager;
    Sensor rotationVector;

    //Timer part
    CountDownTimer timer;
    Uri alarm;
    Ringtone r;
    Vibrator vibrator;

    //Logic part
    float[] prevOrientations = new float[3];

    //Preference part
    static float threshold = 10;
    static long timeToAlert = 10 ; //time in second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gyroStatus = (TextView) findViewById(R.id.GyroStatus);
        accelStatus = (TextView) findViewById(R.id.AccelStatus);
        output = (TextView) findViewById(R.id.Output);

        timeLeft = (TextView) findViewById(R.id.TimeLeft);
        timerButton = (Button) findViewById(R.id.TimerButton);

        alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if(alarm == null){

            alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if(alarm == null) {
                alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        r = RingtoneManager.getRingtone(getApplicationContext(),alarm);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager == null){
            finish();
        }else{
            checkSensor();
            startSensor();
            resetTimer();
        }
    }

    private void checkSensor(){
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(gyroSensor == null){
            Toast.makeText(MainActivity.this, "Gyroscope is not detected.", Toast.LENGTH_LONG).show();
            gyroStatus.setTextColor(getResources().getColor(R.color.colorError));
        }else{
            gyroStatus.setTextColor(getResources().getColor(R.color.colorValid));
        }
        if(acceleroSensor == null){
            Toast.makeText(MainActivity.this, "Accelerometer is not detected.", Toast.LENGTH_LONG).show();
            accelStatus.setTextColor(getResources().getColor(R.color.colorError));
        }else{
            accelStatus.setTextColor(getResources().getColor(R.color.colorValid));
        }
    }

    private void startSensor(){
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Arrays.fill(prevOrientations,0f);
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //read sensor data here
                updateSensorValue(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(listener,rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void updateSensorValue(SensorEvent event){
        float[] rotationMatrix = new float[16];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        // Remap coordinate system
        float[] remappedRotationMatrix = new float[16];
        SensorManager.remapCoordinateSystem(rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotationMatrix);

        // Convert to orientations
        float[] orientations = new float[3];
        SensorManager.getOrientation(remappedRotationMatrix,orientations);
        // Convert to angle
        for(int i = 0; i < 3; i++) {
            orientations[i] = (float)(Math.toDegrees(orientations[i]));
        }
        output.setText("X: " + Math.floor(orientations[0]) + " Y: " + Math.floor(orientations[1]) + " Z: " + Math.floor(orientations[2]));
        //for test orientation change
        /*if(isOrientationChanged(orientations)){
            getWindow().getDecorView().setBackgroundColor(Color.RED);

        }else{
            getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        }*/
        if(isOrientationChanged(orientations)){
            //Do something if there is orientation changing

        }
        prevOrientations = orientations;
    }

    private boolean isOrientationChanged(float[] orientations){
        boolean isChanged = false;
        for(int i = 0; i < 3; i++){
            if(Math.abs(orientations[i] - prevOrientations[i]) >= threshold){
                isChanged = true;
            }
        }
        return isChanged;
    }

    public void toggleTimer(View view){

        if(timerButton.getText().toString().equalsIgnoreCase("Start")){
            //for test
            Toast.makeText(MainActivity.this, "Service started", Toast.LENGTH_LONG).show();

            timer = new CountDownTimer(timeToAlert * 1000, 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long secsUntilFinished = millisUntilFinished/1000 + 1;
                    timeLeft.setText("" + String.format("%02d",secsUntilFinished/60) + ":" + String.format("%02d",secsUntilFinished%60));
                }

                @Override
                public void onFinish() {
                    alert();
                    Toast.makeText(MainActivity.this, "Finished", Toast.LENGTH_LONG).show();
                }
            }.start();

            timerButton.setText("Stop");
        }else{
            //for test
            Toast.makeText(MainActivity.this, "Service stopped", Toast.LENGTH_LONG).show();

            timer.cancel();
            resetTimer();
        }
    }

    private void resetTimer(){
        if(r.isPlaying()){
            r.stop();
            vibrator.cancel();
        }
        timeLeft.setText("" + String.format("%02d",timeToAlert/60) + ":" + String.format("%02d",timeToAlert%60));
        timerButton.setText("Start");
    }

    private void alert(){
        r.play();
        vibrator.vibrate(10 * 1000);
    }
}
