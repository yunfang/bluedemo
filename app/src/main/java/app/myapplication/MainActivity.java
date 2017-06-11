package app.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.uyu.device.library.BluetoothSPP;
import com.uyu.device.library.BluetoothState;
import com.uyu.device.library.DeviceList;

import app.myapplication.domain.bluetooth.tradition.MotorBluetoothAdapter;
import app.myapplication.sharepreference.SharePreferenceTool;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothSPP bt;
    /**
     * 是否第一次连上蓝牙
     */
    private boolean isConnectBltFirst = true;
    private Button id_bu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        id_bu = (Button) findViewById(R.id.id_bu);
        id_bu.setOnClickListener(this);


        bluetoothConfigure();
    }


    /**
     * 蓝牙配置
     */
    public void bluetoothConfigure() {
        bt = new BluetoothSPP(this);

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , R.string.bluetool_not_available
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                MotorBluetoothAdapter.getInstance().receiveFromBluetooth(data);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            public void onDeviceConnected(String name, String address) {
                MotorBluetoothAdapter.getInstance().connectedSuccess();
                if (isConnectBltFirst) {
                    isConnectBltFirst = false;
                    //第一次连接上蓝牙以后做处理
//                    chargeInit();
                }
            }

            public void onDeviceDisconnected() {
                MotorBluetoothAdapter.getInstance().connectLost();
            }

            public void onDeviceConnectionFailed() {
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        MotorBluetoothAdapter.getInstance().setBt(bt);
        boolean ismConnected = MotorBluetoothAdapter.getInstance().ismConnected();
        if(ismConnected){
            id_bu.setVisibility(View.GONE);
        }else{
            id_bu.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
                SharePreferenceTool.setSharePreferenceValue(MainActivity.this, SharePreferenceTool.PREF_CONNECT_BLUTTOOTH_UID, address);
                SharePreferenceTool.setSharePreferenceValue(MainActivity.this, SharePreferenceTool.PREF_DUID, address);

//                chooseBtSuccessBack();

                //updateBtidInfo(address);
//                MotorBluetoothAdapter.getInstance().connect(address);

                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    public void setup() {
        checkChooseBluetooth();
    }

    public void checkChooseBluetooth() {
        String bltUid = SharePreferenceTool.getSharePreferenceValue(MainActivity.this, SharePreferenceTool.PREF_CONNECT_BLUTTOOTH_UID);
        Log.d("bltUid",bltUid);
        //如果没有选择设置蓝牙，选择蓝牙
        if (bltUid.equals("")) {
            //不需要连接了，checkauth失败会让重新匹配蓝牙
            //go2ConnectBt();
        } else {
            MotorBluetoothAdapter.getInstance().connect(bltUid);
            //updateBtidInfo(bltUid);
        }

    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//
//        if(!bt.isBluetoothEnabled()) {
//            bt.enable();
//        } else {
//            if(!bt.isServiceAvailable()) {
//                bt.setupService();
//                bt.startService(BluetoothState.DEVICE_OTHER);
//                setup();
//            }
//        }
//    }
    /**
     * 去连接蓝牙
     */
    public void go2ConnectBt() {
        if(bt.isBluetoothEnabled()){
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.id_bu:
                go2ConnectBt();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.stopService();

    }

}
