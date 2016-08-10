package com.example.flora.evlab4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    //Variables
    String address = null;
    public static String EXTRA_ADDRESSA = "device_address"; //PROBLEM?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get address from previous activity
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        //Layout
        setContentView(R.layout.activity_main);

        //Switch to Accelerometer Activity
        Button switchact = (Button) findViewById(R.id.buttonaccel);
        switchact.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, Accelerometer.class);
                k.putExtra(EXTRA_ADDRESSA, address);
                startActivity(k);
            }
        });

        //Switch to Slider Activity
        Button switchact2 = (Button) findViewById(R.id.buttonslider);
        switchact2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent l = new Intent(MainActivity.this, Sliders.class);
                l.putExtra(EXTRA_ADDRESSA, address);
                startActivity(l);
            }
        });

        //Switch to Data Collection Activity
        Button switchact3 = (Button) findViewById(R.id.buttondata);
        switchact3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent m = new Intent(MainActivity.this, Data.class);
                m.putExtra(EXTRA_ADDRESSA, address);
                startActivity(m);
            }
        });
    }
}
