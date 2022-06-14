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

    static final int GRIDXDIM = 10;
    static final int GRIDYDIM = 10;

    //MQTT
    static MqttClient client;
    static String broker = "tcp://localhost:1883";
    static MqttConnectOptions connOpts;
    static String clientId = MqttClient.generateClientId();
    static String district1Topic = "seta/smartcity/rides/district1";
    static String district2Topic = "seta/smartcity/rides/district2";
    static String district3Topic = "seta/smartcity/rides/district3";
    static String district4Topic = "seta/smartcity/rides/district4";
    static int qos = 2;

    public static void main(String[] args) throws InterruptedException {

        initMQTTComponents(); // MqttConnectOptions

        // generating and publish two random rides every 5 seconds
        int idCounter = 0;

        while (true){
            RideRequest ride1 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;
            RideRequest ride2 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;

            publishRide(ride1.toMsg());
            publishRide(ride2.toMsg());

            Thread.sleep(5000);
        }

    }

    private static void publishRide(RideRequestMsg r){
        try {
            // Connect the client
            client = new MqttClient(broker, clientId);
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected");

            String payload = String.valueOf(0 + (Math.random() * 10)); // TODO le ride nel payload (protobuffer o json)
            MqttMessage message = new MqttMessage(payload.getBytes());

            // Set the QoS on the Message
            message.setQos(qos);
            System.out.println(clientId + " Publishing message: " + payload + " ...");
            client.publish(topic, message);
            System.out.println(clientId + " Message published");

            if (client.isConnected())
                client.disconnect();
            System.out.println("Publisher " + clientId + " disconnected");


        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private static void initMQTTComponents(){
        connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
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
