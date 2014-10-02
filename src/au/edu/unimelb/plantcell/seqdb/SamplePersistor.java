package au.edu.unimelb.plantcell.seqdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;

/**
 * Saves each sample description (metadata) to a single table in the database. The site uses this information
 * to provide more information to the end user at the appropriate time
 * 
 * @author acassin
 *
 */
public class SamplePersistor {

	public SamplePersistor() {
	}
	
	public int persist1kpSamples(final EntityManager em, final File csv_1kp_sample_list_andrewl) throws IOException {
		BufferedReader rdr = new BufferedReader(new FileReader(csv_1kp_sample_list_andrewl));
		EntityTransaction t = em.getTransaction();
		t.begin();
		int n = 0;
		try {
			String line;
			while ((line = rdr.readLine()) != null) {
				String[] fields = line.split(",\\s*");
				if (fields.length != 10) {
					throw new IOException("Got illegal sample line: "+line);
				}
				SampleAnnotation sa = new SampleAnnotation();
				sa.setSampleID(fields[0]);
				sa.setSampleType("");
				sa.setClade(fields[1]);
				sa.setOrder(fields[2]);
				sa.setFamily(fields[3]);
				sa.setSpecies(fields[4]);
				sa.setTissueType(fields[5]);
				sa.setClassification("");
				em.persist(sa);
				n++;
			}
			t.commit();
			return n;
		} catch (Exception e) {
			if (t.isActive()) {
				t.rollback();
			}
			throw e;
		} finally {
			rdr.close();
		}
	}
}
