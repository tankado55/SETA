package SETA;

import com.google.protobuf.InvalidProtocolBufferException;
import it.ewlab.district.TaxiAvailabilityMsgOuterClass.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.paho.client.mqttv3.*;

import taxiProcess.Taxi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TaxiAvailabilitySubscriber {
    ArrayList<RideRequestQueue> rideRequestQueues;
    MqttClient client;

    TaxiAvailabilitySubscriber(ArrayList<RideRequestQueue> rideRequestQueues, MqttClient client){
        this.rideRequestQueues = rideRequestQueues;
        this.client = client;
    }

    public void subscribe() {
        String clientId = MqttClient.generateClientId();
        String topic = "seta/smartcity/taxyAvailability";

        try {
            // Callback
            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) {

                    if (topic.equals("seta/smartcity/taxyAvailability")){
                        TaxiAvailabilityMsg msg;
                        try {
                            msg = TaxiAvailabilityMsg.parseFrom(message.getPayload());
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                        rideRequestQueues.get(msg.getDistrict() - 1).addAvailableTaxi();
                        System.out.println("SETA:  Received a Message! - Taxi availability on discrict " + msg.getDistrict());
                    }
                    else if (topic.equals("exitRequest")){
                        new Thread(() -> {
                            System.out.println("Received Exiting Message!");
                            try {
                                JSONObject msg = new JSONObject(new String(message.getPayload()));
                                if (rideRequestQueues.get(msg.getInt("district") -1).removeAvailableTaxi()){
                                    JSONObject payload = new JSONObject();
                                    payload.put("ok", true);
                                    MqttMessage response = new MqttMessage(payload.toString().getBytes());
                                    client.publish("exitResponse" + msg.getString("id"), response);
                                }
                            } catch (JSONException e) {throw new RuntimeException(e);} catch (MqttPersistenceException e) {
                                throw new RuntimeException(e);
                            } catch (MqttException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();

                    }
                    else if (topic.equals("completedRides")){
                        try {
                            JSONObject msg = new JSONObject(new String(message.getPayload()));
                            RideChecker.getInstance().addRide(msg.getInt("rideId"));
                            rideRequestQueues.get(msg.getInt("district") - 1).addCompleted();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
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
            client.subscribe("exitRequest", SETA.qos);
            client.subscribe("completedRides", SETA.qos);
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
