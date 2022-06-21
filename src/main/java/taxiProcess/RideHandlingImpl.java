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

        synchronized (taxi.getPosition()){
            Position start = new Position(request.getRideRequestMsg().getStart());
            if (taxi.getPosition().getDistrict() != start.getDistrict()){
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
        }
        if (request.getDistance() < myDistance){
            RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        else if (request.getDistance() == myDistance){
            if (request.getBattery() < taxi.getBattery()){
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            else if (request.getBattery() == taxi.getBattery()){
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
