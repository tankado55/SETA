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
        double myDistance = taxi.getDistance(new Position(request.getRideRequestMsg().getStart()));

        // different district, always discard FALSE
        synchronized (taxi.getPosition()){
            Position start = new Position(request.getRideRequestMsg().getStart());
            if (taxi.getPosition().getDistrict() != start.getDistrict()){
                System.out.println("ride" + request.getRideRequestMsg().getId() +"I'm in another district, I don't want the ride!");
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
        }

        synchronized (taxi.busy){
            if (taxi.busy){
                if (taxi.getCurrentRideId().equals(request.getRideRequestMsg().getId())){
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
                else{
                    System.out.println("ride" + request.getRideRequestMsg().getId() +"I'm busy, I don't want the ride!");
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
                return;
            }
        }

        synchronized (taxi.electionLock){
            if (!taxi.electionLock){
                taxi.electionQueue.put(new RideRequest(request.getRideRequestMsg()));
            }
        }
        if (request.getDistance() < myDistance){
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
                if (Integer.valueOf(request.getTaxiId()) < Integer.valueOf(taxi.getId())){
                    RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            }
        }

        taxi.addDelayedResponse(new DelayedResponse(new RideRequest(request.getRideRequestMsg()), responseObserver));

        // here the request must be delayed to respond OK or discard later

    }
}
