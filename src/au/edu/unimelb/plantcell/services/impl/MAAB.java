package au.edu.unimelb.plantcell.services.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

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
 * Returns key files to the end user for a paper yet-to-be published on HRGP (Hydroxy-proline-rich-glycoproteins).
 * We implement this service to describe methods and protocols used, rather than bogging down the paper
 * with details only a few people will care about. The paper simply cites a URL rather than the details.
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
	
	/**
	 * Returns perl scripts as plain text to the caller based on the specified path (which must correspond to a name
	 * specified in the published paper). The scripts are served from WEB-INF/scripts so anything placed there
	 * will be served from the .war file
	 * 
	 * @param path
	 * @return
	 */
	@GET
	@Path("scripts/{id}")
	public Response getScript(@PathParam("id") final String path) {
		try {
			if (!path.matches("^\\w+\\.pl$")) {
				throw new IOException("Illegal script pathname: "+path);
			}
			DataHandler dh = getInternalDataFile(path);
			return Response.ok(dh).build();
		} catch (Exception e) {
			return Response.serverError().status(500).entity(e.getMessage()).build();
		}
		
	}
	
	/**
	 * This service implements URLs for methods which are too detailed to include in the body of the paper.
	 */
	@GET
	@Path("methods/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getMethod(@PathParam("id") final String path) {
		try {
			DataHandler dh = getInternalMethodFile(path);
			return Response.ok(dh).build();
		} catch (Exception e) {
			return Response.serverError().status(500).entity(e.getMessage()).build();
		}
	}
	
	@GET
	@Path("images/{id}")
	@Produces("image/png")
	public Response getImage(@PathParam("id") final String path) {
		try {
			DataHandler dh = getInternalImageFile(path);
			return Response.ok(dh).build();
		} catch (Exception e) {
			return Response.serverError().status(500).entity(e.getMessage()).build();
		}
	}
	/**
	 * Returns a list of available scripts to the caller
	 * 
	 * @return
	 */
	@GET
	@Path("list") 
	public Response getScripts() {
		try {
			Set<String> scripts = servletContext.getResourcePaths("/scripts");
			if (scripts == null) {
				throw new IOException("No scripts in onekp.war!");
			}
			StringBuilder sb = new StringBuilder();
			for (String s : scripts) {
				if (!s.endsWith(".pl")) {
					continue;
				}
				sb.append(s);
				sb.append('\n');
			}
			return Response.ok(sb.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().status(500).entity(e.getMessage()).build();
		}
	}
	
	private void rejectBogusPath(final String path) throws IOException {
		if (path == null || path.length() > 1000 || path.matches("^\\s*$") || path.indexOf("..") >= 0) {
			throw new IOException("Bogus file request: "+path);
		}
	}
	
	private DataHandler getInternalFile(final String subfolder, final String path) throws IOException {
		rejectBogusPath(path);
		URL u = servletContext.getResource("/WEB-INF/"+subfolder+"/"+path);
		if (u == null) {
			throw new IOException("Unable to read: "+path);
		}
		return new DataHandler(u);
	}
	
	private DataHandler getInternalDataFile(final String path) throws IOException {
		return getInternalFile("scripts", path);
	}
	
	private DataHandler getInternalMethodFile(final String path) throws IOException {
		return getInternalFile("methods", path);
	}
	
	private DataHandler getInternalImageFile(final String path) throws IOException {
		return getInternalFile("images", path);
	}
}
