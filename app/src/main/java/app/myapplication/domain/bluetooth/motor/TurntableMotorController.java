package app.myapplication.domain.bluetooth.motor;


/**
 * Created by windern on 2016/1/18.
 */
public class TurntableMotorController extends MotorController<TurntableMotor>{
    public ControlMessage setLocation(int location){
        ControlMessage controlMessage = new ControlMessage();
        controlMessage.setMotorNum(motor.getNum());
        controlMessage.setControlType(ControlType.SET);
        controlMessage.setValue0(location);
        return controlMessage;
    }

    public ControlMessage getLocation(){
        ControlMessage controlMessage = new ControlMessage();
        controlMessage.setMotorNum(motor.getNum());
        controlMessage.setControlType(ControlType.GET);
        return controlMessage;
    }
}
