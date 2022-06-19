package taxiProcess;

import beans.TaxiInfo;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.GreetingsServiceGrpc;
import it.ewlab.ride.GreetingsServiceOuterClass.*;

public class GreetingsImpl extends GreetingsServiceGrpc.GreetingsServiceImplBase {

    @Override
    public void greeting(GreetingsRequest request, StreamObserver<GreetingsResponse> responseObserver) {
        Taxi taxi = Taxi.getInstance();
        TaxiInfo info = new TaxiInfo(request.getTaxiInfoMsg());
        taxi.addTaxiInfo(info);

        GreetingsResponse response = GreetingsResponse.newBuilder().setOk(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void bye(GreetingsRequest request, StreamObserver<GreetingsResponse> responseObserver) {
        Taxi taxi = Taxi.getInstance();
        TaxiInfo info = new TaxiInfo(request.getTaxiInfoMsg());
        taxi.removeTaxiInfo(info);

        GreetingsResponse response = GreetingsResponse.newBuilder().setOk(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
