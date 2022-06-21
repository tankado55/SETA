package taxiProcess;

import SETA.RideRequest;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RideHandlingServiceOuterClass;

public class DelayedResponse {

    RideRequest rideRequest;
    StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> observer;

    public DelayedResponse(RideRequest rideRequest, StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> observer){
        this.rideRequest = rideRequest;
        this.observer = observer;
    }

    public RideRequest getRideRequest() {
        return rideRequest;
    }

    public StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> getObserver() {
        return observer;
    }
}
