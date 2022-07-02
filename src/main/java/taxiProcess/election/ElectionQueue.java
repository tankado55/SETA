package taxiProcess.election;

import SETA.RideRequest;
import taxiProcess.Taxi;

import java.util.LinkedList;

public class ElectionQueue {

    private LinkedList<RideRequest> rides = new LinkedList<RideRequest>();

    public synchronized void put(RideRequest ride) {
        if (!contains(ride) && !ride.getId().equals(Taxi.getInstance().getCurrentRideId())){
            rides.add(ride);
            notify();
            System.out.println("Added ride" + ride.getId() + " in queue");
        }
    }

    public synchronized RideRequest take() {
        RideRequest ride = null;

        while(rides.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(rides.size() > 0){
            ride = rides.getFirst();
            rides.removeFirst();
        }

        return ride;
    }

    private synchronized Boolean contains(RideRequest ride){
        for (RideRequest thisRide : rides){
            if (ride.getId().equals(thisRide.getId())){
                return true;
            }
        }
        return false;
    }

    public synchronized void clear(){
        rides.clear();
    }
}
