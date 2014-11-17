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
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.Queries;


/**
 * The implementation of this service is quite different to the contig k25 service. The
 * reason for that is the fasta file's served are not one file per sample but rather one fasta file only
 * with all samples. This changes the queries considerably.
 * 
 * @author acassin
 *
 */
@Path("/k25s")
@Stateless
@Produces(MediaType.TEXT_PLAIN)
public class k25ScaffoldService extends OneKPSequenceService {
	private final static Logger logger = Logger.getLogger("k25sService");
	
	@PersistenceUnit(unitName="seqdb_onekp")			// must match persistence.xml entry
	private EntityManagerFactory emf;

	private static EntityManager seqdb_onekp;
	
	@Override 
	public DatasetDesignation getDesignation() {
		return new DatasetDesignation("k25s", "");
	}
	
	@Override
	public String getSampleIDFromSequenceID(final String id) {
		assert(id != null);
		int start = "scaffold-".length();
		return id.substring(start,start+4);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isFullLengthID(final String id) {
		if (id != null && id.matches("^scaffold-[A-Z]{4}-\\d+-\\S{1,60}$")) {
			return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validateID(final String id) throws IOException {
		if (!isFullLengthID(id)) {
			throw new IOException("k25s dataset expects id's of the form: scaffold-ABCD-1234");
		}
		logger.info(id+" is valid.");
	}
	
	/**
	 * Subclasses must override this to ensure the correct id is looked up in the database
	 * @param id whatever the user provides
	 * @return
	 */
	public String getSequenceIDFromSequenceID(final String id) {
		return id;
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
	
	@GET
	@Path("protein/{id}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getProtein(@PathParam("id") final String id) { 
		Logger l = getLogger();
		l.info("Getting protein id is: "+(id != null));
		Response r = doGet(id, new SequenceType[] { SequenceType.AA });
		l.info("Protein get completed.");
		return r;
	}
	
	@GET
	@Path("transcript/{id}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getTranscript(@PathParam("id") final String id) {
		Logger l = getLogger();
		l.fine("Getting transcript contig id is: "+(id != null));
		Response r = doGet(id, new SequenceType[] { SequenceType.RNA });
		l.info("Transcript get completed.");
		return r;
	}
	
	@GET
	@Path("all/{id}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getAll(String id) {
		Logger l = getLogger();
		l.fine("Getting all sequences for "+(id != null));
		Response r = doGet(id, new SequenceType[] { SequenceType.AA, SequenceType.RNA });
		l.info("Get all completed.");
		return r;
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
		try {
			Queries q = new Queries(this);
			if (q != null) {
				getLogger().info("Constructed valid queries object.");
			} 
			int n_prots         = q.countSequencesInSample(onekp_sample_id, SequenceType.AA);
			int n_transcripts   = q.countSequencesInSample(onekp_sample_id, SequenceType.RNA);
			SampleAnnotation sa = q.getSampleMetadata(onekp_sample_id);
			
			StringBuilder sb = new StringBuilder();
			sb.append("Sample ID: "+onekp_sample_id+"\n");
			sb.append("Species: "+sa.getSpecies()+"\n");
			sb.append("Tissue type sequenced: "+sa.getTissueType()+"\n");
			sb.append("Taxonomic family: "+sa.getFamily()+"\n");
			sb.append("Taxonomic order: "+sa.getOrder()+"\n");
			sb.append("Taxonomic clade: "+sa.getClade()+"\n");
			sb.append("Number of predicted proteins: "+n_prots+"\n");
			sb.append("Number of assembled scaffolds: "+n_transcripts+"\n");
			sb.append("Available in datasets (AA): "+q.getDatasetsAsString(onekp_sample_id, SequenceType.AA)+"\n");
			sb.append("Available in datasets (DNA): "+q.getDatasetsAsString(onekp_sample_id, SequenceType.RNA)+"\n");
			return Response.ok(sb.toString()).build();
		} catch (Exception e) {
			logger.warning(e.getMessage());
			String msg = "ERROR: no results available for "+onekp_sample_id+" in dataset "+this.getDesignation().getLabel()+"\n"+
						 "       detailed error message is: "+e.getMessage()+"\n";
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	
}
