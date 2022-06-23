package taxiProcess;

public class Battery {
    private double level = 100;
    private Boolean toRecharge = false;

    public Battery(){}

    public double getLevel() {
        return level;
    }

    public Boolean toRecharge() {
        return toRecharge;
    }

    public void startRecharge() {
        // TODO refactor
        Taxi taxi = Taxi.getInstance();
        taxi.startExitRequest();
        toRecharge = true;
    }

    public void discarge(double quantity){
        level -= quantity;
    }
}
