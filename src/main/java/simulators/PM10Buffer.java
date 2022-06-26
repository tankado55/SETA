package simulators;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PM10Buffer implements Buffer{

    LinkedList<Measurement> measurements = new LinkedList<>();
    List<Measurement> averages = new ArrayList<>();

    int incrementalId = 0;


    @Override
    public synchronized void addMeasurement(Measurement m) {
        measurements.add(m);
        if (measurements.size() >=8){
            double sum = 0;
            for (Measurement measurement : measurements){
                sum+= measurement.getValue();
            }
            double average = sum/measurements.size();
            long lastTimestamp = measurements.getLast().getTimestamp();
            ++incrementalId;
            averages.add(new Measurement(String.valueOf(incrementalId), "averagePM10", average, lastTimestamp));
            measurements.removeFirst();
            measurements.removeFirst();
            measurements.removeFirst();
            measurements.removeFirst();
        }
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> result = new ArrayList<>(averages);
        // if another thread add a measurement here is a problem
        averages.clear();
        return result;
    }
}
