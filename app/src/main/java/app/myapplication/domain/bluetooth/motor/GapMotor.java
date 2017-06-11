package app.myapplication.domain.bluetooth.motor;

/**
 * Created by windern on 2016/1/18.
 */
public class GapMotor extends Motor{
    /**
     * 间距
     */
    private int distance = 0;

    public GapMotor(){
        motorType = MotorType.GAP;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}
