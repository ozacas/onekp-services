package au.edu.unimelb.plantcell.services.impl;

import java.io.IOException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.ejb.Stateless;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Returns key files to the end user as published in the literature
 * 
 * @author acassin
 *
 */
@Path("/maab")
@Stateless
@Produces(MediaType.TEXT_PLAIN)
public class MAAB {
	@Context 
	private ServletContext servletContext;
	
	@GET
	@Path("scripts/{id}")
	public Response getScript(@PathParam("id") final String path) {
		try {
			DataHandler dh = getInternalDataFile(path);
			return Response.ok(dh).build();
		} catch (Exception e) {
			return Response.serverError().status(500).entity(e.getMessage()).build();
		}
		
	}
	
	private DataHandler getInternalDataFile(final String path) throws IOException {
		if (path == null || path.length() > 1000) {
			throw new IOException("Bogus file request: "+path);
		}
		
		URL u = servletContext.getResource("/WEB-INF/scripts/"+path);
		if (u == null) {
			throw new IOException("Unable to read: "+path);
		}
		return new DataHandler(u);
	}
}
