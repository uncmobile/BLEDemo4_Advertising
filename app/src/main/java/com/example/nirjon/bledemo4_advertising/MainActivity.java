package com.example.nirjon.bledemo4_advertising;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

public class MainActivity extends AppCompatActivity {

    Switch mySwitch, mySwitchr;
    TextView myTV, myTVR;
    //String myUUIDstring = "CDB7950D-73F1-4D4D-8E47-C090502DBD63";
    String myUUIDstring = "ec505efd-75b9-44eb-8f2a-6fe0b41e7264";

    BluetoothManager myManager;
    BluetoothAdapter myAdapter;
    BluetoothLeAdvertiser myAdvertiser;
    AdvertiseSettings myAdvertiseSettings;
    AdvertiseData myAdvertiseData;

    BluetoothLeScanner myScanner;

    boolean advFlag = true;

    AdvertiseCallback myAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v("TAG", "Advertise start succeeds: " + settingsInEffect.toString());
            //myTV.append("\nAdvertisement restarted successfully with new data.");
            myTV.invalidate();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.v("Tag", "Advertise start failed: " + errorCode);
            myTV.append("\nAdvertisement restart failed: code = " + errorCode);
            myTV.invalidate();
        }
    };

    ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if(result == null) {Log.v("Tag", "Result = NULL");return;}
            if(result.getDevice() == null) {Log.v("Tag", "Device = NULL");return;}
            StringBuilder builder = new StringBuilder(myUUIDstring);

            List<ParcelUuid> lp = result.getScanRecord().getServiceUuids();
            Map<ParcelUuid, byte[]> lpmap = result.getScanRecord().getServiceData();

            Object mykey = lpmap.keySet().toArray()[0];
            byte[] data = lpmap.get(mykey);
            String dstr = byteArrToString(data);
            String hstr = stringToHex(dstr);
            builder.append("\n").append(dstr).append(" (0x").append(hstr).append(")");

            Log.v("Tag", builder.toString());
            myTVR.setText(builder.toString());
            myTVR.invalidate();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BT_Adv_init();
        BT_AdvReceive_init();
    }

    static String byteArrToString(byte[] data){

        char[] result = new char[data.length];
        for(int i = 0; i < data.length; i++){
            result[i] = (char) data[i];
        }
        return new String(result);
    }

    static String stringToHex(String charStr) {
        char[] charr = charStr.toCharArray();
        StringBuilder hexstr = new StringBuilder();
        for (char ch : charr) {
            hexstr.append(Integer.toHexString((int) ch));
        }
        return hexstr.toString();
    }

    void BT_Adv_init()
    {
        myTV = (TextView) findViewById(R.id.tv);
        mySwitch = (Switch) findViewById(R.id.switchID);

        myManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myAdapter = myManager.getAdapter();
        myAdvertiser = myAdapter.getBluetoothLeAdvertiser();

        myAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(ADVERTISE_TX_POWER_HIGH)
                .build();

        if(!myAdapter.isMultipleAdvertisementSupported()){
            mySwitch.setEnabled(false);
            myTV.setText("Device does not support BLE advertisement.");
            myTV.invalidate();
            return;
        }

        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ParcelUuid puuid = new ParcelUuid(UUID.fromString(myUUIDstring));
                            advFlag = true;
                            for(int i = 0; i < 1000000 && advFlag == true; i++){
                                final String str = String.format("%06d", i);

                                myAdvertiseData = new AdvertiseData.Builder()
                                        .addServiceUuid(puuid)
                                        .addServiceData(puuid, str.getBytes(Charset.forName("UTF-8")))
                                        .setIncludeDeviceName(false)
                                        .setIncludeTxPowerLevel(false)
                                        .build();
                                try {

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            myTV.setText("Service UUID: " + myUUIDstring + "\n"
                                                    + "Service Data: " + str
                                                    + " (0x"  + stringToHex(str) + ")");
                                            myTV.invalidate();
                                        }
                                    });

                                    myAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, myAdvertiseCallback);
                                    Thread.sleep(1000);
                                    myAdvertiser.stopAdvertising(myAdvertiseCallback);
                                    Log.v("Tag", "Advertise: " + str);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }).start();
                }
                else{
                    advFlag = false;
                    myAdvertiser.stopAdvertising(myAdvertiseCallback);
                }
            }
        });

    }

    void BT_AdvReceive_init()
    {
        myTVR = (TextView) findViewById(R.id.tvr);
        mySwitchr = (Switch) findViewById(R.id.switchIDR);

        myScanner = myAdapter.getBluetoothLeScanner();

        final ScanSettings myScanSettings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        final ScanFilter myScanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(this.myUUIDstring)))
                .build();
        final List<ScanFilter> myScanFilers = new ArrayList<ScanFilter>();
        myScanFilers.add(myScanFilter);

        mySwitchr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    myScanner.startScan(myScanFilers, myScanSettings, myScanCallback);
                }
                else{
                    myScanner.stopScan(myScanCallback);
                }
            }
        });

    }

}
