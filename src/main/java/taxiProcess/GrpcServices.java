package taxiProcess;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServices extends Thread{

    private static GrpcServices instance;
    private int port;

    String serverState = "ThreadState";

    private GrpcServices(){}

    public static GrpcServices getInstance(){
        if (instance == null)
            instance = new GrpcServices();

        return  instance;
    }
    @Override
    public void run(){

        try {

            Server server = ServerBuilder.forPort(port).addService(new RideHandlingImpl()).addService(new GreetingsImpl()).addService(new RechargeServicesImpl()).build();

            System.out.println("Starting Grpc server ... ");

            server.start();

            System.out.println("Grpc Server started!");
            serverState = "Server Started";

            server.awaitTermination();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (InterruptedException e) {

            e.printStackTrace();

        }
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public synchronized String getServerState(){
        return serverState;
    }
}
