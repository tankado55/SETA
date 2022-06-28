package adminServer;

import beans.StatisticsData;
import beans.TaxiInfo;
import beans.TaxiList;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/statistics")
public class StatisticsServices {

    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response add(StatisticsData data){
        StatisticsManager sm = StatisticsManager.getInstance();
        sm.add(data);
        System.out.println("Received Statistics");
        return Response.ok("Statistics Received!").build();
    }

    @Path("get/taxiList")
    @GET
    @Produces({"application/json", "application/xml"})
    public  Response getTaxiList(){
        TaxisInfoManager taxisInfoManager = TaxisInfoManager.getInstance();
        ArrayList<TaxiInfo> taxiInfos = taxisInfoManager.getTaxiInfoList();
        TaxiList taxiList= new TaxiList(taxiInfos);

        return Response.ok(taxiList).build();
    }
}
