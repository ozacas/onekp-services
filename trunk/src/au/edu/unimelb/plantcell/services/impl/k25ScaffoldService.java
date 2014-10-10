package au.edu.unimelb.plantcell.services.impl;

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
import au.edu.unimelb.plantcell.seqdb.SingleFastaDatasetQueries;


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
public class k25ScaffoldService implements OneKPSequenceService {
	private final static Logger logger = Logger.getLogger("k25sService");
	
	@PersistenceContext(unitName="seqdb_onekp_k25s")			// must match persistence.xml entry
	private EntityManager seqdb_onekp_k25s;
	
	@Override
	public EntityManager getEntityManager() {
		assert(seqdb_onekp_k25s !=  null);
		return seqdb_onekp_k25s;
	}
	
	@Override
	public EntityManager validateDatabaseConnection() throws Exception {
		EntityManager em = getEntityManager();
		if (em == null) {
			logger.warning("No database connection!");
			throw new Exception("No database connection!");
		}
		return em;
	}
	
	/**
	 * Throws an exception if the ID is not in a suitable format, otherwise nothing. Called before any
	 * response to a web request is done.
	 * 
	 * @param id
	 * @throws IOException
	 */
	private void validateID(final String id) throws IOException {
		if (!id.matches("^scaffold-[A-Z]{4}-\\d+-\\S{1,60}$")) {
			throw new IOException("Invalid 1KP ID: expected eg. scaffold-ABCD-1234");
		}
		logger.info(id+" is valid.");
	}	
	
	private Response doGet(final String id, SequenceType[] sequence_types) {
		try {
			validateID(id);
			String seq_id = id.substring(5);
			EntityManager em = validateDatabaseConnection();
			SingleFastaDatasetQueries q = new SingleFastaDatasetQueries(em);
			StringBuilder sb = new StringBuilder(10 * 1024);
			for (SequenceType st : sequence_types) {
				String entry = q.getSequence(seq_id, st);
				if (entry != null) {
					sb.append(q);
					sb.append('\n');
				} else {
					logger.warning("No entry for "+st+" for ID: "+id);
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
	@Override
	public Response getProtein(@PathParam("id") final String id) { 
		logger.info("Getting protein id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA });
	}
	
	@GET
	@Path("transcript/{id}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getTranscript(@PathParam("id") final String id) {
		logger.fine("Getting transcript contig id is: "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.RNA });
	}
	
	@Override
	public Response getAll(String id) {
		logger.fine("Getting all sequences for "+(id != null));
		return doGet(id, new SequenceType[] { SequenceType.AA, SequenceType.RNA });
	}

	@Override
	public Response getProteome(String onekp_sample_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response getTranscriptome(String onekp_sample_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@GET
	@Path("summary/{sample}")
	@RolesAllowed("1kp_user")
	@Override
	public Response getSummary(@PathParam("sample") final String onekp_sample_id) {
		try {
			EntityManager em = validateDatabaseConnection();
			SingleFastaDatasetQueries q = new SingleFastaDatasetQueries(em);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			int n_prots         = 0;//q.countSequencesInSample(onekp_sample_id, SequenceType.AA);
			int n_transcripts   = 0;// q.countSequencesInSample(onekp_sample_id, SequenceType.RNA);
			SampleAnnotation sa = null; //q.getSampleMetadata(onekp_sample_id);
			
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
	
}
