package au.edu.unimelb.plantcell.seqdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
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
	private List<FastaFile> files;
	private SequenceType sequence_type;
	private Logger logger;
	
	/*
	 * we dont commit each record individually for the SequenceReference file, we instead save them
	 * to a .tsv and then do a batch "load local data infile..." to speedily load a large number of records.
	 * This member holds the path to the temporary file used during populateDatabase()
	 */
	private PrintWriter pw;
	
	public FastaPersistor(final List<FastaFile> files, SequenceType st, final Logger logger, final PrintWriter pw) throws IOException {
		assert(files != null && files.size() > 0 && logger != null);
		this.files = files;
		this.sequence_type = st;
		this.logger = logger;
		this.pw = pw;
	}
	
	public void saveSequenceReferences(final File ref_file_to_read_from, final EntityManager em) throws Exception {
		// run native query to load database into SequenceReference table...
		try {
			logger.info("Loading sequence ref records into database");
			em.getTransaction().begin();
			String path = ref_file_to_read_from.getAbsolutePath();
			path = path.replaceAll("\\\\", "/");
			Query q = em.createNativeQuery("load data local infile \'"+path+
					"\' replace into table SEQUENCEREFERENCE fields terminated by '\\t' (id, length, SEQ_ID, start_offset, FASTA_ID);");
			int ret = q.executeUpdate();
			em.getTransaction().commit();
			logger.info("Completed "+ret+" sequence references (rows) from "+files.size()+" fasta files.");
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			e.printStackTrace();
			throw e;
		}
	}
	
	public int populateDatabase(final EntityManager em, final DatasetDesignation dsd) throws Exception {				
		int total = 0;
		for (FastaFile ff : files) {
			logger.info("Populating database from: "+ff.getPath());
			
			total += populateDatabaseFromFile(pw, em, ff.getPath(), dsd);
		}
		logger.info("Processed "+total+" sequences.");
		return total;
	}
	
	private int populateDatabaseFromFile(final PrintWriter pw, final EntityManager em, String fasta_path, final DatasetDesignation dsd) throws Exception {
		assert(pw != null && fasta_path != null && em != null && dsd != null);
		BufferedReader rdr = new BufferedReader(new FileReader(new File(fasta_path)));
		int n = 0;
		int saved = 0;
		em.getTransaction().begin();
		FastaFile ff = getFastaRecord(em, fasta_path, persistDatasetDesignation(em, dsd));
		em.getTransaction().commit();
		try {
			String line;
			long offset = 0;
			long cur_start = -1;
			SequenceReference sr = newSequenceReference(ff);
			while ((line = rdr.readLine()) != null) {
				if (line.startsWith(">")) {
					n++;
					if (cur_start >= 0) {
						sr.setLength((int) (offset - cur_start - 1));
						sr.save(pw);
						saved++;
						sr = newSequenceReference(ff);
					}
					sr.setStart(offset);
					cur_start = offset;
					int id_end = line.indexOf(' ');
					if (id_end < 0) {
						id_end = line.length();
					}
					String id = line.substring(1, id_end);
					sr.setSequenceID(id);
				} 
				offset += line.length() + 1;		// +1 for newline
			}
			
			if (sr.hasSequenceID()) {
				sr.setLength((int) (offset - cur_start - 1));
				sr.save(pw);
				saved++;
			}
			
			logger.info("Saved "+saved+" sequences, processed: "+n);
			if (n != saved) {
				throw new IOException("Did not save all fasta sequences processed! "+n+" != "+saved);
			}
			return n;
		} catch (Exception e) {
			throw e;
		} finally {
			rdr.close();
		}
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
