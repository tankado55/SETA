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
        Position start = new Position(request.getRideRequestMsg().getStart());
        synchronized (taxi.getPosition()) {
            if (taxi.getPosition().getDistrict() != start.getDistrict()) {
                System.out.println("ride" + request.getRideRequestMsg().getId() + "I'm in another district , I don't want the ride" + request.getRideRequestMsg().getId());
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                taxi.electionQueue.discard(request.getRideRequestMsg().getId());
                return;
            }
        }

            //synchronized (taxi.busy) {
                if (taxi.busy) {
                    if (taxi.getCurrentRideId().equals(request.getRideRequestMsg().getId())) {
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    } else {
                        System.out.println("ride" + request.getRideRequestMsg().getId() + "I'm busy, I don't want the ride" + request.getRideRequestMsg().getId());
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        taxi.electionQueue.discard(request.getRideRequestMsg().getId());
                    }
                    return;
                }
            //}

        synchronized (taxi.electionLock){

            if (!taxi.electionLock && taxi.getPosition().getDistrict() == start.getDistrict()){
                System.out.println("in queue from impl");
                taxi.electionQueue.put(new RideRequest(request.getRideRequestMsg()));
            }
        }


        if (request.getDistance() < taxi.getDistance(rideReq.getStartingPosition())){
            System.out.println("You are more close than me, take the ride" + request.getRideRequestMsg().getId());
            RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            taxi.electionQueue.discard(request.getRideRequestMsg().getId());
            return;
        }
        else if (request.getDistance() == myDistance){
            if (request.getBattery() > taxi.getBatteryLevel()){
                System.out.println("You have more battery than me, take the ride" + request.getRideRequestMsg().getId());
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                taxi.electionQueue.discard(request.getRideRequestMsg().getId());
                return;
            }
            else if (request.getBattery() == taxi.getBatteryLevel()){
                if (Integer.parseInt(request.getTaxiId()) < Integer.parseInt(taxi.getId())){
                    System.out.println("You have a lower iod than me, take the ride" + request.getRideRequestMsg().getId());
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    taxi.electionQueue.discard(request.getRideRequestMsg().getId());
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
