package au.edu.unimelb.plantcell.seqdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;

/**
 * Similar conceptually to {@code Queries} class, but this is only for k25s which has a single
 * fasta file with all samples in it.
 * 
 * @author acassin
 *
 */
public class SingleFastaDatasetQueries {
	private EntityManager em;
	
	public SingleFastaDatasetQueries(final EntityManager em) {
		this.em = em;
	}

	public File findFastaFile(final SequenceType st) throws NoResultException {
		assert(st != null);
		Query q = em.createQuery("select f.path from FastaFile f where f.sequence_type = :st");
		q.setParameter("st", st);
		
		return new File((String) q.getSingleResult());
	}

	public String getSequence(final String seqID, final SequenceType st) {
		assert(seqID != null && seqID.length() > 0 && st != null);
		Query q = em.createQuery("select sr from SequenceReference sr where sr.seqID = :seqID");
		q.setParameter("seqID", seqID);
		SequenceReference sr = (SequenceReference) q.getSingleResult();
		if (sr == null) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			File path = findFastaFile(st);
			raf = new RandomAccessFile(path.getAbsolutePath(), "r");
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

	public int countSequencesInSample(final String onekp_sample_id, final SequenceType st) throws NoResultException {
		Query q = em.createQuery("select msf.n from MultiSampleFasta msf, FastaFile ff where msf.fasta_id = ff.fasta_id and msf.sample_id = :sample and ff.sequence_type = :st");
		q.setParameter("sample", onekp_sample_id);
		q.setParameter("st", st);
		Integer n = (Integer) q.getSingleResult();
		return n.intValue();
	}

	public SampleAnnotation getSampleMetadata(final String sample_id) throws NoResultException {
		Query q = em.createQuery("select sa from SampleAnnotation sa where sa.sample_id = :sample");
		q.setParameter("sample", sample_id);
		return (SampleAnnotation) q.getSingleResult();
	}
	
	
}
