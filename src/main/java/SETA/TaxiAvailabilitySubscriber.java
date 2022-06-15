package SETA;

import com.google.protobuf.InvalidProtocolBufferException;
import it.ewlab.district.TaxiAvailabilityMsgOuterClass.*;
import org.eclipse.paho.client.mqttv3.*;

import taxiProcess.Taxi;

import java.util.ArrayList;

public class TaxiAvailabilitySubscriber implements Runnable {
    ArrayList<RideRequestQueue> rideRequestQueues;
    MqttClient client;

    TaxiAvailabilitySubscriber(ArrayList<RideRequestQueue> rideRequestQueues, MqttClient client){
        this.rideRequestQueues = rideRequestQueues;
        this.client = client;
    }

    @Override
    public void run() {
        // TODO non serve farlo multithread
        String clientId = MqttClient.generateClientId();
        String topic = "seta/smartcity/taxyAvailability";

        try {
            // Callback
            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) {
                    // Called when a message arrives from the server that matches any subscription made by the client
                    TaxiAvailabilityMsg msg;
                    try {
                        msg = TaxiAvailabilityMsg.parseFrom(message.getPayload());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    rideRequestQueues.get(msg.getDistrict() - 1).addAvailableTaxi();
                    System.out.println("SETA:  Received a Message! - Taxi availability on discrict " + msg.getDistrict());
                }

                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used here
                }

            });
            System.out.println(clientId + "SETA -  Subscribing to taxiAvailability ...");
            client.subscribe(topic,SETA.qos);
            System.out.println(clientId + " Subscribed to topics : " + topic);

            /*
            System.out.println("\n ***  Press a random key to exit *** \n");
            Scanner command = new Scanner(System.in);
            command.nextLine();
            client.disconnect();
            */

        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }

    }
}
