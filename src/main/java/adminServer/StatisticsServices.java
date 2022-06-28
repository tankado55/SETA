package adminServer;

import beans.*;

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
    public Response getTaxiList(){
        TaxisInfoManager taxisInfoManager = TaxisInfoManager.getInstance();
        ArrayList<TaxiInfo> taxiInfos = taxisInfoManager.getTaxiInfoList();
        TaxiList taxiList= new TaxiList(taxiInfos);

        return Response.ok(taxiList).build();
    }

    @Path("get/nAverages/{taxiId}/{n}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getNAverages(@PathParam("n") int n, @PathParam("taxiId") String taxiId){
        StatisticsManager statisticsManager = StatisticsManager.getInstance();
        StatisticsAverages averages = statisticsManager.getAverages(taxiId, n);

        return Response.ok(averages).build();
    }

    @Path("get/{t1}/{t2}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getStatisticsInterval(@PathParam("t1") long t1, @PathParam("t2") long t2){
        StatisticsManager statisticsManager = StatisticsManager.getInstance();
        StatisticsAverages averages = statisticsManager.getStatisticsInterval(t1, t2);

        return Response.ok(averages).build();
    }

    @Path("get/firstTimestamp")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getFirstTimestamp(){
        StatisticsManager statisticsManager = StatisticsManager.getInstance();
        Long minTimestamp = statisticsManager.getFirstStatTimestamp();

        return Response.ok(new LongBeanWrapper(minTimestamp)).build();
    }

    @Path("get/lastTimestamp")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getLastTimestamp(){
        StatisticsManager statisticsManager = StatisticsManager.getInstance();
        Long maxTimestamp = statisticsManager.getLastStatTimestamp();

        return Response.ok(new LongBeanWrapper(maxTimestamp)).build();
    }


}
