package com.example.flora.evlab4;


        import android.app.Activity;
        import android.content.Context;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.os.Bundle;
        import android.widget.ImageView;
        import android.widget.TextView;


        import android.bluetooth.BluetoothSocket;
        import android.content.Intent;
        import android.view.View;
        import android.widget.Button;
        import android.widget.Toast;
        import android.app.ProgressDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.os.AsyncTask;

        import java.io.IOException;
        import java.util.UUID;

public class Accelerometer extends Activity implements SensorEventListener {

    //Initialize Disconnect button
    Button btnDis;

    //Data sent via Bluetooth
    String address = null;
    String value1 = String.valueOf(0);
    String value2 = String.valueOf(0);
    String value3 = String.valueOf(0);
    String values = String.valueOf(0);

    //Bluetooth settings
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    //SPP UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Accelerometer setup
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float x = 0;
    private float y = 0;
    private float z = 0;

    private float steering = 0;
    private float throttle = 0;
    private float brake = 0;

    public boolean isHorizontal = true;

    final static int middlePoint = 128;

    private TextView currentX, currentY, currentZ, steeringA, throttleA, brakeA;

    ImageView logo;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Get address from main activity
        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESSA);

        //Layout
        setContentView(R.layout.activity_accelerometer);

        //Initialize disconnect button
        btnDis = (Button)findViewById(R.id.buttondis);

        initializeViews();

        //Connect Bluetooth
        new ConnectBT().execute();

        //Implement disconnect button
        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });

        //Get Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // fai! we dont have an accelerometer!
        }

    }

    //initializeViews() display sensor values on app
    public void initializeViews() {
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);

        steeringA = (TextView) findViewById(R.id.steeringA);
        throttleA = (TextView) findViewById(R.id.throttleA);
        brakeA = (TextView) findViewById(R.id.brakeA);
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // clean current values
        displayCleanValues();

        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        double noise = 1.5;
        float noiseG = 10;
        /*
            initial values for horizontal:
                x~=0.15-0.25
                y~=0.0 - (-0.05)
                z~=10
                --------
            initial values for vertical:
                x~=0.3-0.4
                y~=9.6-9.7
                z~=0.2-0.3
         */
        if (x < noise && x > 0 || x > -noise && x < 0) {
            x = 0;
            //deltaX can be used for steering with tablet being held vertical or horizontal
            //90° til to left -> deltaX ~= -10.2 || 90° tilt to the right -> deltaX ~= 9.3
        }

        if (y > noiseG - noise && y < noiseG + noise){
            y = noiseG;
        }
        else if (y < noise && y > 0 || y > -noise && y < 0) {
            y = 0;
            //deltaY can only be used for accel/braking with tablet being horizontal
            //90° tilt towards user -> deltaY ~= -9.8 || 90° tilt away from user -> deltaY ~= 9.8
        }

        if (z > noiseG - noise && z < noiseG + noise) {
            z = noiseG;
        }
        else if(z < noise && z > 0 || z > -noise && z < 0){
            z = 0;
        }
        // display the current x,y,z accelerometer values
        displayCurrentValues();

        calcMotor();
        displayMotor();
        addValues();


    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");

        steeringA.setText("0.0");
        throttleA.setText("0.0");
        brakeA.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(x));
        currentY.setText(Float.toString(y));
        currentZ.setText(Float.toString(z));


    }

    public void calcMotor() {
        if (x > 0) {
            steering = middlePoint - (x * 11);
        } else if (x < 0) {
            steering = middlePoint - (x * 10);
        } else steering = 0;

        if (z < 0) { //tilting towards user
            throttle = 0;
            brake = -(z * 20);
        } else if (z > 0) {
            throttle = z * 20;
            brake = 0;
        } else {
            throttle = 0;
            brake = 0;
        }
    }

    public void displayMotor() {
        steeringA.setText(Float.toString(steering));
        throttleA.setText(Float.toString(throttle));
        brakeA.setText(Float.toString(brake));
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


    //Send data via Bluetooth
    private void addValues() {
        if (btSocket!=null) {

            value1 = Float.toString(steering);
            value2 = Float.toString(throttle);
            value3 = Float.toString(brake);

            values = value1+","+value2+","+value3+"#";

            try {
                btSocket.getOutputStream().write(values.toString().getBytes());
                btSocket.getOutputStream().flush();
            } catch (IOException e) {
                msg("Error");
            }
        }
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  {
        private boolean ConnectSuccess = true;
        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(Accelerometer.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }
        @Override
        //while the progress dialog is shown, the connection is done in background
        protected Void doInBackground(Void... devices) {
            try {

                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }

            catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }

            return null;
        }
        @Override
        //after the doInBackground, it checks if everything went fine
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }

            else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    private void Disconnect() {
        if (btSocket!=null) {
            try {
                btSocket.close();
            }
            catch (IOException e)
            { msg("Error Disconnect");}
        }
        finish();
    }


}