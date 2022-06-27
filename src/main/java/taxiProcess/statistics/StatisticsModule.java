package taxiProcess.statistics;

import beans.StatisticsData;
import simulators.Measurement;
import simulators.PM10Buffer;
import simulators.PM10Simulator;
import taxiProcess.Taxi;

import java.time.Instant;
import java.util.List;
//Every 15 seconds, each Taxi has to compute and communicate to the administrator server the following local statistics
public class StatisticsModule extends Thread{
    Taxi taxi;
    PM10Buffer pm10Buffer;

    Double km;
    Integer ridesCount;

    public StatisticsModule(Taxi t, PM10Buffer pm10Buffer){
        taxi = t;
        this.pm10Buffer = pm10Buffer;
    }
    @Override
    public void run() {
        while (true){
            try {Thread.sleep(15000);} catch (InterruptedException e) {e.printStackTrace();}
            sendToServer(taxi.getId(), System.currentTimeMillis(), taxi.getBatteryLevel());
        }
    }

    private void sendToServer(String taxiId, long timestamp, Double batteryLevel){
        String postPath = "/statistics/add";
        List<Measurement> pollutionAverages = pm10Buffer.readAllAndClean();
        StatisticsData data = new StatisticsData(taxiId, timestamp, batteryLevel, km, ridesCount, pollutionAverages);

        //TODO clear km and ridesCoud, also taxi have to increment it

    }
}
