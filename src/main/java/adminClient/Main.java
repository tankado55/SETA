package adminClient;

import beans.StatisticsData;
import beans.TaxiInfo;
import beans.TaxiList;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //init
        Client client = Client.create();
        String serverAddress = "http://localhost:1337";

        System.out.println("Commands:");
        System.out.println("1 - taxiList");
        System.out.println("2 - Average of n stat of a taxi");
        System.out.println("3 - Average of stats beetwen T1 and t2 of all taxi");
        Scanner command = new Scanner(System.in);
        while (true){
            String input = command.nextLine();
            if (input.equals("1")){
                String getPath = "/statistics/get/taxiList";
                ClientResponse clientResponse = getRequest(client, serverAddress+getPath);
                TaxiList taxiList = clientResponse.getEntity(TaxiList.class);
                System.out.println("Users List");
                for (TaxiInfo t : taxiList.getTaxiInfoList()){
                    System.out.println("TaxiId: " + t.getId() + " ip: " + t.getIp() + " port: " + t.getPort());
                }
            }
            else if (input.equals(2)){
                System.out.println("Insert a taxi Id, please");
                String taxiId = command.nextLine();
                System.out.println("How many stat you want? insert a number, please");
                int n = command.nextInt();
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
