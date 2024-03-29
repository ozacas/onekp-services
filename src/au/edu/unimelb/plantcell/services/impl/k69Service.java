package au.edu.unimelb.plantcell.services.impl;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;

@Path("/k69")
@Stateless
@Produces(MediaType.TEXT_PLAIN)
public class k69Service extends OneKPSequenceService {
	private final static Logger logger = Logger.getLogger("k69Service");

	@PersistenceUnit(unitName="seqdb_onekp")			// must match persistence.xml entry
	private EntityManagerFactory emf;
	
	private EntityManager seqdb_onekp;
	
	@Override
	public DatasetDesignation getDesignation() {
		return new DatasetDesignation("k69", "");
	}
	
	@Override
	public Logger getLogger() {
		return logger;
	}

	@GET
	@Path("protein/{id : .+}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getProtein(@PathParam("id") final String id) { 
		logger.info("Getting protein id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA });
	}
	
	@GET
	@Path("transcript/{id : .+}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getTranscript(@PathParam("id") final String id) {
		logger.fine("Getting transcript contig id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.RNA });
	}
	
	@GET
	@Path("all/{id : .+}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getAll(@PathParam("id") final String id) {
		logger.fine("Getting all available sequence: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA, SequenceType.RNA });
	}
	
	@GET
	@Path("proteome/{sample}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getProteome(@PathParam("sample") final String onekp_sample_id) {
		return getSample(onekp_sample_id, SequenceType.AA);
	}
	
	@GET
	@Path("transcriptome/{sample}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getTranscriptome(@PathParam("sample") final String onekp_sample_id) {
		return getSample(onekp_sample_id, SequenceType.RNA);
	}

	@GET
	@Path("summary/{sample}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getSummary(@PathParam("sample") final String onekp_sample_id) {
		return getSampleSummary(onekp_sample_id);
	}

	@Override
	public EntityManager getEntityManager() {
		synchronized (emf) {
			if (seqdb_onekp == null) {
				seqdb_onekp = emf.createEntityManager();
			}
			return seqdb_onekp;
		}
	}

	@Override
	public void validateID(String id) throws IOException {
		validateOasesAssemblyID(id);
	}
}
