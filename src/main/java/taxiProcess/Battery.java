package taxiProcess;

import com.google.protobuf.Timestamp;

import java.time.Instant;

public class Battery {
    private double level = 100;
    private Boolean toRecharge = false;
    private Instant time;
    public Boolean isCharging = false;

    public Battery(){}

    public double getLevel() {
        return level;
    }

    public Boolean toRecharge() {
        return toRecharge;
    }

    public void setTriggerForRechargeAfterRideCompleted() {
        toRecharge = true;
    }

    public void removeTriggerForRechargeAfterRideCompleted() {
        toRecharge = false;
    }

    public void discharge(double quantity){
        level -= quantity;
    }

    public void setCurrentTimestamp(){
        time = Instant.now();
    }

    public Timestamp getRequestTimestamp(){
        return Timestamp.newBuilder().setSeconds(time.getEpochSecond())
                .setNanos(time.getNano()).build();
    }

    public Instant getRequestInstant(){
        return time;
    }

    public void makeRecharge(){
        synchronized (isCharging){
            isCharging = true;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            level = 100.0;
            System.out.println("Recharge Completed!");
            isCharging = false;
        }
    }
}
