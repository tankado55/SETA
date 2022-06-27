package adminServer;

import beans.StatisticsData;

import java.util.ArrayList;
import java.util.List;

public class StatisticsManager {
    private List<StatisticsData> statisticsData = new ArrayList<>();

    private static StatisticsManager instance;

    //singleton
    public synchronized static StatisticsManager getInstance(){
        if(instance==null)
            instance = new StatisticsManager();
        return instance;
    }

    public synchronized void add(StatisticsData data){
        statisticsData.add(data);
    }
}
