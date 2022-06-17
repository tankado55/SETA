package taxiProcess;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class RideHandler {

    public static void startGrpcServices(int port){
        try {

            Server server = ServerBuilder.forPort(port).addService(new RideHandlingImpl()).build();

            server.start();

            System.out.println("Server started!");

            server.awaitTermination();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (InterruptedException e) {

            e.printStackTrace();

        }
    }
}
