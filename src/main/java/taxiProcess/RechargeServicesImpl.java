package taxiProcess;

import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RechargeServicesGrpc.*;
import it.ewlab.ride.RechargeServicesOuterClass.*;
import it.ewlab.ride.RideHandlingServiceOuterClass;

public class RechargeServicesImpl extends RechargeServicesImplBase {

    @Override
    public void askPremiseToCharge(RechargeRequest request, StreamObserver<RechargeResponse> responseObserver) {
        Taxi taxi = Taxi.getInstance();

        RechargeResponse response = RechargeResponse.newBuilder().setOk(true).build();
        if (!taxi.wantToCharge || request.getDistrict() != taxi.getPosition().getDistrict()){
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        else{
            synchronized (taxi.charging){
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }
}
