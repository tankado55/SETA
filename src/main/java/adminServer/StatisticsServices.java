package adminServer;

import beans.StatisticsData;
import beans.TaxiInfo;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
}
