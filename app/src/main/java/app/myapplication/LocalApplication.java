package app.myapplication;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import app.myapplication.domain.bluetooth.tradition.BltConnectService;
import app.myapplication.domain.bluetooth.tradition.MotorBluetoothAdapter;

/**
 * Created by zhouyunfang on 17/6/3.
 */

public class LocalApplication extends Application implements ServiceConnection {


    @Override
    public void onCreate() {
        super.onCreate();
        startBltConnectService();
        bindBltConnectService();

    }

    public void startBltConnectService () {
        Intent it = new Intent(this, BltConnectService.class);
        startService(it);
    }

    private void bindBltConnectService () {
        Intent it = new Intent (this, BltConnectService.class);
        this.bindService(it, this, Service.BIND_AUTO_CREATE);
    }
    //BltConnect服务
    private BltConnectService bltConnectService;
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        if (service instanceof BltConnectService.BltConnectServiceBinder) {
            BltConnectService.BltConnectServiceBinder binder = (BltConnectService.BltConnectServiceBinder)service;
            bltConnectService = binder.getService();
            MotorBluetoothAdapter.getInstance().setBltConnectService(bltConnectService);
            bltConnectService.startListen();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Toast.makeText(this, "onServiceDisconnected name=" + componentName, Toast.LENGTH_LONG).show();
    }
}
