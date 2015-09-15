package fr.broeglin.integration.iti.services;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/cmdb/v1")
public class CmdbService {

  @GET
  @Path("/{ciType}")
  @Produces("application/json")
  public Response cis(@PathParam("ciType") String id,
      @QueryParam("offset") @DefaultValue("0") int offset,
      @QueryParam("limit") @DefaultValue("100") int limit) {
    return null;
  }

  @GET
  @Path("/{ciType}/{id}")
  @Produces("application/json")
  public Response ci(@PathParam("ciType") String ciType, @PathParam("id") String id) {
    return null;
  }
}
