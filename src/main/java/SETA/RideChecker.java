package SETA;

import java.util.ArrayList;
import java.util.List;

public class RideChecker {

    private static RideChecker instance;

    private List<Integer> completedRides = new ArrayList<>();
    private int i = 0;

    public static RideChecker getInstance() {
        if (instance == null)
            instance = new RideChecker();
        return instance;
    }

    public synchronized void addRide(int rideId){
        if (completedRides.contains(rideId)){
            System.out.println("PROBLEM!!! ride " + rideId + " inserted two times!");
        }
        else{
            completedRides.add(rideId);
        }
        while (completedRides.contains(i)){
            ++i;
        }
        System.out.println("last sequential ride completed: " + (i-1));


    }
}
