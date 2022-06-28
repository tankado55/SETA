package adminClient;

import beans.StatisticsAverages;
import beans.StatisticsData;
import beans.TaxiInfo;
import beans.TaxiList;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import simulators.Buffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //init
        Client client = Client.create();
        String serverAddress = "http://localhost:1337";

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true){
            System.out.println("Commands:");
            System.out.println("1 - Print the list of taxis currently in the network");
            System.out.println("2 - Print the average of n stats of a taxi");
            System.out.println("3 - Print the average of stats between T1 and t2 of all taxis");

            String input = null;
            int inputInt;
            try {
                input = bufferedReader.readLine();
                if (input.equals(""))
                    throw new IOException();
                inputInt = Integer.parseInt(input);
                if (inputInt < 1 || inputInt > 3)
                    throw  new IOException();

                if (inputInt == 1){
                    String getPath = "/statistics/get/taxiList";
                    ClientResponse clientResponse = getRequest(client, serverAddress+getPath);
                    TaxiList taxiList = clientResponse.getEntity(TaxiList.class);
                    System.out.println("Taxi List: ");
                    for (TaxiInfo t : taxiList.getTaxiInfoList()){
                        System.out.println("TaxiId: " + t.getId() + " ip: " + t.getIp() + " port: " + t.getPort());
                    }
                }
                else if (inputInt == 2){
                    System.out.println("Insert a taxi Id");
                    String taxiId = bufferedReader.readLine();
                    if (taxiId.equals(""))
                        throw new IOException();
                    System.out.println("How many stats you want? insert a number");
                    int n = command.nextInt();
                    String getPath = "get/nAverages/" + n + "/" + taxiId;
                    ClientResponse clientResponse = getRequest(client, serverAddress+getPath);
                    StatisticsAverages averages = clientResponse.getEntity(StatisticsAverages.class);
                    System.out.println("Taxi Id: " + averages.getTaxiId()
                            + ", average ride count: " + averages.getRideCountAverage()
                            + ", average km driven: " + averages.getKmAverage()
                            + ", average battery level: " + averages.getBatteryAverage()
                            + ", average pollution level: " + averages.getPollutionAverage());
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Please insert a valid input!");
            }

        }
    }

    public static ClientResponse getRequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }
}
