package au.edu.unimelb.plantcell.seqdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;

/**
 * Responsible for reading a sequence from the specified file
 * @author acassin
 *
 */
public class Queries {
	private EntityManager em;
	
	public Queries(final EntityManager em) {
		assert(em != null);
		this.em = em;
	}
	
	protected EntityManager getEntityManager() {
		assert(em != null);
		return em;
	}
	
	@SuppressWarnings("unchecked")
	public String getSequence(final File fasta_file, final String seqID) {
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath");
		q.setParameter("fastaFilePath", fasta_file.getAbsolutePath());
		List<FastaFile> fastas = q.getResultList();
		if (fastas.size() < 1) {
			return null;
		}
		FastaFile ff = fastas.get(0);
		q = em.createQuery("select sr from SequenceReference sr where sr.fasta = :fasta AND sr.seqID = :seqID");
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
		Query q = em.createQuery("select count(sa.id) from SampleAnnotation sa");
		return ((Long)q.getSingleResult()).intValue();
	}
	
	public int countOneKPSamplesForSpecies(String species) {
		Query q = em.createQuery("select count(sa.id) from SampleAnnotation sa where sa.species_name = :species");
		q.setParameter("species", species);
		return ((Long)q.getSingleResult()).intValue();
	}
	
	public SampleAnnotation findSampleMetadataByID(String id) {
		Query q = em.createQuery("select sa from SampleAnnotation sa where sa.sample_id = :id");
		q.setParameter("id", id);
		return (SampleAnnotation) q.getSingleResult();
	}
	
	@SuppressWarnings("unchecked")
	public int countSequencesInFile(final FastaFile ff) throws NoResultException {
		Query q = em.createQuery("select ff from FastaFile ff where ff.path = :fastaFilePath");
		q.setParameter("fastaFilePath", ff.getPath());
		try {
			List<FastaFile> fastas =  q.getResultList();
			q = em.createQuery("select count(sr.id) from SequenceReference sr where sr.fasta = :fasta");
			q.setParameter("fasta", fastas.get(0));
			return ((Long) q.getSingleResult()).intValue();
		} catch (NoResultException nre) {
			return -1;
		}
	}
	
	public int countSequencesInFile(final String onekp_sample_id, final SequenceType st) throws NoResultException {
		Query q = em.createQuery("select ff from FastaFile ff where ff.onekp_sample_id = :id AND ff.sequence_type = :st");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		
		// will throw if no result, so no need to check for ff == null
		FastaFile ff = (FastaFile) q.getSingleResult();
		return countSequencesInFile(ff);
	}
	
	public long getNumberofDatasetDesignations(String label_to_match) throws NoResultException {
		Query q = em.createQuery("select count(dsd.id) from DatasetDesignation dsd where dsd.label=:l");
		q.setParameter("l", label_to_match);
		
		return (long) q.getSingleResult();
	}

	public long getNumberOfFastaFiles(String dsd_label) throws NoResultException {
		Query q = em.createQuery("select count(f.id) from DatasetDesignation dsd, FastaFile f where dsd.label = :l AND f.dsd.id = dsd.id");
		q.setParameter("l", dsd_label);
		
		return (long) q.getSingleResult();
	}

	public File findFastaFile(String onekp_sample_id, SequenceType st) throws NoResultException {
		assert(onekp_sample_id != null && onekp_sample_id.length() == 4 && st != null);
		Query q = em.createQuery("select f.path from FastaFile f where f.onekp_sample_id = :id and f.sequence_type = :st");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		
		return new File((String) q.getSingleResult());
	}
	
	public SequenceReference getSequenceReference(String onekp_sample_id, String seq_id) throws NoResultException {
		return getSequenceReference(onekp_sample_id, seq_id, SequenceType.AA);
	}

	public SequenceReference getSequenceReference(String onekp_sample_id, String seq_id, SequenceType st) throws NoResultException {
		assert(onekp_sample_id != null && onekp_sample_id.length() == 4 && seq_id != null && seq_id.length() > 0);
		Query q = em.createQuery("select sr from SequenceReference sr, FastaFile f "+
						"where f.onekp_sample_id = :id and f.sequence_type = :st and sr.fasta.id = f.id and sr.seqID = :seq_id");
		q.setParameter("id", onekp_sample_id);
		q.setParameter("st", st);
		q.setParameter("seq_id", seq_id);
		return (SequenceReference) q.getSingleResult();
	}

	public SampleAnnotation getSampleMetadata(final String onekp_sample_id) throws NoResultException {
		Query q = em.createQuery("select sa from SampleAnnotation sa where sa.sample_id = :id");
		q.setParameter("id", onekp_sample_id);
		return (SampleAnnotation) q.getSingleResult();
	}
}
