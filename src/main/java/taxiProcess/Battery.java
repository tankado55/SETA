package taxiProcess;

public class Battery {
    private int level = 100;
    private Boolean toRecharge = false;

    public Battery(){}

    public int getLevel() {
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
}
