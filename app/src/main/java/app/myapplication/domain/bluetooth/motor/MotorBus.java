package app.myapplication.domain.bluetooth.motor;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.myapplication.domain.bluetooth.ByteCommand;
import app.myapplication.domain.bluetooth.ComConfig;
import app.myapplication.domain.bluetooth.ComFrame;
import app.myapplication.domain.bluetooth.CommandConvertException;
import app.myapplication.domain.bluetooth.tradition.MotorBluetoothAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by windern on 2016/1/18.
 */
public class MotorBus {
    public ScreenMotorController screenMotCtrl;
    public BaffleMotorController baffleMotCtrl;
    public PupilMotorController pupilMotCtrl;
    public TurntableMotorController leftTurntableMotCtrl;
    public TurntableMotorController rightTurntableMotCtrl;
    public ButtonMotorController buttonMotCtrl;
    public ChargeMotorController chargeMotCtrl;

    private SocketService socketService;

    /**
     * 指令列表，记录每个指令的
     */
    private ArrayList<ControlMessage> listMessages = new ArrayList<>();
    /**
     * 指令集队列
     */
    private LinkedList<ControlMessageSet> queue = new LinkedList<>();
    private ControlMessageSet nowMessageSet = null;

    private OnButtonPressListener onButtonPressListener;
    private OnChangeChargeListener onChangeChargeListener;

    public OnButtonPressListener getOnButtonPressListener() {
        return onButtonPressListener;
    }

    public void setOnButtonPressListener(OnButtonPressListener onButtonPressListener) {
        this.onButtonPressListener = onButtonPressListener;
    }

    public OnChangeChargeListener getOnChangeChargeListener() {
        return onChangeChargeListener;
    }

    public void setOnChangeChargeListener(OnChangeChargeListener onChangeChargeListener) {
        this.onChangeChargeListener = onChangeChargeListener;
    }

    private static MotorBus instatnce = null;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static MotorBus getInstance(){
        if(instatnce==null){
            instatnce = new MotorBus();
        }
        return instatnce;
    }

    /**
     * 构造函数，虚拟对应所有的电机
     */
    private MotorBus(){
        screenMotCtrl = new ScreenMotorController();
        LineMotor screenMotor = new LineMotor();
        screenMotor.setNum(1);
        screenMotCtrl.setMotor(screenMotor);
        screenMotCtrl.setMotorBus(this);

        baffleMotCtrl = new BaffleMotorController();
        LineMotor baffleMotor = new LineMotor();
        baffleMotor.setNum(2);
        baffleMotCtrl.setMotor(baffleMotor);
        baffleMotCtrl.setMotorBus(this);

        pupilMotCtrl = new PupilMotorController();
        GapMotor pupilMotor = new GapMotor();
        pupilMotor.setNum(3);
        pupilMotCtrl.setMotor(pupilMotor);
        pupilMotCtrl.setMotorBus(this);

        leftTurntableMotCtrl = new TurntableMotorController();
        TurntableMotor leftTurntableMotor = new TurntableMotor();
        leftTurntableMotor.setNum(5);
        leftTurntableMotCtrl.setMotor(leftTurntableMotor);
        leftTurntableMotCtrl.setMotorBus(this);

        rightTurntableMotCtrl = new TurntableMotorController();
        TurntableMotor rightTurntableMotor = new TurntableMotor();
        rightTurntableMotor.setNum(4);
        rightTurntableMotCtrl.setMotor(rightTurntableMotor);
        rightTurntableMotCtrl.setMotorBus(this);

        //大按键
        buttonMotCtrl = new ButtonMotorController();
        ButtonMotor buttonMotor = new ButtonMotor();
        buttonMotor.setNum(6);
        buttonMotCtrl.setMotor(buttonMotor);
        buttonMotCtrl.setMotorBus(this);

        //充电控制
        chargeMotCtrl = new ChargeMotorController();
        ChargeMotor chargeMotor = new ChargeMotor();
        chargeMotor.setNum(7);
        chargeMotCtrl.setMotor(chargeMotor);
        chargeMotCtrl.setMotorBus(this);

        //防止命令解析错误，每隔500毫秒重发消息
        startLoopTimer();
    }

    public void setSocketService(SocketService socketService) {
        this.socketService = socketService;
    }

    private Subscription loopTimer;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startLoopTimer(){
        loopTimer = Observable.interval(500, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(time->{
                    handleNowMessageSet();
                });
    }

    public void stopLoopTimer(){
        if(loopTimer!=null && !loopTimer.isUnsubscribed()){
            loopTimer.unsubscribe();
            loopTimer = null;
        }
    }

    /**
     * 直接发送指令
     * @param controlMessageSet
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public PublishSubject<ControlMessageSet> sendMessageSetDirect(ControlMessageSet controlMessageSet){
//        Timber.d("sendMessageSet:%s",controlMessageSet.toString());
        PublishSubject<ControlMessageSet> publishSubject = PublishSubject.create();
        controlMessageSet.setPublishSubject(publishSubject);

        ComFrame comFrame = ComFrame.getInstance();
        try {
            if(controlMessageSet!=null) {
                ArrayList<ControlMessage> notReadyMessageList = controlMessageSet.getNotReadyMessageList();
                byte[] bytes = comFrame.computeSendByte(notReadyMessageList);
                String bytesString = ComConfig.bytesToHexString(bytes);
                Log.d("NowMessageSet,string",notReadyMessageList.toString());
                Log.d("NowMessageSet,bytes",bytesString);


                MotorBluetoothAdapter.getInstance().sendFromMotorBus(bytes);
            }
        } catch (CommandConvertException e) {
            e.printStackTrace();
        }

        return publishSubject;
    }

    /**
     * 发送指令进队列
     * @param controlMessageSet
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public PublishSubject<ControlMessageSet> sendMessageSet(ControlMessageSet controlMessageSet){
//        Timber.d("sendMessageSet:%s",controlMessageSet.toString());
        PublishSubject<ControlMessageSet> publishSubject = PublishSubject.create();
        controlMessageSet.setPublishSubject(publishSubject);

        queue.offer(controlMessageSet);
        handleNextMessageSet();

        return publishSubject;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void handleNextMessageSet(){
        //当前没有执行的命令
        if(nowMessageSet==null) {
            if (queue.size() > 0) {
                nowMessageSet = queue.poll();

                //只加入一次
                for(int i=0;i<nowMessageSet.getList().size();i++){
                    ControlMessage controlMessage = nowMessageSet.getList().get(i);
                    controlMessage.setSeqNum(listMessages.size()+1);
                    listMessages.add(controlMessage);
                }

                handleNowMessageSet();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void handleNowMessageSet(){
        ComFrame comFrame = ComFrame.getInstance();
        try {
            if(nowMessageSet!=null) {
                ArrayList<ControlMessage> notReadyMessageList = nowMessageSet.getNotReadyMessageList();
                byte[] bytes = comFrame.computeSendByte(notReadyMessageList);
                String bytesString = ComConfig.bytesToHexString(bytes);
                Log.d("NowMessageSet,string",notReadyMessageList.toString());
                Log.d("NowMessageSet,bytes",bytesString);

                MotorBluetoothAdapter.getInstance().sendFromMotorBus(bytes);
            }else{
                //如果处理当前的nowMessageSet已经完成，就直接执行下一个
                handleNextMessageSet();
            }
        } catch (CommandConvertException e) {
            e.printStackTrace();
        }
    }

    public void receiveButtonCommand(){
        Log.d("receiveButtonCommand","receiveButtonCommand");
        if(onButtonPressListener!=null){
            Observable.just(1).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result->{
                        onButtonPressListener.onButtonPress();
                    });
        }
    }

    public void receiveChangeChargeCommand(int chargeStatus){
        Log.d("receiveChangeChargeCom","receiveChangeChargeCommand");
        if(onButtonPressListener!=null){
            Observable.just(1).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result->{
                        onChangeChargeListener.onChangeCharge(chargeStatus);
                    });
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void receiveBackByteCommand(ByteCommand[] arrayByteCommand){
        if(arrayByteCommand!=null) {
            BackMessageSet backMessageSet = new BackMessageSet();
            for (int i = 0; i < arrayByteCommand.length; i++) {
                ByteCommand byteCommand = arrayByteCommand[i];
                BackMessage backMessage = byteCommand.createBackMessage(nowMessageSet);
                if(backMessage!=null){
                    if (backMessage.isButtonPressMessage()) {
                        //是按钮消息seq_num=0,值为1执行收到按钮的消息
                        if (backMessage.getFlag() == FlagType.SUCCESS && backMessage.getValue() == 1) {
                            receiveButtonCommand();
                        }
                    } else if(backMessage.isChangeChargeMessage()){
                        //是按钮消息seq_num=-1执行收到充电状态变化的消息
                        if (backMessage.getFlag() == FlagType.SUCCESS) {
                            receiveChangeChargeCommand(backMessage.getValue());
                        }
                    } else {
                        backMessageSet.getList().add(backMessage);
                    }
                }
            }
            Log.d("backMessageSet",backMessageSet.toString());
            receiveMotorMsg(backMessageSet);
        }else{
            //如果收到的消息为null，表示蓝牙又返回，但是不正确，需要继续发送当前的消息
            receiveMotorMsg(null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void receiveMotorMsg(BackMessageSet backMessageSet){
        if(backMessageSet!=null && backMessageSet.getList().size()>0) {
            if(nowMessageSet!=null) {
                nowMessageSet.handleBackMessageSet(backMessageSet);
                if (nowMessageSet.isReady()) {
                    //如果全部收到、去处理下一个命令集
                    //否则继续等待
                    //Handler handler = nowMessageSet.getHandler();
                    PublishSubject<ControlMessageSet> publishSubject = nowMessageSet.getPublishSubject();
                    if (publishSubject != null) {
                        publishSubject.onNext(nowMessageSet);
                        //nowMessageSet.getHandler().sendMessage(nowMessageSet.toMessage());
                    }
                    nowMessageSet = null;
                    handleNextMessageSet();
                } else {
                    handleNowMessageSet();
                }
            }
        }else{
            handleNowMessageSet();
        }
    }

    /**
     * 清空所有消息
     * @return
     */
    public boolean removeAllMessage(){
        List<ControlMessageSet> tmpList = new ArrayList<>();
        return queue.removeAll(tmpList);
    }


//    /**
//     * 重置
//     */
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    public void reset(){
//        Timber.d("发送电机充值命令");
//        ControlMessageSet messageSet = TrainUseCase.reset();
//        MotorBus.getInstance().sendMessageSet(messageSet);
//    }

//    /**
//     * 设置瞳距
//     */
//    public void adjustPupilDistance(){
//        ControlMessageSet messageSet = TrainUseCase.adjustPupileDistance(TrainConfig.PupilDefaultDistance);
//        MotorBus.getInstance().sendMessageSet(messageSet);
//    }
}
