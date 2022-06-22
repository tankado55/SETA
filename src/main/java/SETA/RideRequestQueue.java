package SETA;

import java.util.ArrayList;

public class RideRequestQueue {

    private ArrayList<RideRequest> rides = new ArrayList<RideRequest>();
    private int availableTaxi = 0;

    // remember: the lock is on the whole object, I could use synchronized block in the next two methods locking
    // different resource but probably not worth

    public synchronized void put(RideRequest ride) {
        rides.add(ride);
        if (availableTaxi > 0)
            notify();
    }

    public synchronized void addAvailableTaxi(){
        ++availableTaxi;
        if (rides.size() > 0)
            notify();
    }

    public synchronized  void removeAvailableTaxi(){
        if (availableTaxi < 1) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        --availableTaxi;
    }

    public synchronized RideRequest take() {
        RideRequest ride = null;

        while(rides.size() == 0 || availableTaxi == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(rides.size() > 0 && availableTaxi > 0){
            ride = rides.get(0);
            rides.remove(0);
            --availableTaxi;
        }

        return ride;
    }

    public int getSize(){
        return rides.size();
    }
}
