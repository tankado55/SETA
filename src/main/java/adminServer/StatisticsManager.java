package adminServer;

import beans.StatisticsAverages;
import beans.StatisticsData;
import simulators.Measurement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsManager {
    // private List<StatisticsData> statisticsData = new ArrayList<>();
    private HashMap<String, List<StatisticsData>> statisticsDataMap = new HashMap<>();

    private static StatisticsManager instance;

    //singleton
    public synchronized static StatisticsManager getInstance(){
        if(instance==null)
            instance = new StatisticsManager();
        return instance;
    }

    public synchronized void add(StatisticsData data){
        List<StatisticsData> list = statisticsDataMap.get(data.getTaxiId());
        if (list == null){
            list = new ArrayList<StatisticsData>();
        }
        list.add(data);
        statisticsDataMap.put(data.getTaxiId(), list);
    }

    public synchronized StatisticsAverages getAverages(String taxiId, int n){
        List<StatisticsData> dataList = statisticsDataMap.get(taxiId);
        List<StatisticsData> subDataList = dataList.subList(Math.max(dataList.size() - n, 0), dataList.size());
        double rideCountAverage = subDataList.stream().mapToDouble(StatisticsData::getRidesCount).average().orElse(0.0);
        double kmAverage = subDataList.stream().mapToDouble(StatisticsData::getKm).average().orElse(0.0);
        double batteryAverage = subDataList.stream().mapToDouble(StatisticsData::getBatteryLevel).average().orElse(0.0);
        double pollutionAverage = subDataList.stream()
                                                .mapToDouble(
                                                        statisticsData -> statisticsData.getPollutionAverages()
                                                                                        .stream()
                                                                                        .mapToDouble(value -> value)
                                                                                        .average().orElse(0.0)
                                                ).average().orElse(0.0);
        return new StatisticsAverages(taxiId, rideCountAverage, kmAverage, batteryAverage, pollutionAverage);
    }
}
