package taxiProcess;

import SETA.RideRequest;
import beans.Position;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RideHandlingServiceGrpc;
import it.ewlab.ride.RideHandlingServiceOuterClass.*;

public class RideHandlingImpl extends RideHandlingServiceGrpc.RideHandlingServiceImplBase {

    @Override
    public void startRideHandling(RideHandlingRequest request, StreamObserver<RideHandlingReply> responseObserver){

        Taxi taxi = Taxi.getInstance();
        RideRequest rideReq = new RideRequest(request.getRideRequestMsg());
        double myDistance = taxi.getDistance(rideReq.getStartingPosition());

        // different district, always discard FALSE
        synchronized (taxi.getPosition()) {
            Position start = new Position(request.getRideRequestMsg().getStart());
            System.out.println("Debug linea 20: ride" + start.getDistrict() + "taxi: " + taxi.getPosition().getDistrict());
            if (taxi.getPosition().getDistrict() != start.getDistrict()) {
                System.out.println("ride" + request.getRideRequestMsg().getId() + "I'm in another district , I don't want the ride!");
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
        }

            synchronized (taxi.busy) {
                if (taxi.busy) {
                    if (taxi.getCurrentRideId().equals(request.getRideRequestMsg().getId())) {
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    } else {
                        System.out.println("ride" + request.getRideRequestMsg().getId() + "I'm busy, I don't want the ride!");
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                    return;
                }
            }

        //synchronized (taxi.electionLock){
            if (!taxi.electionLock){
                if (!taxi.getCurrentRideId().equals(request.getRideRequestMsg().getId())){
                    RideRequest ride = new RideRequest(request.getRideRequestMsg());
                    System.out.println("should I put this ride in my queue?" + "debug, mydistrict " + taxi.getPosition().getDistrict() + "ride-district " + ride.getStartingPosition().getDistrict()); // yes because when this taxi finish a ride it delete the queue and if some ride is not yet completed someone will ask
                    synchronized (taxi.getPosition()) {
                        if (ride.getStartingPosition().getDistrict() == taxi.getPosition().getDistrict())
                            taxi.electionQueue.put(new RideRequest(request.getRideRequestMsg()));
                    }
                }
                else { // it means that the ride is taken by another taxi // it need in case some taxi request the resource after this taxi finished the election and it have not win the election
                    System.out.println("ride" + request.getRideRequestMsg().getId() +"I'm not in election, I release the resource because someone else had taken");
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            }
        //}

        // not the right time
        try{
            int requestId = Integer.parseInt(rideReq.getId());
            int current = Integer.parseInt(taxi.currentRide);
        if (requestId < current){
            System.out.println("ok, you can take it, i'm on the successive");
            RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        } catch (Throwable t){System.out.println("no current ride");}

        if (request.getDistance() < taxi.getDistance(rideReq.getStartingPosition())){
            RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        else if (request.getDistance() == myDistance){
            if (request.getBattery() > taxi.getBatteryLevel()){
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            else if (request.getBattery() == taxi.getBatteryLevel()){
                if (Integer.parseInt(request.getTaxiId()) < Integer.parseInt(taxi.getId())){
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            }
            System.out.println("batteria request minore, delayed");
        }
        System.out.println("Debug Distance: ride" + rideReq.getId() + " reqDistance " + request.getDistance() + " my " + taxi.getDistance(rideReq.getStartingPosition()) + " " +  taxi.getPosition().getX() + " " + taxi.getPosition().getY());
        taxi.addDelayedResponse(new DelayedResponse(new RideRequest(request.getRideRequestMsg()), responseObserver));

        // here the request must be delayed to respond OK or discard later

    }
}
