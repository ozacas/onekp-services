package au.edu.unimelb.plantcell.main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import au.edu.unimelb.plantcell.jpa.dao.MultiSampleFasta;
import au.edu.unimelb.plantcell.jpa.dao.SequenceReference;

public class makeMultiSampleRecords {
	private static Logger logger = Logger.getLogger("makeMultiSampleRecords");
	
	public void run() throws Exception {		
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
		HashMap<FastaKey,MultiSampleFasta> id2rec = new HashMap<FastaKey,MultiSampleFasta>();

		do {
			String sql = "select sr.SEQ_ID,sr.LENGTH,sr.START_OFFSET,sr.FASTA_ID from SEQUENCEREFERENCE as sr order by sr.id limit "+n+","+batch_size;
			logger.info("SQL: "+sql);
			ResultSet rs = st.executeQuery(sql);	
			if (!rs.isBeforeFirst()) {		// past end of table ie. finished?
				logger.info("Finished processing rows from database.");
				break;
			}
			
			MultiSampleFasta msf;
			FastaKey cur = null;

			while (rs.next()) {
				SequenceReference sr = new SequenceReference();
				sr.setSequenceID(rs.getString(1));
				sr.setStart(rs.getLong(3));
				sr.setLength(rs.getInt(2));
				String sample_id = getSampleID(sr.getSequenceID());
				int fasta_id = rs.getInt(4);
				FastaKey fk = new FastaKey(sample_id, fasta_id);
				Integer count = counts.get(sample_id);
				if (count == null) {
					count = new Integer(1);
					counts.put(sr.getSequenceID(), count);
				} else {
					counts.put(sr.getSequenceID(),  count.intValue()+1);
				}
			

				if (cur == null) {
					cur = fk;
					msf = new MultiSampleFasta();
					msf.setFasta(cur);
					id2rec.put(cur, msf);
				} else if (!fk.equals(cur)) {
					if (id2rec.containsKey(fk)) {
						throw new Exception("Sample "+fk.getSampleID()+" does not have contiguous sequence records -- programmer error!");
					}
					msf = new MultiSampleFasta();
					msf.setFasta(fk);
					id2rec.put(fk, msf);
					cur = fk;
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
	
		logger.info("Saw "+counts.keySet().size()+" distinct sequence ID's.");
	}

	private MultiSampleFasta findFastaKey(
			final Map<FastaKey, MultiSampleFasta> id2rec, final FastaKey fk) {
		return id2rec.get(fk);	
	}

	private void saveMultiSampleFastaRecords(final Collection<MultiSampleFasta> values, final Statement st) throws IOException,SQLException {
		assert(values != null);
		
		st.execute("truncate table MULTISAMPLEFASTA;");
		int id = 1;
		for (MultiSampleFasta msf : values) {
			String line = id+", "+msf.getN()+", "+msf.getStart()+", "+msf.getEnd()+", '"+msf.getSampleID()
					+"', "+msf.getFastaID();
			st.execute("insert into MULTISAMPLEFASTA (ID, N, START, END, ONEKP_SAMPLE_ID, FASTA_ID) values ("+line+");");
			logger.info(line);
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
