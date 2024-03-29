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
import java.util.Scanner;

public class Main {


    public final static String SERVERADDRESS = "http://localhost:1337";

    public static void main(String[] args) throws IOException {
        Taxi taxi = Taxi.getInstance();
        taxi.init();


        // TODO bisognerebbe sottoscriversi a un topic dove SETA dichiara di essersi avviato, in questo caso ripubblico le availaability

        Scanner command = new Scanner(System.in);
        while (true){
            String input = command.nextLine();
            if (input.equals("recharge")){
                taxi.getBattery().setTriggerForRechargeAfterRideCompleted();
                taxi.startExitRequest();
            }
            else if(input.equals("quit")){
                taxi.setExitTrigger();
                taxi.startExitRequest();
            }
            else{
                System.out.println("Insert a valid input");
            }
        }

        //client.disconnect();


    }




}
