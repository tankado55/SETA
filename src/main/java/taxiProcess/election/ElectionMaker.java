package taxiProcess.election;

import SETA.RideRequest;
import beans.TaxiInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import it.ewlab.ride.RideHandlingServiceGrpc;
import it.ewlab.ride.RideHandlingServiceOuterClass;
import taxiProcess.RideAcquisition;
import taxiProcess.Taxi;

import java.util.ArrayList;
import java.util.List;

public class ElectionMaker extends Thread{

    private Taxi taxi;
    private ElectionQueue queue;

    public ElectionMaker(Taxi taxi, ElectionQueue queue){
        this.taxi = taxi;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true){
            RideRequest ride = queue.take();
            startElection(ride);
        }
    }

    private void startElection(RideRequest ride){
        synchronized (taxi.electionLock){
            taxi.setCurrentRideId(ride.getId());
            taxi.setElectionLock(true);
        }

        System.out.println("Starting Ride" + ride.getId() + " election!");

        List<TaxiInfo> currentContacts;
        RideAcquisition rideAcquisition = null;
        synchronized (taxi.taxiContacts) {
            rideAcquisition = new RideAcquisition(taxi.taxiContacts.size(), ride);
            currentContacts = new ArrayList<>(taxi.taxiContacts);
        }

        List<Thread> requestThreads = new ArrayList<Thread>();
        RideRequest finalRide = ride;
        RideAcquisition finalRideAcquisition = rideAcquisition;
        Double distanceFromRide = taxi.getDistance(ride.getStartingPosition());
        for (TaxiInfo taxiContact : currentContacts){
            String target = taxiContact.getIp() + ":" + taxiContact.getPort();
            // a new thread is launched for each message to send
            Thread requestThread = new Thread(() -> {
                final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

                //creating a blocking stub on the channel
                RideHandlingServiceGrpc.RideHandlingServiceBlockingStub stub = RideHandlingServiceGrpc.newBlockingStub(channel);

                //creating the HelloResponse object which will be provided as input to the RPC method
                RideHandlingServiceOuterClass.RideHandlingRequest request = RideHandlingServiceOuterClass.RideHandlingRequest.newBuilder()
                        .setRideRequestMsg(finalRide.toMsg())
                        .setDistance(distanceFromRide)
                        .setBattery((int)taxi.getBattery().getLevel())
                        .setTaxiId(taxi.getId()).build();
                System.out.println("I want Ride" +  finalRide.getId() + " my distance: " + distanceFromRide + " " +  taxi.getPosition().getX() + " " + taxi.getPosition().getY());
                RideHandlingServiceOuterClass.RideHandlingReply response = stub.startRideHandling(request);

                if (!response.getDiscard()){
                    synchronized (finalRideAcquisition){
                        finalRideAcquisition.acked();
                    }
                }
                System.out.println("Received reply Ride" + finalRide.getId() + ", discard? " + response.getDiscard() + " from " + taxiContact.getId());

                //closing the channel
                channel.shutdown();
            });
            requestThread.start();
            requestThreads.add(requestThread);// save the threads to make the join
        }

        for (Thread t : requestThreads){
            try {
                t.join(); //necessaria per attendere le delayedReply
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (finalRideAcquisition.getAckToReceive() <= 0){
            taxi.handleRide(finalRide);
        }
        else{
            System.out.println("\u001B[33m" + "Ride " + ride.getId() + " taken by another taxi" + "\u001B[0m");
            taxi.clearRide(ride.getId());
            taxi.checkExitStatus();
        }
        taxi.setElectionLock(false);




    }

    public void killThreads(List<Thread> threads){
        for (Thread t : threads){
            t.interrupt();
        }
    }
}
