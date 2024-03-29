package adminServer;

import beans.StatisticsAverages;
import beans.StatisticsData;
import beans.TaxiInfo;
import simulators.Measurement;

import java.util.*;
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

    public synchronized StatisticsAverages getStatisticsInterval(long t1, long t2){
        List<StatisticsData> dataList = new ArrayList<>();
        List<TaxiInfo> taxisInfo = TaxisInfoManager.getInstance().getTaxiInfoList();
        for (TaxiInfo taxiInfo : taxisInfo){
            List<StatisticsData> singleTaxiStats = statisticsDataMap.get(taxiInfo.getId());
            for (StatisticsData data : singleTaxiStats){
                if (data.getTimestamp() >= t1 && data.getTimestamp() < t2){
                    dataList.add(data);
                }
            }
        }

        double rideCountAverage = dataList.stream().mapToDouble(StatisticsData::getRidesCount).average().orElse(0.0);
        double kmAverage = dataList.stream().mapToDouble(StatisticsData::getKm).average().orElse(0.0);
        double batteryAverage = dataList.stream().mapToDouble(StatisticsData::getBatteryLevel).average().orElse(0.0);
        double pollutionAverage = dataList.stream()
                                                .mapToDouble(
                                                        statisticsData -> statisticsData.getPollutionAverages()
                                                                                        .stream()
                                                                                        .mapToDouble(value -> value)
                                                                                        .average().orElse(0.0)
                                                ).average().orElse(0.0);
        return new StatisticsAverages("noId", rideCountAverage, kmAverage, batteryAverage, pollutionAverage);
    }

    public synchronized StatisticsAverages getAverages(long t1, long t2){
        return null;
    }

    public synchronized long getFirstStatTimestamp(){
        List<Long> mins = new ArrayList<>();
        List<TaxiInfo> taxisInfo = TaxisInfoManager.getInstance().getTaxiInfoList();
        for (TaxiInfo taxiInfo : taxisInfo){
            String taxiId = taxiInfo.getId();
            mins.add(statisticsDataMap.get(taxiId).get(0).getTimestamp());
        }
        return Collections.min(mins);
    }

    public synchronized long getLastStatTimestamp(){
        List<Long> maxs = new ArrayList<>();
        List<TaxiInfo> taxisInfo = TaxisInfoManager.getInstance().getTaxiInfoList();
        for (TaxiInfo taxiInfo : taxisInfo){
            String taxiId = taxiInfo.getId();
            maxs.add(statisticsDataMap.get(taxiId).get(statisticsDataMap.get(taxiId).size() - 1).getTimestamp());
        }
        return Collections.max(maxs);
    }
}
