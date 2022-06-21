package taxiProcess;

import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RideHandlingServiceOuterClass;

public class DelayedResponse {

    String rideId;
    StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> observer;

    public DelayedResponse(String rideId, StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> observer){
        this.rideId = rideId;
        this.observer = observer;
    }

    public String getRideId() {
        return rideId;
    }

    public StreamObserver<RideHandlingServiceOuterClass.RideHandlingReply> getObserver() {
        return observer;
    }
}
