package com.pkstudio.pressureulceralarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.prefs.Preferences;

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
    boolean isTimerOn = false;

    //Preference part
    static SharedPreferences sharedPreferences;
    static float threshold;
    static long timeToAlert; //time in second
    static boolean allowAlertSound;
    static boolean allowVibrate;
    static boolean showData;
    static String ringtonePreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        if(sensorManager == null){
            finish();
        }else{
            checkSensor();
            startSensor();
            stopTimer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingButton:
                showSetting();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void init(){
        //Preference part
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        threshold = Float.parseFloat(sharedPreferences.getString("threshold","10"));
        timeToAlert = Long.parseLong(sharedPreferences.getString("timeToAlert","10")) * 60;
        allowAlertSound = sharedPreferences.getBoolean("allowAlertSound", true);
        allowVibrate = sharedPreferences.getBoolean("allowVibrate",true);
        ringtonePreference = sharedPreferences.getString("userRingtone","DEFAULT_ALARM_ALERT_URI");
        showData = sharedPreferences.getBoolean("showData", false);

        //UI part
        gyroStatus = (TextView) findViewById(R.id.GyroStatus);
        accelStatus = (TextView) findViewById(R.id.AccelStatus);
        output = (TextView) findViewById(R.id.Output);

        timeLeft = (TextView) findViewById(R.id.TimeLeft);
        timerButton = (Button) findViewById(R.id.TimerButton);

        //Sensor part
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Timer part
        alarm = Uri.parse(ringtonePreference);
        if(alarm == null){

            alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if(alarm == null) {
                alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        r = RingtoneManager.getRingtone(getApplicationContext(),alarm);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    }

    //Sensor part
    private void checkSensor(){
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(showData) {
            gyroStatus.setText("Gyroscope");
            accelStatus.setText("Accelerometer");
            if (gyroSensor == null) {
                Toast.makeText(MainActivity.this, "Gyroscope is not detected.", Toast.LENGTH_LONG).show();
                gyroStatus.setTextColor(getResources().getColor(R.color.colorError));
            } else {
                gyroStatus.setTextColor(getResources().getColor(R.color.colorValid));
            }
            if (acceleroSensor == null) {
                Toast.makeText(MainActivity.this, "Accelerometer is not detected.", Toast.LENGTH_LONG).show();
                accelStatus.setTextColor(getResources().getColor(R.color.colorError));
            } else {
                accelStatus.setTextColor(getResources().getColor(R.color.colorValid));
            }
        }else{
            gyroStatus.setText("");
            accelStatus.setText("");
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
        if(showData) {
            output.setText("X: " + Math.floor(orientations[0]) + " Y: " + Math.floor(orientations[1]) + " Z: " + Math.floor(orientations[2]));
        }else{
            output.setText("");
        }
        if(isOrientationChanged(orientations)){
            if(isTimerOn){
                resetTimer();
            }
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

    //Timer part
    public void toggleTimer(View view){

        if(isTimerOn == false){
            startTimer();
            isTimerOn = true;
            Toast.makeText(MainActivity.this, "Service started", Toast.LENGTH_LONG).show();
        }else{
            stopTimer();
            isTimerOn = false;
            Toast.makeText(MainActivity.this, "Service stopped", Toast.LENGTH_LONG).show();
        }
    }

    private void stopTimer(){
        if(r.isPlaying()){
            r.stop();
        }
        vibrator.cancel();
        if(isTimerOn){
            timer.cancel();
        }
        timeLeft.setText("" + String.format("%02d",timeToAlert/60) + ":" + String.format("%02d",timeToAlert%60));
        timerButton.setText("Start");
    }

    private void startTimer(){
        timer = new CountDownTimer(timeToAlert * 1000, 1) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secsUntilFinished = millisUntilFinished/1000 + 1;
                timeLeft.setText("" + String.format("%02d",secsUntilFinished/60) + ":" + String.format("%02d",secsUntilFinished%60));
            }

            @Override
            public void onFinish() {
                timeLeft.setText("" + String.format("%02d",0) + ":" + String.format("%02d",0));
                alert();
                Toast.makeText(MainActivity.this, "Finished", Toast.LENGTH_LONG).show();
            }
        }.start();

        timerButton.setText("Stop");
    }

    private void resetTimer(){
        stopTimer();
        startTimer();
        //Toast.makeText(MainActivity.this, "Resetted", Toast.LENGTH_LONG).show();
    }

    private void alert(){
        if(allowAlertSound) {
            r.play();
        }
        if(allowVibrate) {
            vibrator.vibrate(10 * 1000);
        }
    }

    private void showSetting(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
