package taxiProcess.statistics;

public class RidesStats {
    private double km;
    // private double batteryLevel;
    private int rideCount;

    public RidesStats(double km, int rideCount) {
        this.km = km;
        //this.batteryLevel = batteryLevel;
        this.rideCount = rideCount;
    }

    public double getKm() {
        return km;
    }

    public int getRideCount() {
        return rideCount;
    }
}
