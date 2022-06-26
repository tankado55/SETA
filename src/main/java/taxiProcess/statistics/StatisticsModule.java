package taxiProcess.statistics;

import java.time.Instant;
import java.util.List;
//Every 15 seconds, each Taxi has to compute and communicate to the administrator server the following local statistics
public class StatisticsModule {
    Double km;
    Integer ridesCount;
    List<Float> pollutionLevels;

    private void sendToServer(String taxiId, Instant timestamp, Double batteryLevel){

    }
}
