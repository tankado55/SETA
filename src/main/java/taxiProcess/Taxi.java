package taxiProcess;

import SETA.RideRequest;
import beans.Position;
import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.ewlab.ride.GreetingsServiceGrpc;
import it.ewlab.ride.GreetingsServiceGrpc.*;
import it.ewlab.ride.GreetingsServiceOuterClass.*;
import it.ewlab.ride.RechargeServicesGrpc;
import it.ewlab.ride.RechargeServicesOuterClass.*;
import it.ewlab.ride.RideHandlingServiceGrpc;
import it.ewlab.ride.RideHandlingServiceGrpc.*;
import it.ewlab.ride.RideHandlingServiceOuterClass.*;
import it.ewlab.ride.RideRequestMsgOuterClass.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.ewlab.district.TaxiAvailabilityMsgOuterClass.*;

import static java.lang.Math.sqrt;
import static java.lang.Thread.sleep;

public class Taxi {

    private static Taxi instance;

    private String id;
    private String ip = "127.0.0.1";
    private String port;

    // MQTT
    String clientId = MqttClient.generateClientId();
    String ridesTopic = "seta/smartcity/rides/district";
    String taxiAvailabilityTopic = "seta/smartcity/taxyAvailability";
    String exitResponseTopic;
    String broker = "tcp://localhost:1883";
    MqttClient client;
    int qos = 2;

    // Taxi Data
    private Position position;
    private Taxi(){}
    private Battery battery = new Battery();
    private Boolean busy = false;
    private Boolean authorizedExit = false;
    private Integer parallelElectionCount = 0;
    public  Boolean wantToCharge = false;
    public Object charging = new Object();

    // Grpc
    private List<DelayedResponse> delayedRideResponses = new ArrayList<>();
    private List<StreamObserver<RechargeResponse>> delayedRechargeResponses = new ArrayList<>();
    private List<TaxiInfo> taxiContacts = new ArrayList<TaxiInfo>();
    public Object busyLock = new Object();

    public static Taxi getInstance(){
        if (instance == null)
            instance = new Taxi();
        return  instance;
    }

    public void init(){
        registerItself();
        // Grpc
        GrpcServices grpc = GrpcServices.getInstance();
        grpc.setPort(Integer.valueOf(port));
        grpc.start();
        while (grpc.getServerState() != "Server Started"){try {sleep(1000);} catch (InterruptedException e) {throw new RuntimeException(e);}};
        setupMqtt();
        subscribeToRideRequests();
        publishAvailability();
        exitResponseTopic = "exitResponse" + id;
    }

    public String getId() {
        return id;
    }

    public double getBatteryLevel() {
        return battery.getLevel();
    }

    public Battery getBattery() {
        return battery;
    }

    public void addDelayedResponse(DelayedResponse dr){

        synchronized (busy){
            if (busy){
                RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                dr.getObserver().onNext(response);
                dr.getObserver().onCompleted();
            }
            else{
                synchronized (delayedRideResponses){
                    delayedRideResponses.add(dr);
                }
                System.out.println("Delayed Ride " + dr.getRideRequest().getId() + " my distance: " + getDistance(dr.getRideRequest().getStartingPosition()));
            }
        }
    }

    public void addDelayedRechargeResponse(StreamObserver<RechargeResponse> response){
        synchronized (delayedRechargeResponses){
            delayedRechargeResponses.add(response);
        }
    }

    public void registerItself(){
        // Taxi send id, ip, port to server
        // it receive list of other taxiInfo and personal starting position generated randomly from server
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Insert id");
        try {
            id = br.readLine();
            //System.out.println("Insert Port");
            port = "5050" + id;
            //TODO check inputs
        } catch (IOException e) {e.printStackTrace();}
        Client client = Client.create();

        // POST
        String postPath = "/taxis/add";
        TaxiInfo myTaxiInfo = new TaxiInfo(id, ip, port);
        ClientResponse clientResponse = postRequest(client,Main.SERVERADDRESS+postPath,myTaxiInfo);

        TaxisRegistrationInfo regInfo = clientResponse.getEntity(TaxisRegistrationInfo.class);

        for (TaxiInfo info : regInfo.getTaxiInfoList()) {
            System.out.println("taxi " + info.getId() + ", address: " +info.getIp() + ":" + info.getPort());
            taxiContacts.add(info);
        }
        position = new Position(regInfo.getMyStartingPosition().getX(), regInfo.getMyStartingPosition().getY());
        System.out.println("Taxi id: " + id +", My Starting position: "
                + regInfo.getMyStartingPosition().getX() + ", "
                + regInfo.getMyStartingPosition().getY());
        for (TaxiInfo contact : taxiContacts){
            String target = contact.getIp() + ":" + contact.getPort();
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

            //creating a blocking stub on the channel
            GreetingsServiceBlockingStub stub = GreetingsServiceGrpc.newBlockingStub(channel);

            //creating the HelloResponse object which will be provided as input to the RPC method
            GreetingsRequest.TaxiInfoMsg taxiInfoMsg= GreetingsRequest.TaxiInfoMsg.newBuilder().setId(id).setIp(ip).setPort(Integer.valueOf(port)).build();
            GreetingsRequest request = GreetingsRequest.newBuilder().setTaxiInfoMsg(taxiInfoMsg).build();

            GreetingsResponse response = stub.greeting(request);

            //closing the channel
            channel.shutdown();
        }
    }

    public void addTaxiInfo (TaxiInfo info){
        synchronized (taxiContacts){
            taxiContacts.add(info);
        }
    }
    public void removeTaxiInfo (TaxiInfo info){
        synchronized (taxiContacts){
            for (TaxiInfo contact : taxiContacts){
                if (contact.getId() == info.getId()){
                    taxiContacts.remove(contact);
                }
            }
        }
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

    private void setupMqtt(){
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
                        new Thread(() -> {

                            if (topic.equals(ridesTopic + position.getDistrict()) && !authorizedExit){

                                if (!busy){
                                    synchronized (parallelElectionCount){
                                        ++parallelElectionCount;
                                    }

                                    RideAcquisition rideAcquisition = null;
                                    RideRequest ride = null;
                                    List<TaxiInfo> currentContacts;
                                    synchronized (taxiContacts) {
                                        try {
                                            ride = new RideRequest(RideRequestMsg.parseFrom(message.getPayload()));
                                            rideAcquisition = new RideAcquisition(taxiContacts.size(), ride);
                                            System.out.println("Ride " + ride.getId() + " arrived on topic " + topic);
                                        } catch (InvalidProtocolBufferException e) {
                                            e.printStackTrace();
                                        }
                                        currentContacts = new ArrayList<>(taxiContacts);
                                    }
                                    List<Thread> requestThreads = new ArrayList<Thread>();
                                    RideRequest finalRide = ride;
                                    RideAcquisition finalRideAcquisition = rideAcquisition;
                                    Double distanceFromRide = getDistance(ride.getStartingPosition());
                                    for (TaxiInfo taxiContact : currentContacts){
                                        String target = taxiContact.getIp() + ":" + taxiContact.getPort();
                                        // a new thread is launched for each message to send
                                        Thread requestThread = new Thread(() -> {
                                            final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

                                            //creating a blocking stub on the channel
                                            RideHandlingServiceBlockingStub stub = RideHandlingServiceGrpc.newBlockingStub(channel);

                                            //creating the HelloResponse object which will be provided as input to the RPC method
                                            RideHandlingRequest request = RideHandlingRequest.newBuilder()
                                                    .setRideRequestMsg(finalRide.toMsg())
                                                    .setDistance(distanceFromRide)
                                                    .setBattery((int)battery.getLevel())
                                                    .setTaxiId(id).build();
                                            System.out.println("I want Ride " +  finalRide.getId() + " my distance: " + getDistance(finalRide.getStartingPosition()));
                                            RideHandlingReply response = stub.startRideHandling(request);

                                            if (!response.getDiscard()){
                                                synchronized (finalRideAcquisition){
                                                    finalRideAcquisition.acked();
                                                }
                                            }
                                            System.out.println("Received reply Ride" + finalRide.getId() + ", discard? " + response.getDiscard());

                                            //closing the channel
                                            channel.shutdown();
                                        });
                                        requestThread.start();
                                        requestThreads.add(requestThread);// save the threads to make the join
                                    }
                                    for (Thread t : requestThreads){
                                        try {
                                            t.join(); //necessaria per attendere le delayedReply
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (finalRideAcquisition.getAckToReceive() <= 0){
                                        synchronized (busyLock){
                                            synchronized (busy){
                                                busy = true;
                                            }
                                            handleRide(finalRide);
                                        }
                                    }
                                    else{
                                        System.out.println("\u001B[33m" + "Ride " + ride.getId() + " taken by another taxi" + "\u001B[0m");
                                    }
                                    synchronized (parallelElectionCount){
                                        --parallelElectionCount;
                                    }
                                    if (parallelElectionCount == 0 && authorizedExit && battery.toRecharge())
                                        startRechargeRequest();
                                }

                        }
                        else if (topic.equals("exitResponse" + id)){
                            authorizedExit = true;

                            if (parallelElectionCount == 0 && authorizedExit && battery.toRecharge()){
                                System.out.println("Received exit authorization from SETA");
                                startRechargeRequest();
                            }
                            else{
                                exit();
                            }
                        }
                        }).start();

                    }
                    public void connectionLost(Throwable cause) {
                        System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
                        cause.printStackTrace();
                    }
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // Not used here
                    }
                });

            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exit(){
        System.out.println("Bye Bye");
    }

    private void startRechargeRequest(){
        synchronized (busyLock){
            synchronized (busy){
                busy = true;
            }
            System.out.println("I want to charge in district " + position.getDistrict());
            battery.setCurrentTimestamp();

            wantToCharge = true;
            List<TaxiInfo> currentContacts;
            currentContacts = new ArrayList<>(taxiContacts);

            List<Thread> requestThreads = new ArrayList<Thread>();
            for (TaxiInfo taxiContact : currentContacts){
                String target = taxiContact.getIp() + ":" + taxiContact.getPort();
                // a new thread is launched for each message to send
                Thread requestThread = new Thread(() -> {
                    final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

                    //creating a blocking stub on the channel
                    RechargeServicesGrpc.RechargeServicesBlockingStub stub = RechargeServicesGrpc.newBlockingStub(channel);

                    //creating the HelloResponse object which will be provided as input to the RPC method
                    RechargeRequest request = RechargeRequest.newBuilder()
                                    .setDistrict(getPosition().getDistrict())
                                            .setTime(battery.getRequestTimestamp()).build();

                    RechargeResponse response = stub.askPremiseToCharge(request);

                    //closing the channel
                    channel.shutdown();
                });
                requestThread.start();
                requestThreads.add(requestThread);// save the threads to make the join
            }
            for (Thread t : requestThreads){
                try {
                    t.join(); //necessaria per attendere le delayedReply
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("I'm going to recharge station, I received permission from other taxis");
            Position rechargePosition = Utils.getRechargePosition(position.getDistrict());
            battery.discarge(getDistance(rechargePosition));
            position = rechargePosition;
            battery.makeRecharge();
            delayedRechargeResponses.clear();
        }
        authorizedExit = false;
        wantToCharge = false;
    }
    public void subscribeToRideRequests(){

        try {
            int district = position.getDistrict();
            client.subscribe(ridesTopic + district,qos);
            System.out.println("taxi n. " + id + " Subscribed to topics : " + ridesTopic + district);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }

    private void unSubscribeToRideRequests(){
        try {
            client.unsubscribe(ridesTopic + position.getDistrict());
            System.out.println("Unsubscribed from topic: " + ridesTopic + position.getDistrict());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleRide(RideRequest ride){
        //unSubscribeToRideRequests();

        System.out.println("Taxi n. " + id + " taking charge of ride request n. " + ride.getId());

        // free al other rides
        synchronized (delayedRideResponses){
            for (DelayedResponse delayedResponse : delayedRideResponses){
                RideHandlingReply response;
                if (delayedResponse.getRideRequest().getId().equals(ride.getId())) {
                    response = RideHandlingReply.newBuilder().setDiscard(true).build();
                }
                else {
                    response = RideHandlingReply.newBuilder().setDiscard(false).build();
                }
                delayedResponse.getObserver().onNext(response);
                delayedResponse.getObserver().onCompleted();
            }
            delayedRideResponses.clear();
        }
        // make the ride
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Taxi n." + id + ", ride " + ride.getId() + " Completed!");
        if (ride.getStartingPosition().getDistrict() != ride.getDestinationPosition().getDistrict()){
            unSubscribeToRideRequests();
            System.out.println("Moved to "+ position.getDistrict());
        }

        synchronized (authorizedExit){
            if (authorizedExit){
                System.out.println("The availability below is published for previous district");
                publishAvailability(ride.getStartingPosition().getDistrict()); // this is because the taxi obtained premise to leave in the starting district
            }
        }

        battery.discarge(getDistance(ride.getStartingPosition()));
        position = ride.getDestinationPosition();
        battery.discarge(getDistance(ride.getStartingPosition()));

        if (battery.toRecharge()){
            startRechargeRequest();
        }
        synchronized (busy){
            busy = false;
        }

        subscribeToRideRequests();
        publishAvailability();
        }

    public void publishAvailability(){
        TaxiAvailabilityMsg payload = TaxiAvailabilityMsg.newBuilder().setDistrict(position.getDistrict()).build();
        MqttMessage message = new MqttMessage(payload.toByteArray());
        try {
            client.publish(taxiAvailabilityTopic, message);
            System.out.println("to SETA, I'm available on district" + position.getDistrict());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void publishAvailability(int district){
        TaxiAvailabilityMsg payload = TaxiAvailabilityMsg.newBuilder().setDistrict(position.getDistrict()).build();
        MqttMessage message = new MqttMessage(payload.toByteArray());
        try {
            client.publish(taxiAvailabilityTopic, message);
            System.out.println("to SETA, I'm available on district" + district);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public double getDistance(Position pos){
        return sqrt((pos.getY() - position.getY()) * (pos.getY() - position.getY()) + (pos.getX() - position.getX()) * (pos.getX() - position.getX()));
    }

    public Position getPosition() {
        return position;
    }

    public void startExitRequest(){
        // taxi ask seta to leave, if no ride has been published SETA return ok, taxi should retry if it has not acquired ride
        try {client.subscribe("exitResponse" + id);
        JSONObject payload = new JSONObject();
        payload.put("id", id);
        payload.put("district", position.getDistrict());
        MqttMessage msg = new MqttMessage(payload.toString().getBytes());
        client.publish("exitRequest", msg);
        } catch (MqttException | JSONException e) {e.printStackTrace();}
    }
}

// TODO
// ricarica (Demolli)                   1G
// 1. pollution (Demolli/Piscitelli)    1G
// 2. statistiche REST                  2G
// client                               2G
// exit controllata

// se id è già presente consentire di reinserirlo