package au.edu.unimelb.plantcell.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.WordUtils;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.Queries;

/**
 * All JAX-RS sequence services must implement this interface so that the end-user
 * gets a standard consistent api no matter which dataset they are accessing.
 * 
 * @author acassin
 *
 */
public abstract class OneKPSequenceService {

	/**
	 * Must return the database ID which the service requires access to
	 * @return one of k25, k25s, k39, k49, k59 or k69
	 */
	public abstract DatasetDesignation getDesignation();
	
	/**
	 * Must not return null
	 * @return
	 */
	public abstract Logger getLogger();
	
	/**
	 * Returns a textual summary of the specified sample
	 */
	protected Response getSampleSummary(final String onekp_sample_id) {
		Logger logger = getLogger();
		try {
			Queries q = new Queries(this);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			int n_prots = q.countSequencesInSample(onekp_sample_id, SequenceType.AA);
			int n_transcripts = q.countSequencesInSample(onekp_sample_id, SequenceType.RNA);
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
	
	/**
	 * Returns a single sequence for each of the specified sequence types which matches the given id.
	 * @param id
	 * @param sequence_types
	 * @return
	 */
	protected Response doGet(final String id, SequenceType[] sequence_types) {
		try {
			Logger logger = getLogger();
			validateID(id);
			String onekp_sample_id = id.substring(0,4);
			String seq_id = id.substring(5);
			Queries q = new Queries(this);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			StringBuilder sb = new StringBuilder(10 * 1024);
			for (SequenceType st : sequence_types) {
				File     f = q.findFastaFile(onekp_sample_id, st);
				if (f != null) {
					String result = q.getSequence(f, seq_id);
					if (result != null) {
						sb.append(result);
						sb.append('\n');
					} else {
						logger.warning("Got no result for "+seq_id+" "+getDesignation().getLabel());
					}
				} else {
					logger.warning("Could not locate FASTA file for ("+st+"): "+onekp_sample_id);
				}
			}
			logger.fine("Created result for "+seq_id);
			return Response.ok(WordUtils.wrap(sb.toString(), 60, "\n", true)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	protected Response getSample(final String onekp_sample_id, final SequenceType st) {
		Logger logger = getLogger();
		try {
			Queries q = new Queries(this);
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

	
	/**
	 * Returns a handle to the database manager for the service
	 * @return
	 */
	public abstract EntityManager getEntityManager();
	
	/**
	 * Validates the database manager as returned by {@code getEntityManager()}
	 * @return
	 * @throws Exception
	 */
	public EntityManager validateDatabaseConnection() throws Exception {
		EntityManager em = getEntityManager();
		if (em == null) {
			getLogger().warning("No database connection!");
			throw new Exception("No database connection!");
		}
		return em;
	}
	
	
	/**
	 * validates a sequence ID as provided by the user. Throws if the ID is not valid.
	 */
	public abstract void validateID(final String id) throws IOException;
	
	/**
	 * Returns the protein sequence for the specified ID
	 * @param id
	 * @return JAX-RS response object
	 */
	public abstract Response getProtein(final String id);
	
	/**
	 * Returns the transcript sequence for the specified ID
	 * @param id
	 * @return JAX-RS response
	 */
	public abstract Response getTranscript(final String id);
	
	/**
	 * Returns both the protein (output first) and the transcript (output last) sequence for
	 * the specified ID, if available.
	 * @param id
	 * @return JAX-RS response
	 */
	public abstract Response getAll(@PathParam("id") final String id);
	
	/**
	 * Returns all proteins for the specified sample (large response)
	 * @param onekp_sample_id four letter uppercase sample ID eg. ABCD
	 * @return JAX-RS all known proteins for the specified dataset (service) and sample
	 */
	public abstract Response getProteome(final String onekp_sample_id);
	
	/**
	 * Similar to {@code getProteome()} but this returns the entire transcriptome. Not all
	 * returned transcripts may code for proteins (some transcripts will not have a protein counterpart)
	 * @param onekp_sample_id four letter uppercase sample ID eg. ABCD
	 * @return JAX-RS all known proteins for the specified dataset (service) and sample
	 */
	public abstract Response getTranscriptome(@PathParam("sample") final String onekp_sample_id);
	
	/**
	 * Get summary details of a sample, similar in spirit to the OneKP sample page but this
	 * works on a per dataset (service) basis, where not all samples may be present in a given dataset.
	 * 
	 */
	public abstract Response getSummary(@PathParam("sample") final String onekp_sample_id);
}
