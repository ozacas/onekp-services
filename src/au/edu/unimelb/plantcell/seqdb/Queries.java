package au.edu.unimelb.plantcell.seqdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.openjpa.persistence.PersistenceException;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReferenceInterface;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.services.impl.OneKPSequenceService;

/**
 * Responsible for reading a sequence from the specified file
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
	
	@SuppressWarnings("unchecked")
	public String getSequence(final File fasta_file, final String seqID) {
		EntityManager em = service.getEntityManager();
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath and ff.dsd = :dsd");
		q.setParameter("fastaFilePath", fasta_file.getAbsolutePath());
		q.setParameter("dsd", getDesignation());
		List<FastaFile> fastas = q.getResultList();
		if (fastas.size() < 1) {
			return null;
		}
		FastaFile ff = fastas.get(0);
		q = em.createQuery("select sr from "+getSeqRefEntityName()+" sr where sr.fasta = :fasta AND sr.seqID = :seqID");
		q.setParameter("fasta", ff);
		q.setParameter("seqID", seqID);
		SequenceReference sr = (SequenceReference) q.getSingleResult();
		if (sr == null) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(ff.getPath(), "r");
			raf.seek(sr.getStart());
			byte[] bytes = new byte[sr.getLength()];
			int n = raf.read(bytes, 0, sr.getLength());
			if (n != sr.getLength()) {
				throw new IOException("Cannot read "+sr.getLength()+" bytes (only got "+n+")!");
			}
			String s = new String(bytes, Charset.forName("US-ASCII"));
			return s;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
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
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath");
		q.setParameter("fastaFilePath", ff.getPath());
		try {
			List<FastaFile> fastas =  q.getResultList();
			if (fastas.size() != 1) {
				Logger logger = service.getLogger();
				logger.warning("Did not get the expected number of FASTA files for "+ff.getPath());
				logger.warning("Expected 1 file, but got "+fastas.size());
			}
			q = em.createQuery("select count(sr.sequenceID) from "+getSeqRefEntityName()+" sr where sr.fastaFile = :fasta");
			q.setParameter("fasta", fastas.get(0));
			return ((Long) q.getSingleResult()).intValue();
		} catch (NoResultException nre) {
			return -1;
		}
	}
	
	public int countSequencesInSample(final String onekp_sample_id, final SequenceType st) throws NoResultException {
		EntityManager em = service.getEntityManager();
		Query q = em.createQuery("select ff from FastaFile ff where ff.onekp_sample_id = :id AND ff.sequence_type = :st and ff.dsd = :dsd");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("dsd", getDesignation());
		
		// will throw if no result, so no need to check for ff == null
		FastaFile ff = (FastaFile) q.getSingleResult();
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

	public File findFastaFile(String onekp_sample_id, SequenceType st) throws NoResultException {
		assert(onekp_sample_id != null && onekp_sample_id.length() == 4 && st != null);
		Query q = service.getEntityManager().createQuery("select f.path from FastaFile f "+
					"where f.onekp_sample_id = :id and f.sequence_type = :st and f.dsd = :dsd");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("dsd", getDesignation());
		
		return new File((String) q.getSingleResult());
	}
	
	public SequenceReferenceInterface getSequenceReference(String onekp_sample_id, String seq_id) throws NoResultException {
		return getSequenceReference(onekp_sample_id, seq_id, SequenceType.AA);
	}

	public SequenceReferenceInterface getSequenceReference(String onekp_sample_id, String seq_id, SequenceType st) throws NoResultException {
		assert(onekp_sample_id != null && onekp_sample_id.length() == 4 && seq_id != null && seq_id.length() > 0);
		Query q = service.getEntityManager().createQuery("select sr from "+getSeqRefEntityName()+" sr, FastaFile f "+
						"where f.onekp_sample_id = :id and f.sequence_type = :st and "+
						"sr.fasta.id = f.id and sr.seqID = :seq_id and f.dsd = :dsd");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("seq_id", seq_id);
		q.setParameter("dsd", getDesignation());
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
}
