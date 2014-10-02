package au.edu.unimelb.plantcell.services.impl;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Stateless
@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
public class Test {

	@GET
	@Path("hi")
	public String getTest() {
		return "Hi!";
	}
}
