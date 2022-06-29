package adminClient;

import beans.*;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import simulators.Buffer;
import taxiProcess.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {
    //utils
    private static final DecimalFormat df = new DecimalFormat("0.00");
    public static void main(String[] args) {
        //init
        Client client = Client.create();
        String serverAddress = "http://localhost:1337";

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true){
            System.out.println("\n");
            System.out.println("Commands:");
            System.out.println("1 - Print the list of taxis currently in the network");
            System.out.println("2 - Print the average of n stats of a taxi");
            System.out.println("3 - Print the average of stats between a time1 and a time2 of all taxis");

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
                    String n = bufferedReader.readLine();
                    if (n.equals(""))
                        throw new IOException();
                    int nInt = Integer.parseInt(n);
                    String getPath = "/statistics/get/nAverages/" + taxiId + "/" + n;
                    ClientResponse clientResponse = getRequest(client, serverAddress+getPath);
                    StatisticsAverages averages = clientResponse.getEntity(StatisticsAverages.class);
                    System.out.println("Taxi Id: " + averages.getTaxiId()
                            + ", average ride count: " + df.format(averages.getRideCountAverage())
                            + ", average km driven: " + df.format(averages.getKmAverage())
                            + ", average battery level: " + df.format(averages.getBatteryAverage())
                            + ", average pollution level: " + df.format(averages.getPollutionAverage()));
                }
                else if (inputInt == 3){
                    // min timeStamp
                    ClientResponse clientResponse = getRequest(client, serverAddress+"/statistics/get/firstTimestamp");
                    LongBeanWrapper minTimestamp = clientResponse.getEntity(LongBeanWrapper.class);
                    String formattedMinTimestamp = Utils.toHumanDate(minTimestamp.getWrapped());
                    System.out.println("The first stat was picked at " + formattedMinTimestamp);
                    // max timeStamp
                    clientResponse = getRequest(client, serverAddress+"/statistics/get/lastTimestamp");
                    LongBeanWrapper maxTimestamp = clientResponse.getEntity(LongBeanWrapper.class);
                    String formattedMAxTimestamp = Utils.toHumanDate(maxTimestamp.getWrapped());
                    System.out.println("The last stat was picked at " + formattedMAxTimestamp);
                    // user inputs
                    System.out.println("Insert t1 in the same format");
                    String t1 = bufferedReader.readLine();
                    System.out.println("Insert t2 in the same format");
                    String t2 = bufferedReader.readLine();
                    // get request
                    String getPath = "/statistics/get/" + Utils.toMachineDate(t1) + "/" + Utils.toMachineDate(t2);
                    clientResponse = getRequest(client, serverAddress+getPath);
                    StatisticsAverages averages = clientResponse.getEntity(StatisticsAverages.class);
                    System.out.println("Average ride count: " + df.format(averages.getRideCountAverage())
                            + ", average km driven: " + df.format(averages.getKmAverage())
                            + ", average battery level: " + df.format(averages.getBatteryAverage())
                            + ", average pollution level: " + df.format(averages.getPollutionAverage()));
                }

            } catch (Throwable t) {
                //t.printStackTrace();
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
