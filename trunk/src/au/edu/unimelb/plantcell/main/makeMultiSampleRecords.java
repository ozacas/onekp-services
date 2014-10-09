package au.edu.unimelb.plantcell.main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

import au.edu.unimelb.plantcell.jpa.dao.MultiSampleFasta;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;

public class makeMultiSampleRecords {
	private static Logger logger = Logger.getLogger("makeMultiSampleRecords");
	
	public void run() throws Exception {
		HashMap<FastaKey,MultiSampleFasta> id2rec = new HashMap<FastaKey,MultiSampleFasta>();
		String cur = null;
		
		Properties props = new Properties();
		props.put("user", "root");
		props.put("password", "Ethgabitc!");
		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/seqdb_onekp_k25s", props);
		if (conn == null) {
			throw new IOException("Cannot establish database connection!");
		}
		Statement st = conn.createStatement(); 
		int batch_size = 10000;
		int n = 0;
		st.setFetchSize(batch_size);
		st.setMaxRows(batch_size);
		HashMap<String,Integer> counts = new HashMap<String,Integer>();
		do {
			String sql = "select sr.SEQ_ID,sr.LENGTH,sr.START_OFFSET,sr.FASTA_ID from SEQUENCEREFERENCE as sr order by sr.id limit "+n+","+batch_size;
			logger.info("SQL: "+sql);
			ResultSet rs = st.executeQuery(sql);	
			if (!rs.isBeforeFirst()) {		// past end of table ie. finished?
				logger.info("Finished processing rows from database.");
				break;
			}
			
			if (n >= 200000) { break; }
			MultiSampleFasta msf;
			while (rs.next()) {
				SequenceReference sr = new SequenceReference();
				sr.setSequenceID(rs.getString(1));
				sr.setStart(rs.getLong(3));
				sr.setLength(rs.getInt(2));
				String sample_id = getSampleID(sr.getSequenceID());
				FastaKey fk = new FastaKey(sample_id, rs.getInt(4));
				Integer count = counts.get(sample_id);
				if (count == null) {
					count = new Integer(1);
					counts.put(sr.getSequenceID(), count);
				} else {
					counts.put(sr.getSequenceID(),  count.intValue()+1);
				}
			

				if (cur == null) {
					cur = sample_id;
					msf = new MultiSampleFasta();
					msf.setSampleID(cur);
					id2rec.put(fk, msf);
				} else if (!sample_id.equals(cur)) {
					if (id2rec.containsKey(sample_id)) {
						throw new Exception("Sample "+sample_id+" does not have contiguous sequence records -- programmer error!");
					}
					msf = new MultiSampleFasta();
					msf.setSampleID(sample_id);
					id2rec.put(fk, msf);
					cur = sample_id;
				} else {
					msf = findFastaKey(id2rec, cur);
					if (msf == null) {
						throw new Exception("No record of sample: "+cur);
					}
				}
				msf.updateToIncludeSequence(sr);
			}
			n += batch_size;
			logger.info("Processed "+n+" records: "+id2rec.size()+" samples.");
		} while (true);
		
		saveMultiSampleFastaRecords(id2rec.values(), conn.createStatement());
		
		for (String key : counts.keySet()) {
			//logger.info("Got sequence ID: "+key+" "+counts.get(key)+" times.");
		}
		logger.info("Saw "+counts.keySet().size()+" distinct sequence ID's.");
	}

	private MultiSampleFasta findFastaKey(
			HashMap<FastaKey, MultiSampleFasta> id2rec, String cur) {
		for (FastaKey fk : id2rec.keySet()) {
			if (fk.hasSampleID(cur)) {
				return id2rec.get(fk);
			}
		}
		return null;
	}

	private void saveMultiSampleFastaRecords(final Collection<MultiSampleFasta> values, final Statement st) throws IOException,SQLException {
		assert(values != null);
		
		st.execute("truncate table MULTISAMPLEFASTA;");
		int id = 1;
		for (MultiSampleFasta msf : values) {
			boolean ok = st.execute("insert into MULTISAMPLEFASTA (ID, N, START, END, ONEKP_SAMPLE_ID) values ("+
					id+", "+msf.getN()+", "+msf.getStart()+", "+msf.getEnd()+", '"+msf.getSampleID()
					+"');");
			id++;
		}
	}

	public static void main(String[] args) {
		try {
			makeMultiSampleRecords mmsr = new makeMultiSampleRecords();
			mmsr.run();
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static String getSampleID(final String sequenceID) throws Exception {
		if (sequenceID.matches("^scaffold-[A-Z]{4}-\\d+-.*$")) {
			int start = "scaffold-".length();
			return sequenceID.substring(start, start+4);
		}
		throw new Exception("Invalid sequence ID: no OneKP sample ID!");
	}
}
