package taxiProcess;

import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {


    public final static String SERVERADDRESS = "http://localhost:1337";

    public static void main(String[] args) throws IOException {
        Taxi taxi = Taxi.getInstance();
        taxi.registerItself();
        taxi.subscribeToRideRequests();
        taxi.publishAvailability();

        System.out.println("\n ***  Press a random key to exit *** \n");
        Scanner command = new Scanner(System.in);
        command.nextLine();
        client.disconnect();


    }




}
