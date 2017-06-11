package app.myapplication.domain.bluetooth.tradition;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.uyu.device.library.BluetoothSPP;

import java.util.Arrays;

import app.myapplication.ToastUtil;
import app.myapplication.domain.bluetooth.ComConfig;
import app.myapplication.domain.bluetooth.ComFrame;
import app.myapplication.domain.bluetooth.motor.MotorBus;

/**
 * 蓝牙中间连接器，管理蓝牙的状态，发送、接收消息
 * Created by windern on 2016/3/22.
 */
public class MotorBluetoothAdapter {
    private static MotorBluetoothAdapter instatnce = null;
    public static MotorBluetoothAdapter getInstance(){
        if(instatnce==null){
            instatnce = new MotorBluetoothAdapter();
        }
        return instatnce;
    }

    private MotorBluetoothAdapter() {

    }

    private boolean mConnected = false;

    public boolean ismConnected() {
        return mConnected;
    }

    private BluetoothSPP bt;
    private BltConnectService bltConnectService;

    public BluetoothSPP getBt() {
        return bt;
    }

    public void setBt(BluetoothSPP bt) {
        this.bt = bt;
    }

    public BltConnectService getBltConnectService() {
        return bltConnectService;
    }

    public void setBltConnectService(BltConnectService bltConnectService) {
        this.bltConnectService = bltConnectService;
    }

    /**
     * 连接蓝牙
     * @param address
     */
    public void connect(String address){
        Log.d("connect",address);
        if(bt!=null) {
            bt.connect(address);
        }
    }

    /**
     * 蓝牙连接成功
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void connectedSuccess(){
        mConnected = true;
        if(bltConnectService!=null) {
            ToastUtil.showShortToast(bltConnectService.getApplicationContext(),"蓝牙连接成功");
            bltConnectService.stopListen();
        }

        //连接成功后，直接发送当前要处理的消息
        MotorBus.getInstance().handleNowMessageSet();
    }

    /**
     * 蓝牙连接失败
     */
    public void connectLost(){
        mConnected = false;
        if(bltConnectService!=null) {
            ToastUtil.showShortToast(bltConnectService.getApplicationContext(), "蓝牙丢失");
            bltConnectService.startListen();
        }
    }

    /**
     * 发送蓝牙字节消息
     * 如果超过20个字节，分多次发送
     * @param WriteBytes
     * @return
     */
    public boolean sendFromMotorBus(byte[] WriteBytes){
        if(ismConnected()) {
            byte[] value = new byte[20];
            value[0] = (byte) 0x00;

            int loopCount = (WriteBytes.length % 20 == 0) ? (WriteBytes.length / 20) : (WriteBytes.length / 20 + 1);

            for (int i = 0; i < loopCount; i++) {
                byte[] copywrite = Arrays.copyOfRange(WriteBytes, i * 20, (i + 1) * 20);
                if (i == loopCount - 1) {
                    int leftCount = WriteBytes.length % 20;
                    if (leftCount > 0) {
                        copywrite = Arrays.copyOfRange(WriteBytes, i * 20, i * 20 + leftCount);
                    }
                }

                Log.d("send:copywrite",ComConfig.bytesToString(copywrite));
                bt.send(copywrite, false);
            }
            return true;
        }else{
            return false;
        }
    }

    /**
     * 接收蓝牙字节消息
     * @param bytes
     */
    public void receiveFromBluetooth(byte[] bytes){
        Log.d("receiveFromBluetooth",ComConfig.bytesToString(bytes));
        ComFrame.getInstance().receiveBytes(bytes);
    }
}
