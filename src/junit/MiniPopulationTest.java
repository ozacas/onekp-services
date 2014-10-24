package junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.junit.Test;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SampleAnnotation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.FastaPersistor;
import au.edu.unimelb.plantcell.seqdb.Queries;
import au.edu.unimelb.plantcell.seqdb.SamplePersistor;
import au.edu.unimelb.plantcell.services.impl.DummyOneKPService;


public class MiniPopulationTest {
	private EntityManagerFactory singleton;
	private EntityManager singleton_manager;
	private DatasetDesignation dsd = new DatasetDesignation("mini", "A silly test database");
	private Logger logger = Logger.getLogger("MiniPopulationTest");
	
	private String getPersistenceUnit() {
		return "seqdb_mini";
	}
	
	private EntityManagerFactory getEntityManagerFactory() {
		if (singleton == null) {
			singleton = Persistence.createEntityManagerFactory(getPersistenceUnit());
		}
		return singleton;
	}
	
	public EntityManager getEntityManager() {
		if (singleton_manager == null) {
			singleton_manager = getEntityManagerFactory().createEntityManager();
			singleton_manager.setFlushMode(FlushModeType.COMMIT);
		}
		return singleton_manager;
	}
	
	@Test
	public void populateOneKPSampleMetadata() {
		File f = new File("/tmp/1kp_sample_list_20140925.csv");
		SamplePersistor sp = new SamplePersistor();
		try {
			sp.persist1kpSamples(getEntityManager(), f);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// verify that data has populated correctly
		Queries q = new Queries(new DummyOneKPService());
		int n_samples = q.countOneKPSamples();
		assertEquals(1328, n_samples);
		
		// see 1kp sample list webpage: http://www.onekp.com/samples/list.php
		assertEquals(9, q.countOneKPSamplesForSpecies("Glycine soja"));
		
		SampleAnnotation sa = q.findSampleMetadataByID("ABCD");
		assertNotNull(sa);
		assertEquals("ABCD", sa.getSampleID());
		assertEquals("Racomitrium elongatum", sa.getSpecies());
		assertEquals("Mosses", sa.getClade());
		assertEquals("gametophyte", sa.getTissueType());
	}
	
	@Test
	public void populateDatabaseWithMiniFile() {		
		Queries sr = new Queries(new DummyOneKPService());

		long n_datasets_before = sr.getNumberofDatasetDesignations(dsd.getLabel());
		long n_fasta_before    = sr.getNumberOfFastaFilesInDataset(dsd.getLabel());
		
		// chosen file has two sequences in it, we test for that below...
		File f = new File("/home/acassin/test/chlamy_tplate_complex/from-tset-paper/ttray2.unaligned.fasta");
		FastaFile ff = new FastaFile(f);
		List<FastaFile> files = new ArrayList<FastaFile>();
		assertNotNull(dsd);
		files.add(ff);
		
		// save fasta records to database
		assertNotNull(files);
		assertEquals(1, files.size());
	
		try {
			File f2 = File.createTempFile("MinPopTest", "_seqref.tsv");
			FastaPersistor fp = new FastaPersistor(files, SequenceType.AA, logger, new PrintWriter(f2));
			int n = fp.populateDatabase(getEntityManager(), dsd);
			fp.saveSequenceReferences(f2, getEntityManager(), dsd);
			assertEquals(2, n);
			f2.delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Must not throw exception during database population");
		}
		
		// test that all entries have been saved
		assertEquals(2, sr.countSequencesInFile(ff));
		
		// and that the right data is being referenced...
		try {
			String seq1 = sr.getSequence(f, "Vocar20011164m");
			assertNotNull(seq1);
			assertTrue(seq1.startsWith(">Vocar20011164m"));
			assertTrue(seq1.endsWith("AFQ"));
		} catch (Exception e) {
			fail("getSequence() must not fail!");
		}
		
		try {
			String seq2 = sr.getSequence(f, "Cre12.g535350.t1.2");
			assertNotNull(seq2);
			assertTrue(seq2.startsWith(">Cre12.g535350.t1.2"));
			assertTrue(seq2.endsWith("MAQDALRDGGAAALAALQAKVYGLSGAADAMAAWRRQLLAAAPLGTAAALGRLDVSLPAAV"));
		} catch (Exception e) {
			fail("getSequence() must not fail!");
		}
		
		// check that datasetdesignation & fastafile tables have been populated
		assertEquals(sr.getNumberofDatasetDesignations(dsd.getLabel()), n_datasets_before+1);
		assertEquals(sr.getNumberOfFastaFilesInDataset(dsd.getLabel()), n_fasta_before+1);
	}
	
	@Test
	public void largerTest() {		
		/**
		 * NB: all objects will get their own dataset designation separate from earlier tests as it is
		 *     a separate test case. This happens automatically - it is not coded for.
		 */
		File f = new File("/home/acassin/test/chlamy_tplate_complex/hunt_for_ttray/QWRA.usearch-orfs.39.fasta");
		FastaFile ff = new FastaFile(f);
		List<FastaFile> files = new ArrayList<FastaFile>();
		files.add(ff);
		
		try {
			File f2 = File.createTempFile("MinPopTest", "_seqref.tsv");
			FastaPersistor fp = new FastaPersistor(files, SequenceType.AA, logger, new PrintWriter(f2));
			int n = fp.populateDatabase(getEntityManager(), dsd);
			fp.saveSequenceReferences(f2, getEntityManager(), dsd);
			assertEquals(269051, n);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Must not throw exception during database population");
		}
		
	}
}
