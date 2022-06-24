package taxiProcess;

import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RechargeServicesGrpc;
import it.ewlab.ride.RechargeServicesOuterClass.*;

import java.time.Instant;

public class RechargeServicesImpl extends RechargeServicesGrpc.RechargeServicesImplBase {

    @Override
    public void askPremiseToCharge(RechargeRequest request, StreamObserver<RechargeResponse> responseObserver) {
        Taxi taxi = Taxi.getInstance();

        RechargeResponse response = RechargeResponse.newBuilder().setOk(true).build();
        if (!taxi.wantToCharge || request.getDistrict() != taxi.getPosition().getDistrict()){
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        else{
            synchronized (taxi.getBattery().isCharging) {// this means I want to charge but i'm not charging
                Instant requestTimestamp = Instant.ofEpochSecond(request.getTime().getSeconds(), request.getTime().getNanos());
                if (requestTimestamp.isBefore(taxi.getBattery().getRequestInstant())) {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                } else taxi.addDelayedRechargeResponse(responseObserver);
            }
        }
    }
}

