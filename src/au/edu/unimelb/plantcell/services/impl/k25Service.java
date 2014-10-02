package au.edu.unimelb.plantcell.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.WordUtils;

import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.Queries;

@Path("/k25")
@Stateless
@Produces(MediaType.TEXT_PLAIN)
public class k25Service {
	private final static Logger logger = Logger.getLogger("k25Service");
	
	@PersistenceContext(unitName="seqdb_onekp_k25")			// must match persistence.xml entry
	private EntityManager seqdb_onekp_k25;
	
	public EntityManager getEntityManager() {
		assert(seqdb_onekp_k25 !=  null);
		return seqdb_onekp_k25;
	}
	
	/**
	 * Throws an exception if the ID is not in a suitable format, otherwise nothing. Called before any
	 * response to a web request is done.
	 * 
	 * @param id
	 * @throws IOException
	 */
	private void validateID(final String id) throws IOException {
		if (!id.matches("^[A-Z]{4}_\\d+")) {
			throw new IOException("Invalid 1KP ID: expected eg. ABCD_1234");
		}
		logger.info(id+" is valid.");
	}
	
	private Response doGet(final String id, SequenceType[] sequence_types) {
		try {
			validateID(id);
			String onekp_sample_id = id.substring(0,4);
			String seq_id = id.substring(5);
			EntityManager em = validateDatabaseConnection();
			Queries q = new Queries(em);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			StringBuilder sb = new StringBuilder(10 * 1024);
			for (SequenceType st : sequence_types) {
				File     f = q.findFastaFile(onekp_sample_id, st);
				if (f != null) {
					sb.append(q.getSequence(f, seq_id));
					sb.append('\n');
				} else {
					logger.warning("Could not locate FASTA file for ("+st+"): "+onekp_sample_id);
				}
			}
			logger.fine("Created result for "+seq_id);
			return Response.ok(WordUtils.wrap(sb.toString(), 60, "\n", true)).build();
		} catch (Exception e) {
			//e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("protein/{id}")
	@RolesAllowed("1kp_user")
	public Response getProtein(@PathParam("id") final String id) { 
		logger.info("Getting protein id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA });
	}
	
	@GET
	@Path("transcript/{id}")
	@RolesAllowed("1kp_user")
	public Response getTranscript(@PathParam("id") final String id) {
		logger.fine("Getting transcript contig id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.RNA });
	}
	
	@GET
	@Path("all/{id}")
	@RolesAllowed("1kp_user")
	public Response getAll(@PathParam("id") final String id) {
		logger.fine("Getting all available sequence: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA, SequenceType.RNA });
	}
	
	@GET
	@Path("proteome/{sample}")
	@RolesAllowed("1kp_user")
	public Response getProteome(@PathParam("sample") final String onekp_sample_id) {
		return getSample(onekp_sample_id, SequenceType.AA);
	}
	
	public Response getSample(final String onekp_sample_id, final SequenceType st) {
		try {
			EntityManager em = validateDatabaseConnection();
			Queries q = new Queries(em);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 

			File     f = q.findFastaFile(onekp_sample_id, st);
			if (f != null) {
					return Response.ok(f).build();
			} else {
					throw new IOException("Could not locate FASTA file for proteome: "+onekp_sample_id);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			logger.warning(e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("transcriptome/{sample}")
	public Response getTranscriptome(@PathParam("sample") final String onekp_sample_id) {
		return getSample(onekp_sample_id, SequenceType.RNA);
	}

	@GET
	@Path("summary/{sample}")
	public Response getSummary(@PathParam("sample") final String onekp_sample_id) {
		try {
			EntityManager em = validateDatabaseConnection();
			Queries q = new Queries(em);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			int n_prots = q.countSequencesInFile(onekp_sample_id, SequenceType.AA);
			int n_transcripts = q.countSequencesInFile(onekp_sample_id, SequenceType.RNA);
			SampleAnnotation sa = q.getSampleMetadata(onekp_sample_id);
			
			StringBuilder sb = new StringBuilder();
			sb.append("Sample ID: "+onekp_sample_id+"\n");
			sb.append("Species: "+sa.getSpecies()+"\n");
			sb.append("Tissue type sequenced: "+sa.getTissueType()+"\n");
			sb.append("Taxonomic family: "+sa.getFamily()+"\n");
			sb.append("Taxonomic order: "+sa.getOrder()+"\n");
			sb.append("Taxonomic clade: "+sa.getClade()+"\n");
			sb.append("Number of predicted proteins: "+n_prots+"\n");
			sb.append("Number of assembled contigs: "+n_transcripts+"\n");
			return Response.ok(sb.toString()).build();
		} catch (Exception e) {
			logger.warning(e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private EntityManager validateDatabaseConnection() throws Exception {
		EntityManager em = getEntityManager();
		if (em == null) {
			logger.warning("No database connection!");
			throw new Exception("No database connection!");
		}
		return em;
	}
}
