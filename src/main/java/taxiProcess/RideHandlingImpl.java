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
    }
}
