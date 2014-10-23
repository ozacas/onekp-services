package au.edu.unimelb.plantcell.main;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import au.edu.unimelb.plantcell.jpa.dao.FastaFile;

/**
 * The fasta parsing code has a bug in it where the filename is not correctly parsed due to a limitation
 * of the regular expression. This code corrects the database table for this bug, by using a looser regular
 * expression on those records which were not correctly parsed. A summary of changes made to the table is reported at the end.
 * 
 * @author acassin
 *
 */
public class fixOneKPSampleIDs {
		private static EntityManagerFactory singleton;
		private static EntityManager singleton_manager;
		
		private static String getPersistenceUnit() {
			return "seqdb_onekp";
		}
		
		private static EntityManagerFactory getEntityManagerFactory() {
			if (singleton == null) {
				singleton = Persistence.createEntityManagerFactory(getPersistenceUnit());
			}
			return singleton;
		}
		
		private static EntityManager getEntityManager() {
			if (singleton_manager == null) {
				singleton_manager = getEntityManagerFactory().createEntityManager();
			}
			return singleton_manager;
		}
	
		public static void main(String[] args) {
			Logger l = Logger.getLogger("fixOneKPSamples");
			EntityManager em = getEntityManager();
			
			l.info("Processing FastaFile table for missing sample IDs.");
			em.getTransaction().begin();
			Query q = em.createQuery("select ff from FastaFile ff");
			@SuppressWarnings("unchecked")
			List<FastaFile> files = q.getResultList();
			int ok = 0;
			int bad = 0;
			int reset = 0;
			for (FastaFile ff : files) {
				if (ff.getSampleID().length() != 4) {
					Pattern p = Pattern.compile("\\b([A-Z]{4})");
					Matcher m = p.matcher(ff.getPath());
					if (m.find()) {
						ff.setSampleID(m.group(1));
						reset++;
					} else {
						bad++;
					}
				} else {
					ok++;
				}
			}
			em.getTransaction().commit();
			
			l.info("Corrected "+reset+" fasta files, found "+ok+" files, could not reset "+bad+" records.");
			l.info("Processing complete.");
		}
}
