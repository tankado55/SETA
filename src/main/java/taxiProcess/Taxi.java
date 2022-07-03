package taxiProcess;

import SETA.RideRequest;
import beans.Position;
import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import exceptions.taxi.IdAlreadyPresentException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.ewlab.recharge.RechargeServicesGrpc;
import it.ewlab.recharge.RechargeServicesOuterClass;
import it.ewlab.ride.GreetingsServiceGrpc;
import it.ewlab.ride.GreetingsServiceGrpc.*;
import it.ewlab.ride.GreetingsServiceOuterClass.*;
import it.ewlab.ride.RideHandlingServiceOuterClass.*;
import it.ewlab.ride.RideRequestMsgOuterClass.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import it.ewlab.district.TaxiAvailabilityMsgOuterClass.*;
import simulators.PM10Buffer;
import simulators.PM10Simulator;
import taxiProcess.election.ElectionMaker;
import taxiProcess.election.ElectionQueue;
import taxiProcess.statistics.RidesStatsBuffer;
import taxiProcess.statistics.StatisticsModule;

import static java.lang.Math.sqrt;
import static java.lang.Thread.sleep;

public class Taxi {

    private static Taxi instance;

    private String id;
    private final String ip = "127.0.0.1";
    private String port;

    // MQTT
    String clientId = MqttClient.generateClientId();
    String ridesTopic = "seta/smartcity/rides/district";
    String taxiAvailabilityTopic = "seta/smartcity/taxyAvailability";
    String exitResponseTopic;
    String broker = "tcp://localhost:1883";
    MqttClient client;
    int qos = 2;

    // REST
    Client restClient;

    // Taxi Data
    private Position position;
    private Taxi(){}
    private final Battery battery = new Battery();
    public Boolean busy = false;
    public Boolean authorizedExit = false;
    public  Boolean wantToCharge = false;
    private PM10Buffer pm10Buffer;
    private final RidesStatsBuffer rideStatsBuffer = new RidesStatsBuffer();
    public String currentRide = new String();
    private Boolean toExit = false;
    public Boolean electionLock = false;
    private String currentRideId = new String();
    public ElectionQueue electionQueue = new ElectionQueue();

    // Grpc
    public List<DelayedResponse> delayedRideResponses = new ArrayList<>();
    private final List<StreamObserver<RechargeServicesOuterClass.RechargeResponse>> delayedRechargeResponses = new ArrayList<>();
    public List<TaxiInfo> taxiContacts = new ArrayList<>();
    private ElectionMaker electionMaker;

    public static Taxi getInstance(){
        if (instance == null)
            instance = new Taxi();
        return  instance;
    }

    public synchronized void setCurrentRideId(String currentRideId) {
        this.currentRideId = currentRideId;
    }

    public String getCurrentRideId() {
        return currentRideId;
    }

    public synchronized void setElectionLock(Boolean electionLock) {
        this.electionLock = electionLock;
    }

    public void init(){
        while (true){
            try{
                registerItself();
                break;
            } catch (Throwable t){System.out.println("taxi registration failed, try another id");}
        }
        // Grpc
        GrpcServices grpc = GrpcServices.getInstance();
        grpc.setPort(Integer.valueOf(port));
        grpc.start();
        while (grpc.getServerState() != "Server Started"){try {sleep(1000);} catch (InterruptedException e) {throw new RuntimeException(e);}}
        pm10Buffer = new PM10Buffer();
        new PM10Simulator(pm10Buffer).start();
        new StatisticsModule(this, restClient, pm10Buffer, rideStatsBuffer).start();
        setupMqtt();
        subscribeToRideRequests();
        publishAvailability();
        electionMaker = new ElectionMaker(this, electionQueue);
        electionMaker.start();
        exitResponseTopic = "exitResponse" + id;
        try {
            client.subscribe("SetaOnline",qos);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
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
/*
    public void addDelayedResponse(DelayedResponse dr){

        synchronized (busy){
            if (busy){
                synchronized (currentRide){
                    if (currentRide.equals(dr.getRideRequest().getId())){
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(true).build();
                        dr.getObserver().onNext(response);
                        dr.getObserver().onCompleted();
                    }
                    else{
                        RideHandlingReply response = RideHandlingReply.newBuilder().setDiscard(false).build();
                        dr.getObserver().onNext(response);
                        dr.getObserver().onCompleted();
                    }
                }
            }
            else{
                synchronized (delayedRideResponses){
                    delayedRideResponses.add(dr);
                }
                System.out.println("Delayed Ride" + dr.getRideRequest().getId() + " my distance " + getDistance(dr.getRideRequest().getStartingPosition()));
            }
        }
    }
*/
    public void addDelayedRechargeResponse(StreamObserver<RechargeServicesOuterClass.RechargeResponse> responseObserver){
        synchronized (delayedRechargeResponses){
            if (battery.isCharging){
                System.out.println("I delayed another taxi recharge");
                delayedRechargeResponses.add(responseObserver);
            }
            else{
                RechargeServicesOuterClass.RechargeResponse response = RechargeServicesOuterClass.RechargeResponse.newBuilder().setOk(true).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }

    public void registerItself() throws IdAlreadyPresentException {
        // Taxi send id, ip, port to server
        // it receive list of other taxiInfo and personal starting position generated randomly from server
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Insert id");
        try {
            id = br.readLine();
            //System.out.println("Insert Port");
            Integer.parseInt(id);
            int portInt = 50500 + Integer.parseInt(id);
            port = String.valueOf(portInt);
        } catch (IOException e) {System.out.println("Insert a valid input");}
        restClient = Client.create();

        // POST
        String postPath = "/taxis/add";
        TaxiInfo myTaxiInfo = new TaxiInfo(id, ip, port);
        ClientResponse clientResponse = postRequest(restClient,Main.SERVERADDRESS+postPath,myTaxiInfo);

        if (clientResponse.getStatus() == ClientResponse.Status.CONFLICT.getStatusCode()){
            throw new IdAlreadyPresentException();
        }
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
            TaxiInfo toRemove = null;
            for (TaxiInfo contact : taxiContacts){
                if (contact.getId().equals(info.getId())){
                    toRemove = contact;
                }
            }
            if (toRemove != null)
                taxiContacts.remove(toRemove);
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
    private ClientResponse deleteRequest(Client client, String url, TaxiInfo t){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(t);
        try {
            return webResource.type("application/json").delete(ClientResponse.class, input);
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

                        if (topic.equals(ridesTopic + position.getDistrict()) && !authorizedExit && !busy){

                            System.out.println("received mqtt message on topic:" + topic);
                            startElection(topic, message); // it put ride in the queue

                        }
                        else if (topic.equals("exitResponse" + id)){
                            authorizedExit = true;
                            // If a taxi receive an authorized exit the busy status will never become true after the if
                            if (battery.toRecharge() && !busy && !electionLock){
                                System.out.println("Received exit authorization from SETA");
                                startRechargeRequest();
                            }
                            else if(toExit && !busy && !electionLock){
                                exit();
                            }
                        }
                        else if (topic.equals("SetaOnline")){
                            publishAvailability();
                        }
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

    private void startElection(String topic, MqttMessage message){

        RideRequest ride = null;
        try {ride = new RideRequest(RideRequestMsg.parseFrom(message.getPayload()));} catch (InvalidProtocolBufferException e) {throw new RuntimeException(e);}

        electionQueue.put(ride);
    }

    public void checkExitStatus(){
        if (authorizedExit && toExit && !busy){
            exit();
            return;
        }
        if (authorizedExit && battery.toRecharge() && !busy){
            startRechargeRequest();
            return;
        }
    }

    private void exit(){
        System.out.println("Bye Bye");
        busy = true;
        //remove taxiInfo from server
        // POST
        String postPath = "/taxis/delete";
        TaxiInfo myTaxiInfo = new TaxiInfo(id, ip, port);
        ClientResponse clientResponse = deleteRequest(restClient,Main.SERVERADDRESS+postPath,myTaxiInfo);
        System.out.println("deleted from server");
        System.out.println(clientResponse.getStatusInfo());

        //send a message to all taxi
        List<TaxiInfo> currentContacts;
        synchronized (taxiContacts) {
            currentContacts = new ArrayList<>(taxiContacts);
        }
        List<Thread> requestThreads = new ArrayList<>();
        for (TaxiInfo taxiContact : currentContacts) {
            String target = taxiContact.getIp() + ":" + taxiContact.getPort();
            Thread requestThread = new Thread(() -> {
                final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

                //creating a blocking stub on the channel
                GreetingsServiceBlockingStub stub = GreetingsServiceGrpc.newBlockingStub(channel);

                //creating the HelloResponse object which will be provided as input to the RPC method
                GreetingsRequest.TaxiInfoMsg taxiInfoMsg= GreetingsRequest.TaxiInfoMsg.newBuilder().setId(id).setIp(ip).setPort(Integer.valueOf(port)).build();
                GreetingsRequest request = GreetingsRequest.newBuilder().setTaxiInfoMsg(taxiInfoMsg).build();

                GreetingsResponse response = stub.bye(request);

                //closing the channel
                channel.shutdown();
            });
            requestThread.start();
            requestThreads.add(requestThread);// save the threads to make the join
            for (Thread t : requestThreads){
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("All taxi known, I quit!");
            System.exit(0);
        }
    }



    private void startRechargeRequest(){
        synchronized (busy){
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
                    RechargeServicesOuterClass.RechargeRequest request = RechargeServicesOuterClass.RechargeRequest.newBuilder()
                                    .setDistrict(getPosition().getDistrict())
                                            .setTime(battery.getRequestTimestamp()).build();

                    try{
                        RechargeServicesOuterClass.RechargeResponse response = stub.askPremiseToCharge(request);
                    }catch (Throwable t){System.out.println("taxi unavailable, you can charge by default");}


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

            System.out.println("\u001B[36m" + "I'm going to recharge station, I received permission from other taxis" + "\u001B[0m");
            Position rechargePosition = Utils.getRechargePosition(position.getDistrict());
            battery.discharge(getDistance(rechargePosition));
            System.out.println("Battery Level: " + battery.getLevel());
            synchronized (position){
                position = rechargePosition;
            }
            battery.makeRecharge();
            synchronized (delayedRechargeResponses){
                RechargeServicesOuterClass.RechargeResponse response = RechargeServicesOuterClass.RechargeResponse.newBuilder().setOk(true).build();
                for (StreamObserver<RechargeServicesOuterClass.RechargeResponse> responseObserver : delayedRechargeResponses){
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            }
            delayedRechargeResponses.clear();
        }
        if(toExit){
            exit();
        }
        if (authorizedExit)
            publishAvailability();
        authorizedExit = false;
        wantToCharge = false;
        battery.removeTriggerForRechargeAfterRideCompleted();
    }
    public void subscribeToRideRequests(){

        try {
            int district = position.getDistrict();
            client.subscribe(ridesTopic + district,qos);
            System.out.println("Subscribed to topics : " + ridesTopic + district);
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
/*
    public void clearRide(String rideId){
        synchronized (delayedRideResponses){
            DelayedResponse toRemove = null;
            for (DelayedResponse delayedResponse : delayedRideResponses){
                RideHandlingReply response;
                if (delayedResponse.getRideRequest().getId().equals(rideId)) {
                    response = RideHandlingReply.newBuilder().setDiscard(true).build();
                    delayedResponse.getObserver().onNext(response);
                    delayedResponse.getObserver().onCompleted();
                    toRemove = delayedResponse;
                }
            }
            if (toRemove != null)
                delayedRideResponses.remove(toRemove);
        }
    }
*/
    public void handleRide(RideRequest ride){
        //unSubscribeToRideRequests();
        publishRideCompleted(ride.getId(), ride.getStartingPosition().getDistrict());

        synchronized (busy){
            busy = true;
        }

        System.out.println("Taxi n. " + id + " taking charge of ride" + ride.getId());

        // free al other rides
        /*
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
        */
        // make the ride
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Taxi n." + id + ", ride" + ride.getId() + " Completed!");
        // discharge and change of position
        battery.discharge(getDistance(ride.getStartingPosition()));
        if (ride.getStartingPosition().getDistrict() != ride.getDestinationPosition().getDistrict()){
            unSubscribeToRideRequests();
            synchronized (position){
                position = ride.getDestinationPosition();
            }
            System.out.println("Moved to "+ position.getDistrict());
            synchronized (authorizedExit){
                if (authorizedExit){
                    System.out.println("The availability below is published for previous district due to a change of district during recharge authorization process!");
                    publishAvailability(ride.getStartingPosition().getDistrict()); // this is because the taxi obtained premise to leave in the starting district
                }
            }
        }else {
            synchronized (position){
                position = ride.getDestinationPosition();
            }

        }
        battery.discharge(getDistance(ride.getStartingPosition()));
        System.out.println("Battery Level: " + battery.getLevel());
        //stats
        rideStatsBuffer.add(getDistance(ride.getStartingPosition()));



        if (battery.toRecharge() || battery.getLevel() < 30.0){
            startRechargeRequest();
        }
        if (toExit){
            exit();
            return;
        }



        /* messo dentro publishavailability
        synchronized (busy){
            busy = false;
        }*/

        electionQueue.clear();
        subscribeToRideRequests();
        publishAvailability();
    }

    public void publishAvailability(){
        synchronized (busy){
            TaxiAvailabilityMsg payload = TaxiAvailabilityMsg.newBuilder().setDistrict(position.getDistrict()).build();
            MqttMessage message = new MqttMessage(payload.toByteArray());
            try {
                client.publish(taxiAvailabilityTopic, message);
                System.out.println("to SETA, I'm available on district" + position.getDistrict());
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
            busy = false;
        }

    }
    public void publishRideCompleted(String rideId, int district){
        try {
            JSONObject payload = new JSONObject();
            payload.put("rideId", rideId);
            payload.put("district", district);
            MqttMessage msg = new MqttMessage(payload.toString().getBytes());
            client.publish("completedRides", msg);
        } catch (MqttException | JSONException e) {e.printStackTrace();}
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

    public void setExitTrigger(){
        synchronized (toExit){
            toExit = true;
        }
    }
}
