package au.edu.unimelb.plantcell.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.FastaPersistor;
import au.edu.unimelb.plantcell.seqdb.Queries;
import au.edu.unimelb.plantcell.seqdb.SamplePersistor;

public class populateDatabase {
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
		// 1. first populate the sample metadata table
		File f = new File("/tmp/1kp_sample_list_20140925.csv");
		
		Logger log = Logger.getLogger("OneKP");
		log.info("Removing content from all tables. Please wait, this may take a long time.");
		Queries.emptyTables(getEntityManager());
		log.info("Deletion complete.");
		wait5seconds();
		
		SamplePersistor sp = new SamplePersistor();
		
		try {
			int n = sp.persist1kpSamples(getEntityManager(), f);
			log.info("Added metadata for "+n+" 1kp sequenced samples.");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// 2. open set of data files and populate the database
		File root = new File("/1kp/4website/");
		File[] datasets = root.listFiles(new FileFilter() {

			@Override
			public boolean accept(File f) {
				if (f.isDirectory() && f.getName().startsWith("k")) {
					return true;
				}
				return false;
			}
			
		});
		
		for (File dataset_root : datasets) {
			log.info("Found dataset: "+dataset_root.getAbsolutePath());
		}
		wait5seconds();
	
		for (File dataset_root : datasets) {
			File protein_root = new File(dataset_root, "proteomes");
			File transcriptome_root = new File(dataset_root, "transcriptomes");
		
			File[] proteomes      = protein_root.listFiles(newFileFilter());
			File[] transcriptomes = transcriptome_root.listFiles(newFileFilter());
			
			DatasetDesignation dsd = new DatasetDesignation();
			dsd.setLabel(dataset_root.getName());
			dsd.setDescription("");
			
			log.info("Processing dataset: "+dsd.getLabel());
			
			List<FastaFile> protein_files = new ArrayList<FastaFile>();
			for (File protfile : proteomes) {
				FastaFile ff = new FastaFile(protfile);
				protein_files.add(ff);
			}
			List<FastaFile> transcript_files = new ArrayList<FastaFile>();
			for (File transfile : transcriptomes) {
				FastaFile ff = new FastaFile(transfile);
				transcript_files.add(ff);
			}
			
			log.info("Found "+protein_files.size()+" proteomes for dataset: "+dsd.getLabel());
			log.info("Found "+transcript_files.size()+" transcriptomes for dataset: "+dsd.getLabel());
			

			try {
				File seq_ref_tsv = File.createTempFile("4website_seqref", ".seqref.tsv");
				log.info("Storing SequenceReference records to: "+seq_ref_tsv.getAbsolutePath());
				
				log.info("Computing sequence records for "+protein_files.size()+" protein files.");
				PrintWriter pw = new PrintWriter(seq_ref_tsv);
				
				FastaPersistor prot = new FastaPersistor(protein_files, SequenceType.AA, log, pw);
				int n_prot = prot.populateDatabase(getEntityManager(), dsd);
				log.info("Processed "+n_prot+" AA sequence records");

				log.info("Computing sequence records for "+transcript_files.size()+" transcript files.");
				FastaPersistor trans = new FastaPersistor(transcript_files, SequenceType.RNA, log, pw);
				int n_trans = trans.populateDatabase(getEntityManager(), dsd);
				log.info("Processed "+n_trans+" RNA sequence records");
				pw.close();
			
				trans.saveSequenceReferences(seq_ref_tsv, getEntityManager(), dsd);
				seq_ref_tsv.delete();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			log.info("Population of datasets complete.");
		}
	}

	private static void wait5seconds() {
		try {
			Thread.sleep(5 * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private static FileFilter newFileFilter() {
		return new FileFilter() {
			private int added = 0;
			private int max = 10000;
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				if (pathname.canRead() && (name.endsWith(".fa") || name.endsWith(".fasta")) ) {
					added++;
					if (added > max) {
						return false;
					}
					return true;
				}
				return false;
			}
			
		};
	}
}
