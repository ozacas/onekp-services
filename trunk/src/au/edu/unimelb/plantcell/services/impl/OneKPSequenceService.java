package au.edu.unimelb.plantcell.services.impl;

import javax.persistence.EntityManager;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * All JAX-RS sequence services must implement this interface so that the end-user
 * gets a standard consistent api no matter which dataset they are accessing.
 * 
 * @author acassin
 *
 */
public interface OneKPSequenceService {
	/**
	 * Returns a handle to the database manager for the service
	 * @return
	 */
	public EntityManager getEntityManager();
	
	/**
	 * Validates the database manager as returned by {@code getEntityManager()}
	 * @return
	 * @throws Exception
	 */
	public EntityManager validateDatabaseConnection() throws Exception;
	
	/**
	 * Returns the protein sequence for the specified ID
	 * @param id
	 * @return JAX-RS response object
	 */
	public Response getProtein(final String id);
	
	/**
	 * Returns the transcript sequence for the specified ID
	 * @param id
	 * @return JAX-RS response
	 */
	public Response getTranscript(final String id);
	
	/**
	 * Returns both the protein (output first) and the transcript (output last) sequence for
	 * the specified ID, if available.
	 * @param id
	 * @return JAX-RS response
	 */
	public Response getAll(@PathParam("id") final String id);
	
	/**
	 * Returns all proteins for the specified sample (large response)
	 * @param onekp_sample_id four letter uppercase sample ID eg. ABCD
	 * @return JAX-RS all known proteins for the specified dataset (service) and sample
	 */
	public Response getProteome(final String onekp_sample_id);
	
	/**
	 * Similar to {@code getProteome()} but this returns the entire transcriptome. Not all
	 * returned transcripts may code for proteins (some transcripts will not have a protein counterpart)
	 * @param onekp_sample_id four letter uppercase sample ID eg. ABCD
	 * @return JAX-RS all known proteins for the specified dataset (service) and sample
	 */
	public Response getTranscriptome(@PathParam("sample") final String onekp_sample_id);
	
	/**
	 * Get summary details of a sample, similar in spirit to the OneKP sample page but this
	 * works on a per dataset (service) basis, where not all samples may be present in a given dataset.
	 * 
	 */
	public Response getSummary(@PathParam("sample") final String onekp_sample_id);
}
