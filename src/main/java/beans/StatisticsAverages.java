package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatisticsAverages {
    private String taxiId;
    private double rideCountAverage;
    private double kmAverage;
    private double batteryAverage;
    private double pollutionAverage;

    public StatisticsAverages(){}
    public StatisticsAverages(String taxiId, double rideCountAverage, double kmAverage, double batteryAverage, double pollutionAverage) {
        this.taxiId = taxiId;
        this.rideCountAverage = rideCountAverage;
        this.kmAverage = kmAverage;
        this.batteryAverage = batteryAverage;
        this.pollutionAverage = pollutionAverage;
    }

    public String getTaxiId() {
        return taxiId;
    }

    public double getRideCountAverage() {
        return rideCountAverage;
    }

    public double getKmAverage() {
        return kmAverage;
    }

    public double getBatteryAverage() {
        return batteryAverage;
    }

    public double getPollutionAverage() {
        return pollutionAverage;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public void setRideCountAverage(double rideCountAverage) {
        this.rideCountAverage = rideCountAverage;
    }

    public void setKmAverage(double kmAverage) {
        this.kmAverage = kmAverage;
    }

    public void setBatteryAverage(double batteryAverage) {
        this.batteryAverage = batteryAverage;
    }

    public void setPollutionAverage(double pollutionAverage) {
        this.pollutionAverage = pollutionAverage;
    }
}
