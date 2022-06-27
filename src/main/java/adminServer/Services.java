package adminServer;

import beans.BeansTest;
import beans.TaxiInfo;
import beans.TaxisRegistrationInfo;
import exceptions.taxi.IdAlreadyPresentException;
import exceptions.taxi.TaxiNotPresentException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/taxis")
public class Services {

    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addTaxi(TaxiInfo t){
        // try to add, return position and list of taxis
        // if it fail return a message
        try {
            TaxisInfoManager taxisInfoManager = TaxisInfoManager.getInstance();
            List<TaxiInfo> otherTaxiList = taxisInfoManager.add(t);
            TaxisRegistrationInfo response = new TaxisRegistrationInfo(AdministratorServer.generateStartingPoint()
                                                                           , otherTaxiList);

            return Response.ok(response).build();

        } catch (IdAlreadyPresentException e) {
            e.printStackTrace();
            return Response.status(Response.Status.CONFLICT).entity("taxi ID already present").build();
        }
    }

    @Path("delete")
    @DELETE
    @Consumes({"application/json", "application/xml"})
    public Response deleteTaxi(TaxiInfo t){
        TaxisInfoManager taxisInfoManager = TaxisInfoManager.getInstance();

        try {
            taxisInfoManager.delete(t.getId());
            return Response.ok("taxi deleted").build();
        } catch (TaxiNotPresentException e) {
            e.printStackTrace();
            return Response.status(Response.Status.CONFLICT).entity("no taxi to delete").build();
        }
    }
}
