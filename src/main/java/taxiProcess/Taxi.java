package taxiProcess;

import beans.BeansTest;
import beans.Position;
import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
    private Position position;
    private Taxi(){}

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
        System.out.println(clientId + " Subscribed to topics : " + ridesTopic + position.getDistrict());
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
