package app.myapplication.domain.bluetooth.motor;

/**
 * Created by windern on 2016/1/18.
 */
public class ButtonMotorController extends MotorController<ButtonMotor>{
    public ControlMessage getButtonIsPress(){
        ControlMessage controlMessage = new ControlMessage();
        controlMessage.setMotorNum(motor.getNum());
        controlMessage.setControlType(ControlType.GET);
        return controlMessage;
    }
}
