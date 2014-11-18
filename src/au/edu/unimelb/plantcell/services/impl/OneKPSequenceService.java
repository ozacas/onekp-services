package au.edu.unimelb.plantcell.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
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
	 * Returns true if the specified ID is probably full length (we cant reliably tell if say a digit is missing)
	 * otherwise false
	 */
	public abstract boolean isFullLengthID(String id);
	
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
	 * Subclasses of the service must override this with their own implementation if not valid for the ID's
	 * being requested by the user. This implementation requires the sample id to begin the identifier supplied by
	 * the user. It is not the job of this method to validate input: that will have already been done by validateID()
	 * 
	 * @param id
	 * @return
	 */
	public String getSampleIDFromSequenceID(final String id) {
		assert(id != null);
		return id.substring(0,4);
	}
	
	/**
	 * Subclasses must override this to ensure the correct id is looked up in the database. The default implementation
	 * does not use the sample ID to do lookup, which is at the start of the id, as that would require 
	 * longer ID's (redundant information) to be stored in the database.
	 * 
	 * @param id whatever the user provides
	 * @return
	 */
	public String getSequenceIDFromSequenceID(final String id, SequenceType st) {
		assert(id != null && st != null);
		String ret = id.substring(5);
		/*
		 * an Oases protein ID has _x appended to the ID (in case a transcript has more than one ORF). 
		 * If this ID is used to search for a transcript it will fail. So we recognise this case and handle it appropriately. Otherwise the ID is left alone. 
		 */
		if (st == SequenceType.RNA && ret.startsWith("Locus_")) {
			Pattern p = Pattern.compile("Confidence_[\\d\\\\.]+_Length_\\d+(_\\d+)$");
			Matcher m = p.matcher(ret);
			if (m.find()) {
				return ret.substring(0, ret.length() - m.group(1).length());
			} else {
				return ret;
			}
		} else {
			return ret;
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
			Queries q = new Queries(this);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			final StringBuilder sb = new StringBuilder(10 * 1024);
			SequenceCallback cb = new SequenceCallback() {

				@Override
				public void matchingSequence(String s) {
					sb.append(s);
					sb.append('\n');
				}
				
			};
			String sample_id = getSampleIDFromSequenceID(id);
			logger.info("Extracted OneKP sample ID: "+sample_id+" from "+id);
			for (SequenceType st : sequence_types) {
				File     f = q.findFastaFile(st, sample_id);
				if (f != null) {
					q.getSingleSequence(f, cb, getSequenceIDFromSequenceID(id, st));
				} else {
					logger.warning("Could not locate FASTA file for ("+st+"): "+id);
				}
			}
			if (sb.length() > 0) {
				logger.fine("Found result for "+id);
				return Response.ok(WordUtils.wrap(sb.toString(), 90, "\n", true)).build();
			} else {
				logger.warning("No sequence found for "+id);
				return Response.status(500).entity("No sequence for "+id).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private Response doShortGet(final String partial_id, final SequenceType[] sequence_types) {
		try {
			final Logger logger = getLogger();
			validateID(partial_id);
			Queries q = new Queries(this);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 
			final StringBuilder sb = new StringBuilder(10 * 1024);
			for (SequenceType st : sequence_types) {
				String sample_id = getSampleIDFromSequenceID(partial_id);
				File     f = q.findFastaFile(st, sample_id);
				if (f != null) {
					q.getSequencesByPartialID(f, partial_id, new SequenceCallback() {

						@Override
						public void matchingSequence(final String s) {
							//logger.info(s);
							sb.append(s);
							sb.append('\n');
						}
						
					}, st);
				} else {
					logger.warning("Could not locate FASTA file for ("+st+"): "+partial_id);
				}
			}
			logger.fine("Created result for "+partial_id);
			return Response.ok(WordUtils.wrap(sb.toString(), 90, "\n", true)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Similar to doGet() but this supports queries using the short ID. This will ultimately
	 * boil down to a query using <code>LIKE 'id%'</code> to match the relevant IDs. Its possible
	 * that more than one sequence may match the partial name, if thats the case all will be reported.
	 * 
	 * @param id
	 * @param types
	 * @return
	 */
	protected Response doShortOrLongGet(final String id, SequenceType[] types) {
		if (!isFullLengthID(id)) {
			return doShortGet(id, types);
		} else {
			return doGet(id, types);
		}
	}
	
	protected Response getSample(final String onekp_sample_id, final SequenceType st) {
		Logger logger = getLogger();
		try {
			Queries q = new Queries(this);
			if (q != null) {
				logger.info("Constructed valid queries object.");
			} 

			if (onekp_sample_id.length() != 4) {
				throw new Exception("OneKP sample ID must be four letters!");
			}
			
			File     f = q.findFastaFile(st, onekp_sample_id);
			if (f != null) {
					return Response.ok(f).build();
			} else {
					throw new IOException("Could not locate FASTA file for: "+onekp_sample_id+" "+st);
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
	 * Validates a sequence ID as provided by the user. Throws if the ID is not valid.
	 * 
	 * Throws an exception if the ID is not in a suitable format, otherwise nothing. Called before any
	 * response to a web request is done.
	 * 
	 * @param id
	 * @throws IOException
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
	public abstract Response getAll(final String id);
	
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
	public abstract Response getTranscriptome(final String onekp_sample_id);
	
	/**
	 * Get summary details of a sample, similar in spirit to the OneKP sample page but this
	 * works on a per dataset (service) basis, where not all samples may be present in a given dataset.
	 * 
	 */
	public abstract Response getSummary(final String onekp_sample_id);
}
