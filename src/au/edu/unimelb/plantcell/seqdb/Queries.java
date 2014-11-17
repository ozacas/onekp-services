package au.edu.unimelb.plantcell.seqdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.openjpa.persistence.PersistenceException;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReferenceInterface;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.jpa.dao.k25_SeqRef;
import au.edu.unimelb.plantcell.services.impl.OneKPSequenceService;
import au.edu.unimelb.plantcell.services.impl.SequenceCallback;

/**
 * Responsible for performing essential system-wide queries. The caller must pass a valid {@link OneKPSequenceService}
 * to use as this provides the database <code>EntityManager</code> (and logger) to use. The queries are written to
 * be valid for OpenJPA 2.x, but should also work for EclipseLink and Hibernate (untested).
 * 
 * @author acassin
 *
 */
public class Queries {
	private OneKPSequenceService service;
	
	/**
	 * Sole constructor. Specifies the dataset the queries are to operate on
	 * @param srv
	 * @param dsd
	 */
	public Queries(final OneKPSequenceService srv) {
		assert(srv != null);
		this.service = srv;
	}
	
	private DatasetDesignation getDesignation() {
		return service.getDesignation();
	}
	
	public String getSeqRefEntityName() {
		String ds = service.getDesignation().getLabel().toUpperCase();
		if (ds.equals("K25")) {
			return "k25_SeqRef";
		} else if (ds.equals("K25S")) {
			return "k25s_SeqRef";
		} else if (ds.equals("K39")) {
			return "k39_SeqRef";
		} else if (ds.equals("K49")) {
			return "k49_SeqRef";
		} else if (ds.equals("K59")) {
			return "k59_SeqRef";
		} else if (ds.equals("K69")) {
			return "k69_SeqRef";
		} else {
			return "";
		}
	}
	
	public static void emptyFastaFileTable(final EntityManager em) {
		assert(em != null);
		Query q = em.createQuery("delete from FastaFile");
		@SuppressWarnings("unused")
		int row_cnt = q.executeUpdate();
	}
	
	public static void emptySampleTable(final EntityManager em) {
		assert(em != null);
		Query q = em.createQuery("delete from SampleAnnotation");
		@SuppressWarnings("unused")
		int row_cnt = q.executeUpdate();
	}

	/**
	 * Convenience wrapper around getSingleSequence(), avoids having to know the callback api (handy for testing)
	 * @param fasta_file
	 * @param seqID
	 * @return null if the record cannot be found
	 * @throws Exception something bad happens eg. fasta file does not exist
	 */
	public String getFastaEntry(final File fasta_file, final String seqID) throws Exception {
		final StringBuilder sb = new StringBuilder(10 * 1024);
		SequenceCallback cb = new SequenceCallback() {

			@Override
			public void matchingSequence(String s) {
				sb.append(s);
				sb.append('\n');
			}
			
		};
		getSingleSequence(fasta_file, seqID, cb);
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getSingleSequence(final File fasta_file, final String seqID, final SequenceCallback cb) throws Exception {
		assert(fasta_file != null && seqID != null && cb != null);
		
		EntityManager em = service.getEntityManager();
		assert(em != null);
		
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath and ff.dsd.label = :dsd");
		q.setParameter("fastaFilePath", fasta_file.getAbsolutePath());
		String dsd = getDesignation().getLabel();
		q.setParameter("dsd", dsd);
		List<FastaFile> fastas = q.getResultList();
		Logger l = service.getLogger();
		l.info("Found "+fastas.size()+" fasta files for "+dsd+" - "+seqID);
		if (fastas.size() < 1) {
			return;
		}
		FastaFile ff = fastas.get(0);
		String entity = getSeqRefEntityName();
		String sid = service.getSequenceIDFromSequenceID(seqID);
		l.info("Fetching record from "+entity+" for "+sid+" from "+ff.getPath());
		q = em.createQuery("select sr from "+entity+" sr where sr.fastaFile.id = :fasta AND sr.sequenceID = :seqID");
		q.setParameter("fasta", ff.getID());
		q.setParameter("seqID", sid);
		SequenceReferenceInterface sr;
		try {
			sr = (SequenceReferenceInterface) q.getSingleResult();
			l.info("Fetched single record for "+seqID);
			reportFastaEntries(l, ff, new SequenceReferenceInterface[] { sr }, cb);
		} catch (Exception nre) {
			l.warning(nre.getMessage());
			throw nre;
		}
		
	}

	public int countOneKPSamples() {
		Query q = service.getEntityManager().createQuery("select count(sa.id) from SampleAnnotation sa");
		return ((Long)q.getSingleResult()).intValue();
	}
	
	public int countOneKPSamplesForSpecies(String species) {
		Query q = service.getEntityManager().createQuery("select count(sa.id) from SampleAnnotation sa where sa.species_name = :species");
		q.setParameter("species", species);
		return ((Long)q.getSingleResult()).intValue();
	}
	
	public SampleAnnotation findSampleMetadataByID(String id) {
		Query q = service.getEntityManager().createQuery("select sa from SampleAnnotation sa where sa.sample_id = :id");
		q.setParameter("id", id);
		return (SampleAnnotation) q.getSingleResult();
	}
	
	@SuppressWarnings("unchecked")
	public int countSequencesInFile(final FastaFile ff) throws NoResultException {
		EntityManager em = service.getEntityManager();
		Logger logger = service.getLogger();
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath");
		q.setParameter("fastaFilePath", ff.getPath());
		try {
			List<FastaFile> fastas =  q.getResultList();
			if (fastas.size() > 1) {
				logger.warning("Did not get the expected number of FASTA files for "+ff.getPath());
				logger.warning("Expected 1 file, but got "+fastas.size());
				// FALLTHRU anyway...
			} else if (fastas.size() < 1) {
				throw new NoResultException("No fasta files found for: "+ff.getPath());
			}
			String table = getSeqRefEntityName();
			logger.info("Searching table "+table+" for references to sequences in "+ff.getPath());
			q = em.createQuery("select count(sr.sequenceID) from "+table+" sr where sr.fastaFile = :fasta");
			q.setParameter("fasta", fastas.get(0));
			return ((Long) q.getSingleResult()).intValue();
		} catch (NoResultException nre) {
			return -1;
		}
	}
	
	public int countSequencesInSample(final String onekp_sample_id, final SequenceType st) throws NoResultException {
		EntityManager em = service.getEntityManager();
		String dsd_label = getDesignation().getLabel();
		Logger l = service.getLogger();
		l.info("dsd label is "+dsd_label);
		l.info("onekp sample id is "+onekp_sample_id);
		Query q = em.createQuery("select ff from FastaFile ff where "+
							"ff.onekp_sample_id = :id AND ff.sequence_type = :st and ff.dsd.label = :dsd");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("dsd", dsd_label);
		
		// will throw if no result, so no need to check for ff == null
		FastaFile ff = (FastaFile) q.getSingleResult();
		l.info("Got fasta file "+ff.getPath());
		return countSequencesInFile(ff);
	}
	
	public long getNumberofDatasetDesignations(String label_to_match) throws NoResultException {
		Query q = service.getEntityManager().createQuery("select count(dsd.id) from DatasetDesignation dsd where dsd.label=:l");
		q.setParameter("l", label_to_match);
		
		return (long) q.getSingleResult();
	}

	public long getNumberOfFastaFilesInDataset(String dsd_label) throws NoResultException {
		Query q = service.getEntityManager().createQuery("select count(f.id) from DatasetDesignation dsd, FastaFile f where dsd.label = :l AND f.dsd.id = dsd.id");
		q.setParameter("l", dsd_label);
		
		return (long) q.getSingleResult();
	}

	public File findFastaFile(String id, SequenceType st) throws NoResultException {
		assert(id != null && id.length() == 4 && st != null);
		Logger l = service.getLogger();
		String onekp_sample_id = service.getSampleIDFromSequenceID(id);
		Query q = service.getEntityManager().createQuery("select f.path from FastaFile f "+
					"where f.onekp_sample_id = :id and f.sequence_type = :st and f.dsd.label = :dsd");
		String dsd = getDesignation().getLabel();
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("dsd", dsd);
		l.info("Searching for fasta file: "+st+" "+dsd+" "+onekp_sample_id);
		return new File((String) q.getSingleResult());
	}
	
	public SequenceReferenceInterface getSequenceReference(final String id) throws NoResultException {
		return getSequenceReference(id, SequenceType.AA);
	}

	public SequenceReferenceInterface getSequenceReference(String id, final SequenceType st) throws NoResultException {
		assert(id != null && id.length() > 0);
		String onekp_sample_id = service.getSampleIDFromSequenceID(id);
		String seq_id = service.getSequenceIDFromSequenceID(id);
		String dsd = getDesignation().getLabel();
		Query q = service.getEntityManager().createQuery("select sr from "+getSeqRefEntityName()+" sr, FastaFile f "+
						"where f.onekp_sample_id = :id and f.sequence_type = :st and "+
						"sr.fasta.id = f.id and sr.seqID = :seq_id and f.dsd.label = :dsd");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("seq_id", seq_id);
		q.setParameter("dsd", dsd);
		service.getLogger().info("Fetching sequence: "+st+" "+onekp_sample_id+ " "+seq_id+" "+dsd);
		return (SequenceReferenceInterface) q.getSingleResult();
	}

	public SampleAnnotation getSampleMetadata(final String onekp_sample_id) throws NoResultException {
		Query q = service.getEntityManager().createQuery("select sa from SampleAnnotation sa where sa.sample_id = :id");
		q.setParameter("id", onekp_sample_id);
		return (SampleAnnotation) q.getSingleResult();
	}

	public static void emptyTables(final EntityManager em) {
		em.getTransaction().begin();
		emptySampleTable(em);
		emptyFastaFileTable(em);
		emptyDatasetDesignationTable(em);
		em.getTransaction().commit();

		for (String s : new String[] { "K25_SEQREF", "K25S_SEQREF", "K39_SEQREF", "K49_SEQREF", "K59_SEQREF", "K69_SEQREF" }) {
			emptySequenceReferenceTable(em, s);
		}
	}

	private static void emptySequenceReferenceTable(final EntityManager em, final String s) {
		em.getTransaction().begin();
		try {
			@SuppressWarnings("unused")
			int row_cnt = em.createNativeQuery("truncate table "+s).executeUpdate();
			em.getTransaction().commit();
		} catch (PersistenceException pe) {
			em.getTransaction().rollback();
			// NO-OP: be silent as missing/empty tables are not a problem
		}
	}

	private static void emptyDatasetDesignationTable(final EntityManager em) {
		assert(em != null);
		Query q = em.createQuery("delete from DatasetDesignation");
		@SuppressWarnings("unused")
		int row_cnt = q.executeUpdate();
	}

	/**
	 * Returns the datasets which provide 
	 * @param onekp_sample_id
	 * @param st
	 * @return
	 */
	public String getDatasetsAsString(final String onekp_sample_id, final SequenceType st) throws NoResultException {
		StringBuilder sb = new StringBuilder();
		EntityManager em = service.getEntityManager();
		Query q = em.createQuery("select distinct(dsd.label) from FastaFile ff, DatasetDesignation dsd where "+
					"ff.dsd.id = dsd.id and ff.onekp_sample_id = :sample and ff.sequence_type = :st order by dsd.label");
		q.setParameter("sample", onekp_sample_id);
		q.setParameter("st", st);
		@SuppressWarnings("unchecked")
		List<String> datasets = q.getResultList();
		for (String s : datasets) {
			sb.append(s);
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	/**
	 * Similar to <code>getSequence()</code> this method supports callbacks for each fasta record
	 * as the partial ID specified may match multiple isoforms/proteins.
	 * 
	 * @param f
	 * @param partial_id
	 * @param sequenceCallback called with the fasta-format record, for each matching sequence to partial_id
	 * @return
	 * @throws Exception 
	 */
	public void getSequencesByPartialID(final File fasta_file, final String partial_id, final SequenceCallback sc) throws Exception {
		EntityManager em = service.getEntityManager();
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath and ff.dsd.label = :dsd");
		q.setParameter("fastaFilePath", fasta_file.getAbsolutePath());
		String dsd = getDesignation().getLabel();
		q.setParameter("dsd", dsd);
		List<FastaFile> fastas = q.getResultList();
		Logger l = service.getLogger();
		l.info("Found "+fastas.size()+" fasta files for "+dsd+" - "+partial_id);
		if (fastas.size() < 1) {
			return;
		}
		FastaFile ff = fastas.get(0);
		String entity = getSeqRefEntityName();
		l.info("Fetching record from "+entity);
		
		q = em.createNativeQuery("SELECT sr.SEQ_ID,sr.START,sr.LENGTH FROM K39_SEQREF sr WHERE sr.FASTAFILE_ID = ?1 AND (sr.SEQ_ID LIKE CONCAT(?2, '%'))");
		q.setParameter(1, ff.getID());
		q.setParameter(2, service.getSequenceIDFromSequenceID(partial_id));
		q.setMaxResults(1000);
		try {
			List<Object[]> results = q.getResultList();
			l.info("Found "+results.size()+" matching sequences for "+partial_id);
			
			// we create a list here so that we only open the file once to report all the records
			ArrayList<SequenceReferenceInterface> records = new ArrayList<SequenceReferenceInterface>(results.size());
			
			for (Object[] o : results) {
				// doesnt matter which subclass of SequenceReferenceInterface as reportFastaRecords() doesnt care
				k25_SeqRef sr = new k25_SeqRef();
				sr.setFastaFile(ff);
				sr.setSequenceID(o[0].toString());
				sr.setStart(Long.valueOf(o[1].toString()));
				sr.setLength(Integer.valueOf(o[2].toString()));
				records.add(sr);
			}
			
			reportFastaEntries(l, ff, records.toArray(new SequenceReferenceInterface[0]), sc);
		} catch (Exception nre) {
			l.warning(nre.getMessage());
			throw nre;
		}
	}

	private void reportFastaEntries(final Logger l, final FastaFile ff, 
			final SequenceReferenceInterface[] sr_array, final SequenceCallback cb) throws Exception {
		assert(cb != null && sr_array != null && sr_array.length > 0 && ff != null && l != null);
		
		RandomAccessFile raf = null;
		try {
			l.info("Opening "+ff.getPath()+" to report "+sr_array.length+" sequence records.");
			raf = new RandomAccessFile(ff.getPath(), "r");
			for (SequenceReferenceInterface sr : sr_array) {
				raf.seek(sr.getStart());
				byte[] bytes = new byte[sr.getLength()];
				int n = raf.read(bytes, 0, sr.getLength());
				if (n != sr.getLength()) {
					throw new IOException("Cannot read "+sr.getLength()+" bytes (only got "+n+")!");
				}
				String s = new String(bytes, Charset.forName("US-ASCII"));
				cb.matchingSequence(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
