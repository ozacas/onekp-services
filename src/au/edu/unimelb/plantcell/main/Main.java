package au.edu.unimelb.plantcell.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.FastaFile;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.FastaPersistor;
import au.edu.unimelb.plantcell.seqdb.SamplePersistor;

public class Main {
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
			singleton_manager.setFlushMode(FlushModeType.COMMIT);
		}
		return singleton_manager;
	}
	
	public static void main(String[] args) {
		// 1. first populate the sample metadata table
		File f = new File("/tmp/1kp_sample_list_20140925.csv");
		SamplePersistor sp = new SamplePersistor();
		Logger log = Logger.getLogger("OneKP");
		
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
				if (f.isDirectory() && f.getName().matches("^k\\d+$")) {
					return true;
				}
				return false;
			}
			
		});
		for (File dataset_root : datasets) {
			File protein_root = new File(dataset_root, "proteomes");
			File transcriptome_root = new File(dataset_root, "transcriptomes");
			FileFilter fasta_filter = new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					String name = pathname.getName();
					if (pathname.canRead() && (name.endsWith(".fa") | name.endsWith(".fasta")) ) {
						return true;
					}
					return false;
				}
				
			};
			File[] proteomes      = protein_root.listFiles(fasta_filter);
			File[] transcriptomes = transcriptome_root.listFiles(fasta_filter);

			DatasetDesignation dsd = new DatasetDesignation();
			dsd.setLabel(dataset_root.getName());
			dsd.setDescription("");
			
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
				FastaPersistor prot = new FastaPersistor(protein_files, SequenceType.AA);
				int n_prot = prot.populateDatabase(getEntityManager(), dsd);
				FastaPersistor trans = new FastaPersistor(transcript_files, SequenceType.RNA);
				int n_trans = trans.populateDatabase(getEntityManager(), dsd);
				log.info("Persisted sequence records for "+n_prot+" OneKP proteomes to database");
				log.info("Persisted sequence records for "+n_trans+" OneKP transcriptomes to database");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
	}
}
