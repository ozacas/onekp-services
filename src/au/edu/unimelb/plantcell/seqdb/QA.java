package au.edu.unimelb.plantcell.seqdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;

public class QA {
	private static String DEFAULT_ROOT_URL = "http://localhost:8080/onekp/";		// used for debugging only
	
	private String root_url;
	
	public QA() {
		this(DEFAULT_ROOT_URL);
	}
	
	
	public QA(final String root_url) {
		assert(root_url != null);
		this.root_url = root_url;
	}
	
	/**
	 * Compares two fasta records for equality. If <code>sequence_only</code> is false then
	 * all fasta fields (id, description, sequence) are checked for equality; otherwise only the sequence is checked.
	 * Removes all non-letter characters for comparison.
	 * 
	 * @param entry1 fasta entry 1 eg. >entry1\nSEQISHEREE....
	 * @param entry2 fasta entry 2 eg. >entry2\nCOMPARETHISHERE....
	 * @param sequence_only 
	 * @return true if equal by chosen sort method, false otherwise
	 */
	public boolean compareFastaEntries(String entry1, String entry2, boolean sequence_only) {
		if (sequence_only) {
			return compareFastaSequenceOnly(entry1, entry2);
		} else {
			// compare ID & description
			if (!compareSummaryLinesEqual(entry1, entry2)) {
				return false;
			}
			
			// and finally sequence
			return compareFastaSequenceOnly(entry1, entry2);
		}
	}

	public int getSequenceLength(String entry) {
		String seq = makeSequence(entry);
		if (seq == null) {
			return -1;
		}
		return seq.length();
	}
	
	public boolean compareFastaSequenceOnly(String entry1, String entry2) {
		String seq1 = makeSequence(entry1);
		String seq2 = makeSequence(entry2);
		if (seq1.length() != seq2.length()) {
			return false;
		}
		return seq1.equalsIgnoreCase(seq2);
	}

	private String makeSequence(String entry) {
		StringReader sr = new StringReader(entry);
		BufferedReader rdr = new BufferedReader(sr);
		StringBuilder sb = new StringBuilder(10 * 1024);
		String line;
		try {
			while ((line = rdr.readLine()) != null) {
				if (line.startsWith(">")) {
					continue;
				}
				sb.append(line.trim());
			}
			return sb.toString();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns true if the id and description compare equal (case is ignored during comparison). It is required
	 * that the first line of each entry starts with '>' or false will be returned
	 */
	public boolean compareSummaryLinesEqual(String entry1, String entry2) {
		String[] e1_lines = entry1.split("\n");
		String[] e2_lines = entry2.split("\n");
		if (e1_lines.length < 1 || e2_lines.length < 1 || !e1_lines[0].startsWith(">") || !e2_lines[0].startsWith(">")) {
			return false;
		}
		Pattern p = Pattern.compile("^>(\\S+)\\s+(.*)$");
		Matcher m1 = p.matcher(e1_lines[0]);
		Matcher m2 = p.matcher(e2_lines[0]);
		if (!m1.matches() || !m2.matches()) {
			return false;
		}
		if (m1.group(1).equalsIgnoreCase(m2.group(1))) {
			return false;
		}
		if (!m1.group(2).trim().equalsIgnoreCase(m2.group(2).trim())) {
			return false;
		}
		return true;
	}

	private String getSequence(final URL u) throws Exception {
		Logger.getAnonymousLogger().info(u.toExternalForm());
		InputStream is = u.openConnection().getInputStream();
		StringBuilder sb = new StringBuilder(10 * 1024);
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = rdr.readLine()) != null) {
			sb.append(line);
			sb.append('\n');
		}
		rdr.close();
		return makeSequence(sb.toString());
	}
	
	/**
	 * Uses URL.openConnection() to retreive the specified sequence and test that it has the correct sequence
	 * @param id
	 * @param st
	 * @param seq
	 * @return true if all good, false if fails test
	 */
	public boolean testSequence(final DatasetDesignation dsd, final String id, final SequenceType st, final String seq) {
		assert(dsd != null && id != null && st != null && seq != null && seq.length() > 0);
		String seqType = "protein";
		if (st.equals(SequenceType.RNA)) {
			seqType = "transcript";
		}
		
		try {
			URL u = new URL(getRootURL()+dsd.getLabel()+"/"+seqType+"/"+id);
			return getSequence(u).equalsIgnoreCase(seq);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}


	private String getRootURL() {
		return this.root_url;
	}


	/**
	 * Similar to <code>testSequence()</code> but conducts independent tests of the start/end of the sequence. Either
	 * may be omitted by passing null.
	 * 
	 * @param dsd dataset to search
	 * @param id sequence id to find
	 * @param st sequence type (AA or RNA only)
	 * @param startsWith may be null to omit test
	 * @param endsWith may be null to omit test
	 * @return
	 */
	public boolean testSequenceEnds(final DatasetDesignation dsd, final String id,
			final SequenceType st, final String prefix, final String suffix) {
		Logger logger = Logger.getAnonymousLogger();
		String seqType = "protein";
		if (st.equals(SequenceType.RNA)) {
			seqType = "transcript";
		}
		
		try {
			URL u = new URL(getRootURL()+dsd.getLabel()+"/"+seqType+"/"+id);
			String seq = getSequence(u);
			logger.info(seq);
			
			if (prefix != null) {
				if (!seq.startsWith(prefix)) {
					logger.info("Failed prefix: "+prefix+" "+seq);
					return false;
				}
			}
			if (suffix != null) {
				if (!seq.endsWith(suffix)) {
					logger.info("Failed suffix: "+suffix+" "+seq);
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
