package taxiProcess;

import beans.Position;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RideHandlingServiceGrpc;
import it.ewlab.ride.RideHandlingServiceOuterClass.*;

public class RideHandlingImpl extends RideHandlingServiceGrpc.RideHandlingServiceImplBase {

    @Override
    public void RideHandling(RideHandlingRequest request, StreamObserver<RideHandlingReply> responseObserver){

        Taxi instance = Taxi.getInstance();
        double myDistance = instance.getDistance(new Position(request.getRideRequestMsg().getStart()));

        if (request.getDistance() < myDistance){
            RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        else if (request.getDistance() == myDistance){
            if (request.getBattery() < instance.getBattery){
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            else if (request.getBattery() == instance.getBattery){
                if (request.getTaxiId() < instance.getId){

                }
            }
        }

    }
}
