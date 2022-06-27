package taxiProcess.statistics;

import java.util.ArrayList;
import java.util.List;

public class RidesStatsBuffer {
    private List<Double> kmRides;
    //private double lastBatteryLevel; // this was made in case it needed to kwnow the last statut of the battery at the end of a ride but it is not specified

    public RidesStatsBuffer(){
        kmRides = new ArrayList<>();
        //lastBatteryLevel = 0;
    }

    public synchronized void add(Double stat){
        kmRides.add(stat);
    }

    public synchronized RidesStats readAllAndClean(){
        double sum = kmRides.stream().mapToDouble(Double::doubleValue).sum();
        RidesStats result = new RidesStats(sum, kmRides.size());
        kmRides.clear();
        return result;
    }

}
