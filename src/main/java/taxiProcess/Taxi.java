package taxiProcess;

import SETA.RideRequest;
import beans.BeansTest;
import beans.Position;
import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.RideHandlingServiceGrpc;
import it.ewlab.ride.RideHandlingServiceGrpc.*;
import it.ewlab.ride.RideHandlingServiceOuterClass.*;
import it.ewlab.ride.RideRequestMsgOuterClass.*;
import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.ewlab.district.TaxiAvailabilityMsgOuterClass.*;

import static java.lang.Math.sqrt;

public class Taxi {

    private static Taxi instance;

    private String id;
    private String ip = "127.0.0.1";
    private String port;

    // MQTT
    String clientId = MqttClient.generateClientId();
    String ridesTopic = "seta/smartcity/rides/district";
    String taxiAvailabilityTopic = "seta/smartcity/taxyAvailability";
    String broker = "tcp://localhost:1883";
    MqttClient client;
    int qos = 2;

    // Taxi Data
    private Position position;
    private Taxi(){}
    private int  battery = 100;
    private boolean busy;

    // Grpc
    private Map<String, StreamObserver<RideHandlingReply>> delayedRideResponse = new HashMap<String, StreamObserver<RideHandlingReply>>();
    private List<TaxiInfo> taxiContacts = new ArrayList<TaxiInfo>();

    public static Taxi getInstance(){
        if (instance == null)
            instance = new Taxi();
        return  instance;
    }

    public void init(){
        registerItself();
        RideHandler.startGrpcServices(Integer.valueOf(port));
        subscribeToRideRequests();
        publishAvailability();
    }

    public String getId() {
        return id;
    }

    public int getBattery() {
        return battery;
    }

    public void addDelayedResponse(String rideRequestId, StreamObserver<RideHandlingReply> obs){
        delayedRideResponse.put(rideRequestId, obs);
    }

    public void registerItself(){
        // Taxi send id, ip, port to server
        // it receive list of other taxiInfo and personal starting position generated randomly from server
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Insert id");
        try {
            id = br.readLine();
            System.out.println("Insert Port");
            port = br.readLine();
            //TODO check inputs
        } catch (IOException e) {e.printStackTrace();}
        Client client = Client.create();

        // POST
        String postPath = "/taxis/add";
        TaxiInfo myTaxiInfo = new TaxiInfo(id, ip, port);
        ClientResponse clientResponse = postRequest(client,Main.SERVERADDRESS+postPath,myTaxiInfo);
        // print results
        System.out.println(clientResponse.toString());
        TaxisRegistrationInfo regInfo = clientResponse.getEntity(TaxisRegistrationInfo.class);

        for (TaxiInfo info : regInfo.getTaxiInfoList()) {
            System.out.println("taxi " + info.getId() + ", address: " +info.getIp() + ":" + info.getPort());
        }
        position = new Position(regInfo.getMyStartingPosition().getX(), regInfo.getMyStartingPosition().getY());
        System.out.println("Taxi id: " + id +", My Starting position: "
                + regInfo.getMyStartingPosition().getX() + ", "
                + regInfo.getMyStartingPosition().getY());
    }

    private ClientResponse postRequest(Client client, String url, TaxiInfo t){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(t);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    public void subscribeToRideRequests(){
        if (client == null){
            try {
                client = new MqttClient(broker, clientId);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                // Connect the client
                System.out.println(clientId + " Connecting Broker " + broker);
                client.connect(connOpts);
                client.setCallback(new MqttCallback() {
                    public void messageArrived(String topic, MqttMessage message) {
                        // Called when a message arrives from the server that matches any subscription made by the client
                        System.out.println("Taxi n." + id + " Message arrived on topic " + topic);
                        if (ridesTopic.indexOf(topic) != -1){
                            List<TaxiInfo> currentTaxiContacts = new ArrayList<TaxiInfo>(taxiContacts);
                            RideAcquisition rideAcquisition = null;
                            RideRequest ride = null;
                            try {
                                ride = new RideRequest(RideRequestMsg.parseFrom(message.getPayload()));
                                rideAcquisition= new RideAcquisition(currentTaxiContacts.size(), ride);
                            } catch (InvalidProtocolBufferException e) {
                                e.printStackTrace();
                            }
                            List<Thread> requestThreads = new ArrayList<Thread>();
                            Double distanceFromRide = getDistance(ride.getStartingPosition());
                            RideAcquisition finalRideAcquisition = rideAcquisition;
                            RideRequest finalRide = ride;
                            for (TaxiInfo taxiContact : currentTaxiContacts){
                                String target = taxiContact.getIp() + taxiContact.getPort();
                                // a new thread is launched for each message to send
                                Thread requestThread = new Thread(() -> {
                                    final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

                                    //creating a blocking stub on the channel
                                    RideHandlingServiceBlockingStub stub = RideHandlingServiceGrpc.newBlockingStub(channel);

                                    //creating the HelloResponse object which will be provided as input to the RPC method
                                    RideHandlingRequest request = RideHandlingRequest.newBuilder()
                                            .setRideRequestMsg(finalRide.toMsg())
                                            .setDistance(distanceFromRide)
                                            .setBattery(battery)
                                            .setTaxiId(id).build();

                                    RideHandlingReply response = stub.startRideHandling(request);

                                    if (!response.getDiscard()){
                                        finalRideAcquisition.acked();
                                    }

                                    //closing the channel
                                    channel.shutdown();
                                });
                                requestThread.start();
                                requestThreads.add(requestThread);
                            }
                            for (Thread t : requestThreads){
                                try {
                                    t.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (finalRideAcquisition.getAckToReceive() == 0){
                                handleRide(finalRide);
                            }
                            // gestire la mutual exclusion
                            // serve la lista dei taxi in questo momento
                            // servizio che risponda OK
                            // un taxi può partecipare a due mutual exclusion? si, quando ne vince una rilascia l'altra
                            // se uno sta facendo la ride, risponde sempre ok, quindi per forza boolean
                        }
                    }
                    public void connectionLost(Throwable cause) {
                        System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
                    }
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // Not used here
                    }
                });

            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
            System.out.println(clientId + " Connected - taxy ID: " + id);
        }
        try {
            client.subscribe(ridesTopic + position.getDistrict(),qos);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        System.out.println("taxi n. " + id + " Subscribed to topics : " + ridesTopic + position.getDistrict());
    }

    private void unSubscribeToRideRequests(){
        try {
            client.unsubscribe(ridesTopic + position.getDistrict());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleRide(RideRequest ride){

        System.out.println("Taxi n. " + id + "taking charge of ride request n. " + ride.getId());

        // free al other rides
        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
        StreamObserver<RideHandlingReply> responseObserver = delayedRideResponse.get(ride.getId());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        delayedRideResponse.remove(ride.getId());

        response = RideHandlingReply.newBuilder().setDiscard(false).build();
        for (StreamObserver<RideHandlingReply> observer : delayedRideResponse.values()){
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        // make the ride
        busy = true;
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ride.getStartingPosition().getDistrict() != ride.getDestinationPosition().getDistrict()){
            unSubscribeToRideRequests();
            position = ride.getDestinationPosition();
            subscribeToRideRequests();
        }
        else{
            position = ride.getDestinationPosition();
        }
        busy = false;
        publishAvailability();

    }

    public void publishAvailability(){
        TaxiAvailabilityMsg payload = TaxiAvailabilityMsg.newBuilder().setDistrict(position.getDistrict()).build();
        MqttMessage message = new MqttMessage(payload.toByteArray());
        try {
            client.publish(taxiAvailabilityTopic, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public double getDistance(Position pos){
        return sqrt((pos.getY() - position.getY()) * (pos.getY() - position.getY()) + (pos.getX() - position.getX()) * (pos.getX() - position.getX()));
    }
}

// TODO presentazione a alrti taxi
// condizioni dentro a ridehandlingimpl
// ricarica
// statistiche
// client
