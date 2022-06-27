package beans;

import simulators.Measurement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatisticsData {

    String taxiId;
    long timestamp;
    double batteryLevel;
    double km;
    Integer ridesCount;
    ArrayList<Double> pollutionAverages;

    public StatisticsData() {}

    public StatisticsData(String taxiId, long timestamp, double batteryLevel, double km, Integer ridesCount, ArrayList<Double> pollutionAverages) {
        this.taxiId = taxiId;
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.km = km;
        this.ridesCount = ridesCount;
        this.pollutionAverages = pollutionAverages;
    }

    public String getTaxiId() {
        return taxiId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public double getKm() {
        return km;
    }

    public Integer getRidesCount() {
        return ridesCount;
    }

    public List<Double> getPollutionAverages() {
        return pollutionAverages;
    }
}
