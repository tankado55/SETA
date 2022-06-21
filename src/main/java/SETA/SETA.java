package SETA;

import beans.Position;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import it.ewlab.ride.RideRequestMsgOuterClass.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SETA {

    // Settings
    static final int GRIDXDIM = 10;
    static final int GRIDYDIM = 10;
    static final int districtCount = 4;

    // MQTT
    static MqttClient client;
    static String broker = "tcp://localhost:1883";
    static MqttConnectOptions connOpts;
    static String clientId = MqttClient.generateClientId();
    static String districtTopic = "seta/smartcity/rides/district";
    public static int qos = 2;

    // Data structures
    static ArrayList<RideRequestQueue> rideRequestQueues = new ArrayList<RideRequestQueue>();

    public static void main(String[] args) throws InterruptedException {

        initMQTTComponents();

        // Initialize queues and publishers
        // We have n publisher, one for each queue, because each one can sleep on a queue
        for (int i = 0; i < districtCount; ++i){
            RideRequestQueue rideRequestQueue = new RideRequestQueue();
            rideRequestQueues.add(rideRequestQueue);
            RideRequestPublisher rideRequestPublisher= new RideRequestPublisher(rideRequestQueue, client, districtTopic + (i + 1));
            new Thread(rideRequestPublisher).start();
        }
        // Inizialize subcriber that work with queues
        new TaxiAvailabilitySubscriber(rideRequestQueues, client).subscribe();

        // generating and publish two random rides every 5 seconds
        int idCounter = 0;
        while (true){
            RideRequest ride1 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;
            RideRequest ride2 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;

            rideRequestQueues.get(ride1.getStartingPosition().getDistrict() - 1).put(ride1);
            rideRequestQueues.get(ride2.getStartingPosition().getDistrict() - 1).put(ride2);

            for (int i = 0; i < rideRequestQueues.size(); ++i){
                System.out.println("District " + (i+1) + ", queue: " + rideRequestQueues.get(i).getSize());
            }

            Thread.sleep(5000);
        }

    }

    private static void initMQTTComponents(){

        connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        try {
            // Connect the client
            client = new MqttClient(broker, clientId);
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected");

            //if (client.isConnected())
            //    client.disconnect();
            //System.out.println("Publisher " + clientId + " disconnected");

        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private static RideRequest generateRandomRide(String id){

        while (true){
            Position start = Position.generateRandomPosition(GRIDXDIM, GRIDYDIM);
            Position dest = Position.generateRandomPosition(GRIDXDIM, GRIDYDIM);

            if (!start.equals(dest))
            {
                return new RideRequest(id, start, dest);
            } else {
                System.out.println("start e dest are equals, I'll try again");
            }
        }

    }
}
