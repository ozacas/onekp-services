package au.edu.unimelb.plantcell.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
	 * Used by other oases-bases assembly services
	 */
	public Pattern OASES_ID_REGEX = Pattern.compile("^([A-Z]{4}_Locus_\\d+_Transcript_\\d+/\\d+_Confidence_[\\d\\\\.]+_Length_\\d+)(_\\d+)?$");
	
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
		
		Logger logger = getLogger();
		try {
			validateID(id);
		} catch (IOException ioe) {
			logger.warning("Invalid ID: "+id);
			return Response.status(Status.BAD_REQUEST).build();
		}
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
			String   db_seq_id= getSequenceIDFromSequenceID(id, st);
			if (f != null) {
				try {
					q.getSingleSequence(f, cb, db_seq_id);
				} catch (NoResultException e) {
					// if full ID does not match, then fallback to partial ID matching and try again...
					doPartialIDGet(logger, q, f, db_seq_id, st, cb);
				} catch (IOException ioe) {
					ioe.printStackTrace();
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
			} else {
				logger.warning("Could not locate FASTA file for ("+st+"): "+id);
			}
		}
		if (sb.length() > 0) {
			logger.fine("Found result for "+id);
			return Response.ok(sb.toString()).build();
		} else {
			logger.warning("No sequence found for "+id);
			return Response.status(500).entity("No sequence for "+id).build();
		}
	}
	
	private void doPartialIDGet(final Logger logger, final Queries q, final File f, 
			final String partial_id, final SequenceType st, final SequenceCallback cb) {
		assert(f != null && q != null && partial_id != null && logger != null);
		
		try {
			q.getSequencesByPartialID(f, partial_id, cb, st);
		} catch (NoResultException nre) {
			logger.warning("Could not match "+partial_id+" in "+f.getAbsolutePath());
		} catch (Exception e) {
			logger.warning(e.getMessage());
		}
		logger.fine("Found sequence for partial ID: "+partial_id);
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
	
	/**
	 * Validates an identifier of a sequence for K39..K69 oases assemblies
	 */
	public void validateOasesAssemblyID(final String id) throws IOException {
		boolean ok = false;
		Matcher m = OASES_ID_REGEX.matcher(id);
		if (m.matches()) {
			ok = true;
		} else if (id.matches("^[A-Z]{4}_Locus_\\d+_Transcript_\\d+$")) {
			ok = true;
		} else if (id.matches("^[A-Z]{4}_Locus_\\d+$")) {
			ok = true;
		}
		
		if (!ok) {
			throw new IOException("Invalid ID: expected eg. ABCD_Locus_1_Transcript_4 but got: "+id);
		}
		getLogger().info(id+" is valid.");
	}
}
