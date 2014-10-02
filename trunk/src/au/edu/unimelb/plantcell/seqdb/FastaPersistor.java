package au.edu.unimelb.plantcell.seqdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;

/**
 * Responsible for loading entry offsets into the database for each of the specified fasta files
 * 
 * @author acassin
 *
 */
public class FastaPersistor {
	private static final int BATCH_SIZE = 2000;
	private List<FastaFile> files;
	private SequenceType sequence_type;
	
	public FastaPersistor(final List<FastaFile> files, SequenceType st) {
		this.files = files;
		this.sequence_type = st;
	}
	
	public int populateDatabase(final EntityManager em, final DatasetDesignation dsd) throws Exception {
		Logger logger = Logger.getAnonymousLogger();
				
		int total = 0;
		for (FastaFile ff : files) {
			logger.info("Populating database from: "+ff.getPath());
			
			total += populateDatabaseFromFile(em, ff.getPath(), dsd);
		}
		
		logger.info("Processed "+total+" sequences.");
		return total;
	}
	
	private int populateDatabaseFromFile(final EntityManager em, String fasta_path, final DatasetDesignation dsd) throws Exception {
		assert(em != null && fasta_path != null);
		BufferedReader rdr = new BufferedReader(new FileReader(new File(fasta_path)));
		int batch = 0;
		int n = 0;
		EntityTransaction t = em.getTransaction();
		t.begin();
		FastaFile ff = getFastaRecord(em, fasta_path, persistDatasetDesignation(em, dsd));
		try {
			String line;
			int offset = 0;
			int cur_start = -1;
			SequenceReference sr = newSequenceReference(ff);
			while ((line = rdr.readLine()) != null) {
				if (line.startsWith(">")) {
					if (cur_start >= 0) {
						sr.setLength(offset - cur_start - 1);
						em.persist(sr);
						sr = newSequenceReference(ff);
						batch++;
						n++;
					}
					sr.setStart(offset);
					cur_start = offset;
					int id_end = line.indexOf(' ');
					if (id_end < 0) {
						id_end = line.length();
					}
					sr.setSequenceID(line.substring(1, id_end));
				}
				offset += line.length() + 1;		// +1 for newline
				if (batch % BATCH_SIZE == 0) {
					t.commit();
					t = em.getTransaction();
					t.begin();
				}
			}
			
			if (sr.hasSequenceID()) {
				sr.setLength(offset - cur_start - 1);
				em.persist(sr);
				n++;
			}
			t.commit();
		} catch (Exception e) {
			if (t.isActive()) {
				t.rollback();
			}
			throw e;
		} finally {
			rdr.close();
		}
		return n;
	}

	private SequenceReference newSequenceReference(final FastaFile ff) {
		 SequenceReference sr = new SequenceReference();
		 sr.setFastaFile(ff);
	     return sr;
	}

	private FastaFile getFastaRecord(final EntityManager em, final String path, final DatasetDesignation dsd) {
		Query q = em.createQuery("select f from FastaFile f where f.path=:path");
		q.setParameter("path", path);
		
		FastaFile ret = em.find(FastaFile.class, q.getFirstResult());
		if (ret == null) {
			FastaFile ff = new FastaFile();
			ff.setDesignation(dsd);
			ff.setPathAndSampleID(new File(path));
			ff.setSequenceType(sequence_type);
			em.persist(ff);
			return ff;
		}
		return ret;
	}
	
	private DatasetDesignation persistDatasetDesignation(final EntityManager em, final DatasetDesignation dsd) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<DatasetDesignation> q = cb.createQuery(DatasetDesignation.class);
		Root<DatasetDesignation> b = q.from(DatasetDesignation.class);
		q.select(b).where(cb.equal(b.get("label"), dsd.getLabel()));
		TypedQuery<DatasetDesignation> tq = em.createQuery(q);
					
		int pk_id = tq.getFirstResult();
		DatasetDesignation dd = em.find(DatasetDesignation.class, pk_id);
		if (dd == null) {
			em.persist(dsd);
			return dsd;
		}
		return dd;
	}
}
