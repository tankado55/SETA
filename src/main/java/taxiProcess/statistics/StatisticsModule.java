package taxiProcess.statistics;

import beans.StatisticsData;
import beans.TaxiInfo;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import simulators.Measurement;
import simulators.PM10Buffer;
import simulators.PM10Simulator;
import taxiProcess.Main;
import taxiProcess.Taxi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//Every 15 seconds, each Taxi has to compute and communicate to the administrator server the following local statistics
public class StatisticsModule extends Thread{
    Taxi taxi;
    PM10Buffer pm10Buffer;
    RidesStatsBuffer ridesStatsBuffer;
    Client client;

    public StatisticsModule(Taxi t, Client client, PM10Buffer pm10Buffer, RidesStatsBuffer ridesStatsBuffer){
        taxi = t;
        this.pm10Buffer = pm10Buffer;
        this.ridesStatsBuffer = ridesStatsBuffer;
        this.client = client;
    }
    @Override
    public void run() {
        while (true){
            try {Thread.sleep(15000);} catch (InterruptedException e) {e.printStackTrace();}
            sendToServer(taxi.getId(), System.currentTimeMillis(), taxi.getBatteryLevel());
        }
    }

    private void sendToServer(String taxiId, long timestamp, Double batteryLevel){
        String postPath = "/statistics/add";
        List<Double> pollutionAverages = pm10Buffer.readAllAndClean().stream().map(Measurement::getValue).collect(Collectors.toList());
        RidesStats stats = ridesStatsBuffer.readAllAndClean();
        StatisticsData data = new StatisticsData(taxiId, timestamp, batteryLevel, stats.getKm(), stats.getRideCount(), new ArrayList<>(pollutionAverages));
        ClientResponse clientResponse = postRequest(client, Main.SERVERADDRESS+postPath,data);
        //System.out.println(clientResponse);
    }

    private ClientResponse postRequest(Client client, String url, StatisticsData d){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(d);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }
}
