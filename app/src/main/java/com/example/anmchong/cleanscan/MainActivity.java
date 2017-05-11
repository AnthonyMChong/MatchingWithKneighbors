package com.example.anmchong.cleanscan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;

public class MainActivity extends AppCompatActivity {

    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;

    Button DegValButton;
    Button RadValButton;

    TextView TV1;
    TextView TV2;
    TextView TV3;
    TextView TV4;
    TextView MyCartText;

    EditText DegVal;
    EditText RadVal;
    EditText OffsetVal;

    TextView RadiusOut;
    TextView DegreeOut;

    EditText SimBecVal1;
    EditText SimBecVal2;
    EditText SimBecVal3;

    RelativeLayout ColorLayout;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    String StringOfData = "";
    String StringOfDataFinal = "";
    String StringOfDataIB = "";
    String StringOfDataFinalIB = "";
    private static final int REQUEST_ENABLE_BT = 1;

    int maxReadings = 4;

    private static final long SCAN_PERIOD = 100000;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


// Beacon names here, this app will only take data on beacons in this list....
    String MeasuredIbeaconxW1I = "xW1I"; // the bus beacon 200ms 6dBm
    String MeasuredIbeaconUos6 = "Uos6"; // Beacon A 200ms 6dBm
    String MeasuredIbeacond2Rq = "d2Rq"; // Beacon B 200ms 6dBm
    String MeasuredIbeaconpYEM = "pYEM"; // Beacon C 200ms 6dBm


    int[][] BeaconVal = new int[12][256];
    int[] AverageRSSI = new int [12];
    int[] ReadingIndex = new int [12];
    int BeaconCount = 4; // number of beacons we want to read

//    int[0] StorexW1I = new int[20];
//    int[1] StoreUos6 = new int[20];
//    int[2] Stored2Rq = new int[20];
//    int[3] StorepYEM = new int[20];

    String[] fileLines;
    String[] fileIndividual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        TV1 = (TextView) findViewById(R.id.textView);
        TV2 = (TextView) findViewById(R.id.textView2);
        TV3 = (TextView) findViewById(R.id.textView3);
        TV4 = (TextView) findViewById(R.id.textView4);
        MyCartText = (TextView) findViewById(R.id.CartText);

        RadiusOut = (TextView) findViewById(R.id.RadiusOut);
        DegreeOut = (TextView) findViewById(R.id.DegreeOut);

        SimBecVal1 = (EditText) findViewById(R.id.SimBecVal1);
        SimBecVal2 = (EditText) findViewById(R.id.SimBecVal2);
        SimBecVal3 = (EditText) findViewById(R.id.SimBecVal3);




        ColorLayout = (RelativeLayout) findViewById(R.id.activity_main);

        Arrays.fill(BeaconVal[0],1);
        Arrays.fill(BeaconVal[1],1);
        Arrays.fill(BeaconVal[2],1);
        Arrays.fill(BeaconVal[3],1);

// opening file


        AssetManager am = this.getAssets();
        InputStream InfoFileStream = null;
        try {
            InfoFileStream = am.open("FingerPrintTable.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileContent = "";    // string to be filled with data
        byte[] readbuffer = new byte[10240];
        int n;
        try {
            while ((n = InfoFileStream.read(readbuffer)) != -1) {
                fileContent += (new String(readbuffer, 0, n));  // adding every line into string
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileLines = fileContent.split("\n"); // split data with new line
        fileIndividual = (fileLines[1]).split(",");

        SimBecVal1.setText( "-74" );
        SimBecVal2.setText( "-89" );
        SimBecVal3.setText( "-72" );
        //-74,-89,-72,30.0,9.0,0.0
        //fileIndividual = (fileLines[1]).split(",");  // break up line starts at 0


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
//        mBluetoothAdapter.enable();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {


                    @Override

                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }

                    }


                });

                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.

        // Initializes list view adapter.
        //scanLeDevice(true);
    }


        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // User chose not to enable Bluetooth.
            if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

    @Override
    protected void onPause() {
        super.onPause();
//        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    public int findRSSIavg(int beacindex){
        int avg = 0;
        int aindex = 0;
        for (aindex = 0; aindex < maxReadings; aindex++) {
            avg += BeaconVal[beacindex][aindex];
        }
        avg = avg / maxReadings;
        return avg;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //device.EXTRA_RSSI = Integer.toString(rssi);
                    String mys = new String(scanRecord);
                    String MeasuredEddyUID = "xW1I";    //Eddystone device name we want to read
                    String MeasuredIbeacon = "xW1I";    //Ibeacon device name we want to read

                    String FoundMeasureUID = "";        // initializing strings for future comparisons
                    String FoundMeasureURL = "";
                    String FoundMeasureIB = "";
                        //Toast.makeText(getApplicationContext(), "  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
                        // The above code will notify the user when a beacon is being detected along with the device RSSI
                        //StringOfData += device.getName() + "\n"+"RSSI:"+Integer.toString(rssi) +"\n"+"Bytes:"+scanRecord+"\n"+mys+"\n\n";
                        StringBuilder sb = new StringBuilder();
                        for (byte b : scanRecord) {
                            sb.append(String.format("%02X ", b));
                        }
                        FoundMeasureUID += Character.toString((char) scanRecord[45])+Character.toString((char) scanRecord[46])
                                +Character.toString((char) scanRecord[47])+Character.toString((char) scanRecord[48]);
                        FoundMeasureURL += Character.toString((char) scanRecord[47])+Character.toString((char) scanRecord[48])
                                +Character.toString((char) scanRecord[49])+Character.toString((char) scanRecord[50]);
                        FoundMeasureIB += Character.toString((char) scanRecord[46])+Character.toString((char) scanRecord[47])
                                +Character.toString((char) scanRecord[48])+Character.toString((char) scanRecord[49]);
                    /*
                        // The above code makes a string to compare to the read string
                        // if the read string matches with one of these, the bluetooth reading belongs
                        // to a beacon and its specific framework
                        if (FoundMeasureUID.compareTo(MeasuredEddyUID) == 0) {
                            //StringOfData +=" " + rssi+"\n";
                            TV1.setText(Integer.toString(rssi));
                        }

                        if (FoundMeasureURL.compareTo(MeasuredEddyUID) == 0) {

                            StringOfDataIB +=" " + rssi;
                            TV2.setText(Integer.toString(rssi));
                        }
                        if (FoundMeasureIB.compareTo(MeasuredIbeacon) == 0) {

                            //IBfound = 1;

                            StringOfDataIB +=" " + rssi;
                            TV1.setText(Integer.toString(rssi));
                        }
                */
                    if (FoundMeasureIB.compareTo(MeasuredIbeaconxW1I) == 0) {
                        StringOfDataIB +="" + rssi;
                        TV1.setText("xW1I:   " + Integer.toString(rssi));
                        //ColorLayout.setBackgroundColor(Color.GREEN);
                        BeaconVal [0][ReadingIndex[0]] = rssi;
                        ReadingIndex[0]++;
                        if (ReadingIndex[0]>maxReadings)
                            ReadingIndex[0] = 0;
                    }
                    if (FoundMeasureIB.compareTo(MeasuredIbeaconUos6) == 0) {
                        StringOfDataIB +="" + rssi;
                        TV2.setText("Uos6:   " + Integer.toString(rssi));
                        BeaconVal [1][ReadingIndex[1]] = rssi;
                        ReadingIndex[1]++;
                        if (ReadingIndex[1]>maxReadings) {
                            ReadingIndex[1] = 0;
                        }
                        //SimBecVal3.setText(Integer.toString(findRSSIavg(1)));
                        SimBecVal3.setText(Integer.toString(rssi));
                    }
                    if (FoundMeasureIB.compareTo(MeasuredIbeacond2Rq) == 0) {
                        StringOfDataIB +="" + rssi;
                        TV3.setText("d2Rq:   " + Integer.toString(rssi));
                        BeaconVal [2][ReadingIndex[2]] = rssi;
                        ReadingIndex[2]++;
                        if (ReadingIndex[2]>maxReadings) {
                            ReadingIndex[2] = 0;
                        }
                        //SimBecVal2.setText(Integer.toString(findRSSIavg(2)));
                        SimBecVal2.setText(Integer.toString((rssi)));
                    }
                    if (FoundMeasureIB.compareTo(MeasuredIbeaconpYEM) == 0) {
                        StringOfDataIB +="" + rssi;
                        TV4.setText("pYEM:   " + Integer.toString(rssi));
                        BeaconVal [3][ReadingIndex[3]] = rssi;
                        ReadingIndex[3]++;
                        if (ReadingIndex[3]>maxReadings) {
                            ReadingIndex[3] = 0;
                        }
                        //SimBecVal1.setText(Integer.toString(findRSSIavg(3)));
                        SimBecVal1.setText(Integer.toString((rssi)));
                    }

                    if ((FoundMeasureUID.compareTo(MeasuredEddyUID) == 0) |
                            (FoundMeasureIB.compareTo(MeasuredIbeaconxW1I) == 0) |
                            (FoundMeasureIB.compareTo(MeasuredIbeaconUos6) == 0) |
                            (FoundMeasureIB.compareTo(MeasuredIbeacond2Rq) == 0) |
                            (FoundMeasureIB.compareTo(MeasuredIbeaconpYEM) == 0) |
                            (FoundMeasureURL.compareTo(MeasuredEddyUID) == 0) |
                            (FoundMeasureIB.compareTo(MeasuredIbeacon) == 0) )
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }

                }
            };


    public void startUpdatesButtonHandler(View view) {
        scanLeDevice(true);
//        StringOfDataFinal += StringOfData;
//        StringOfDataFinalIB += StringOfDataIB;
//        StringOfData = "";
//        StringOfDataIB = "";
    }

    public void RadUpClickListener(View view) {
        float Radvalcurrent = new Float(RadVal.getText().toString()).floatValue();
        Radvalcurrent += 0.5;
        RadVal.setText(Float.toString(Radvalcurrent));
    }

    public void DegUpClickListener(View view) {
        float Degvalcurrent = new Float(DegVal.getText().toString()).floatValue();
        Degvalcurrent += 30;
        if (Degvalcurrent >= 360){
            Degvalcurrent = 0;
        }
        DegVal.setText(Float.toString(Degvalcurrent));
    }

    public void OffsetClickListener(View view) {
        float OffsetValcurrent = new Float(OffsetVal.getText().toString()).floatValue();
        OffsetValcurrent += 180;
        if (OffsetValcurrent >= 360){
            OffsetValcurrent = 0;
        }
        OffsetVal.setText(Float.toString(OffsetValcurrent));
    }

    public void SaveButtonHandler(View view) {
    }

    public void stopUpdatesButtonHandler(View view) {
        scanLeDevice(false);
        ColorLayout.setBackgroundColor(Color.WHITE);
        int avgindex = 0;

        int beaconindex = 0;
        for(;beaconindex<BeaconCount;) {
            for (AverageRSSI[beaconindex] = 0; (BeaconVal[beaconindex][avgindex] != 1) && (avgindex < 20); ) {
                AverageRSSI[beaconindex] += BeaconVal[beaconindex][avgindex];
                avgindex++;
            }
            if (avgindex == 0) {// there were no read values
                if (beaconindex == 0)
                    TV1.setText("avgxW1I:" + "none");
                if (beaconindex == 1)
                    TV2.setText("avgUos6:" + "none");
                if (beaconindex == 2)
                    TV3.setText("avgd2Rq:" + "none");
                if (beaconindex == 3)
                    TV4.setText("avgpYEM:" + "none");
                AverageRSSI[beaconindex] = 0;
            } else {
                AverageRSSI[beaconindex] = AverageRSSI[beaconindex] / avgindex;
                if (beaconindex == 0)
                    TV1.setText("avgxW1I:" + AverageRSSI[beaconindex]);
                if (beaconindex == 1)
                    TV2.setText("avgUos6:" + AverageRSSI[beaconindex]);
                if (beaconindex == 2)
                    TV3.setText("avgd2Rq:" + AverageRSSI[beaconindex]);
                if (beaconindex == 3)
                    TV4.setText("avgpYEM:" + AverageRSSI[beaconindex]);
            }
            beaconindex++;
            avgindex = 0;

/*
            StringOfDataFinal += "" + AverageRSSI[1] + "," + AverageRSSI[2] + "," + AverageRSSI[3] + ","
                    + DegVal.getText().toString() + "," + RadVal.getText().toString() +
                    "," + OffsetVal.getText().toString() + "\n";
                    */
        }
        Arrays.fill(BeaconVal[0],1);
        Arrays.fill(BeaconVal[1],1);
        Arrays.fill(BeaconVal[2],1);
        Arrays.fill(BeaconVal[3],1);
        Arrays.fill(ReadingIndex,0);
        ///////////////////////////////////////////////////////////////////////////////////////////
        // Sorting Algorithm Start here///////////////////////////////////////////////////////////
        /* code in this area will run when the cancel button is pressed

         */
        float foundDeg;
        float foundRadius;
        int Kneighors = 4;
        int lineIndex;
        float CurrentDiff;

        float [][] Lowest4 = new float[Kneighors][6];
        // initializing array to be filled with high numbers or comparison will not work
        for (int initindex = 0 ; initindex< Kneighors;initindex++ ){
            for (int subinitindex = 0; subinitindex < 3; subinitindex++){
                Lowest4 [initindex][subinitindex] = 300;
            }
        }

        int lowindex = 0;
        int bumpindex = 0;

        float CurrentSimBecVal1 = new Integer(SimBecVal1.getText().toString()).floatValue();
        float CurrentSimBecVal2 = new Integer(SimBecVal2.getText().toString()).floatValue();
        float CurrentSimBecVal3 = new Integer(SimBecVal3.getText().toString()).floatValue();
        // Above is how you can read from a UI input, you can test your values like this

        for (lineIndex = 0;lineIndex < fileLines.length ; lineIndex++){  // going through each line until we dont have anymore
            fileIndividual = fileLines[lineIndex].split(",");                // splitting current line
            CurrentDiff = abs(Float.parseFloat(fileIndividual[0]) - (float)CurrentSimBecVal1) +
            abs(Float.parseFloat(fileIndividual[1]) - (float)CurrentSimBecVal2) +
            abs(Float.parseFloat(fileIndividual[2]) - (float)CurrentSimBecVal3);

            for (lowindex = 0 ;lowindex < Kneighors ; lowindex++){  // 4 worst match, highest mag
                if (Lowest4[lowindex][0] > CurrentDiff){  // start comparing at best match
                    // this for loop will knock down all the old information in our top 4 list
                    for (bumpindex = Kneighors-1; bumpindex != lowindex ; bumpindex-- ){
                        Lowest4[bumpindex][0] = Lowest4[bumpindex - 1][0]; // replacing because we found lower
                        Lowest4[bumpindex][1] = Lowest4[bumpindex - 1][1];
                        Lowest4[bumpindex][2] = Lowest4[bumpindex - 1][2];
                        Lowest4[bumpindex][3] = Lowest4[bumpindex - 1][3]; // replacing because we found lower
                        Lowest4[bumpindex][4] = Lowest4[bumpindex - 1][4];
                        Lowest4[bumpindex][5] = Lowest4[bumpindex - 1][5];
                    }
                    Lowest4[lowindex][0] = CurrentDiff; // replacing because we found lower
                    Lowest4[lowindex][1] = Float.parseFloat(fileIndividual[3]);
                    Lowest4[lowindex][2] = Float.parseFloat(fileIndividual[4]);
                    Lowest4[lowindex][3] = Float.parseFloat(fileIndividual[0]);
                    Lowest4[lowindex][4] = Float.parseFloat(fileIndividual[1]);
                    Lowest4[lowindex][5] = Float.parseFloat(fileIndividual[2]);
                    break;
                }
            }
        }

        // if you want to read the file, the lines are already saved in the filelines array
        // ex: if you wanted to access the 3rd line in the file filelines[2]
        // Since it is a CSV you must split up each line for the information
        // ex: if you wanted the 2nd value of the 3rd line you would run
        // fileIndividual = (fileLines[2]).split(",");
        // fileIndividual[1];
        // fileIndividual is a global string array defined at the top
        // Good luck!

        TV1.setText("mag: "+Float.toString(Lowest4[0][0])+" deg: "+Float.toString(Lowest4[0][1]) +" rad: " + Float.toString(Lowest4[0][2])+
                " A:"+ Float.toString(Lowest4[0][3])+" B:"+Float.toString(Lowest4[0][4]) +" C:"+ Float.toString(Lowest4[0][5]));
        TV2.setText("mag: "+Float.toString(Lowest4[1][0])+" deg: "+Float.toString(Lowest4[1][1]) +" rad: " + Float.toString(Lowest4[1][2])+
                " A:"+ Float.toString(Lowest4[1][3])+" B:"+Float.toString(Lowest4[1][4]) +" C:"+ Float.toString(Lowest4[1][5]));
        TV3.setText("mag: "+Float.toString(Lowest4[2][0])+" deg: "+Float.toString(Lowest4[2][1]) +" rad: " + Float.toString(Lowest4[2][2])+
        " A:"+ Float.toString(Lowest4[2][3])+" B:"+Float.toString(Lowest4[2][4]) +" C:"+ Float.toString(Lowest4[2][5]));
        TV4.setText("mag: "+Float.toString(Lowest4[3][0])+" deg: "+Float.toString(Lowest4[3][1]) +" rad: " + Float.toString(Lowest4[3][2])+
                " A:"+ Float.toString(Lowest4[3][3])+" B:"+Float.toString(Lowest4[3][4]) +" C:"+ Float.toString(Lowest4[3][5]));


        // Weighted average with K near neighbors
        float weightHold = 0;
        for(lowindex=0; lowindex < Kneighors; lowindex++) {
            //Log.d("myTag", "lowindex: "+lowindex+"   Lowest4[lowindex][0]: "+Lowest4[lowindex][0] +"   rad: " + Float.toString(Lowest4[0][2]) + " RSSIA"+CurrentSimBecVal1);
            if (Lowest4[lowindex][0] != 0) {
                weightHold += (1 / (Lowest4[lowindex][0]));
            } else {
                weightHold = -1;    // perfect match found
                break;
            }
        }
        foundRadius = 0;
        foundDeg = 0;
        float SinSum = 0;
        float CosSum = 0;
        float xSum = 0;
        float ySum = 0;
        if (weightHold == -1){  // set to perfect match
            foundDeg = Lowest4[0][1];
            foundRadius = Lowest4[0][2];
            ySum = (float)((foundRadius*sin(Math.toRadians(foundDeg))));
            xSum = (float)((foundRadius*cos(Math.toRadians(foundDeg))));
            }
            else{   // take the weighted avg
                for (lowindex=0; lowindex< Kneighors ; lowindex++) {
                    SinSum += ((1/Lowest4[lowindex][0])/weightHold)*(sin(Math.toRadians(Lowest4[lowindex][1])));
                    CosSum += ((1/Lowest4[lowindex][0])/weightHold)*(cos(Math.toRadians(Lowest4[lowindex][1])));
                    //Log.d("myTag", "angle: " + Lowest4[lowindex][1] + " Sin: " + SinSum + " Cos: " + CosSum);
                    ySum += ((1/Lowest4[lowindex][0])/weightHold)*(Lowest4[lowindex][2]*sin(Math.toRadians(Lowest4[lowindex][1])));
                    xSum += ((1/Lowest4[lowindex][0])/weightHold)*(Lowest4[lowindex][2]*cos(Math.toRadians(Lowest4[lowindex][1])));
                    foundRadius += ((1 / Lowest4[lowindex][0]) / weightHold) * (Lowest4[lowindex][2]);
                }
                //Log.d("myTag","Sin: "+ SinSum+ " Cos: "+ CosSum );
                foundDeg = (float) toDegrees(atan2(SinSum,CosSum));

            }
        DegreeOut.setText("Degree:"+Float.toString(foundDeg));
        RadiusOut.setText("Radius:"+Float.toString(foundRadius));
        MyCartText.setText("x"+ xSum +"\n" +"  y"+ySum );
        //MyCartText.setText("x"+Double.toString(foundRadius*cos(Math.toRadians(foundDeg)))+"\n"
         //       +"  y"+Double.toString(foundRadius*sin(Math.toRadians(foundDeg))) );
        // how you set your ouput text displays for testing


    }


}