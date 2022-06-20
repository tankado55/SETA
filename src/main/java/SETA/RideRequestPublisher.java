package SETA;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class RideRequestPublisher implements Runnable {
    RideRequestQueue ridesQueue;
    MqttClient client;
    String topic;

    RideRequestPublisher(RideRequestQueue ridesQueue, MqttClient client, String topic){
        this.ridesQueue = ridesQueue;
        this.client = client;
        this.topic = topic;
    }

    @Override
    public void run() {
        while (true){
            RideRequest ride = ridesQueue.take();
            MqttMessage message = new MqttMessage(ride.toMsg().toByteArray());
            message.setQos(SETA.qos);
            try {
                client.publish(topic, message);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
            System.out.println(" RideRequest " + ride.getId() + " published, topic: " + topic);
        }
    }
}
