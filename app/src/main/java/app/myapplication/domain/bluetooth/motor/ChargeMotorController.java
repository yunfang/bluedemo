package app.myapplication.domain.bluetooth.motor;


/**
 * Created by windern on 2016/1/18.
 */
public class ChargeMotorController extends MotorController<ChargeMotor>{
    public ControlMessage changeChargeStatus(int chargeStatus){
        ControlMessage controlMessage = new ControlMessage();
        controlMessage.setMotorNum(motor.getNum());
        controlMessage.setControlType(ControlType.CHARGE);
        controlMessage.setValue0(chargeStatus);
        return controlMessage;
    }
}
