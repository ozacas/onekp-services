package junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.junit.Test;

import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.Queries;

public class ReferenceTest {
	private EntityManagerFactory singleton;
	private EntityManager singleton_manager;
	
	private String getPersistenceUnit() {
		return "seqdb_onekp_k25";
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
		}
		return singleton_manager;
	}
	
	public String getSequence(final SequenceReference sr, final File f) throws IOException {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(f, "r");
			raf.seek(sr.getStart());
			byte[] vec = new byte[sr.getLength()];
			int n = raf.read(vec);
			if (n != vec.length) {
				throw new IOException("Unable to read "+vec.length+" bytes from "+f.getAbsolutePath());
			}
			return new String(vec, Charset.forName("US-ASCII"));
		} finally {
			if (raf != null) {
				raf.close();
			}
		}
	}
	
	@Test
	public void referenceTest() {
		Queries q = new Queries(getEntityManager());
		
		try {
			File abcd = q.findFastaFile("ABCD", SequenceType.AA);
			Logger log = Logger.getLogger("ReferenceTest");

			log.info("Got "+abcd.getAbsolutePath()+" for ABCD protein fasta file");
			assertEquals("/1kp/4website/k25/proteomes/ABCD-SOAPdenovo-Trans-assembly.prots.out.fa", abcd.getAbsolutePath());
			
			// check the first, last and a couple of randomly chosen proteins for correct sequence
			SequenceReference first_seq = q.getSequenceReference("ABCD", "1");
			assertNotNull(first_seq);
			log.info("Resolved ABCD_"+first_seq.getSequenceID());
			SequenceReference last_seq  = q.getSequenceReference("ABCD", "78577");
			SequenceReference rand1_seq = q.getSequenceReference("ABCD", "9731");
			assertNotNull(last_seq);
			assertNotNull(rand1_seq);
			
			String s1 = getSequence(first_seq, abcd);
		    log.info(s1);
			String s2 = getSequence(last_seq, abcd);
			log.info(s2);
			String s3 = getSequence(rand1_seq, abcd);
			log.info(s3);
			assertNotNull(s1);
			assertNotNull(s2);
			assertNotNull(s3);
			System.out.println(s2);
			System.out.println(s3);
			log.info("Test completed.");
		} catch (NoResultException nre) {
			nre.printStackTrace();
			fail("Cannot get result!");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail("Unable to read sequences!");
		}
	}
	
}
