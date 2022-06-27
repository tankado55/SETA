package adminServer;

import beans.StatisticsData;
import beans.TaxiInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
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
    public  Response getTaxiList(){
        TaxisInfoManager taxisInfoManager = TaxisInfoManager.getInstance();
        List<TaxiInfo> taxiInfoList = taxisInfoManager.getTaxiInfoList();
    }
}
