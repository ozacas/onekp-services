package au.edu.unimelb.plantcell.seqdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QA {

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
	public static boolean compareFastaEntries(String entry1, String entry2, boolean sequence_only) {
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

	public static int getSequenceLength(String entry) {
		String seq = makeSequence(entry);
		if (seq == null) {
			return -1;
		}
		return seq.length();
	}
	
	public static boolean compareFastaSequenceOnly(String entry1, String entry2) {
		String seq1 = makeSequence(entry1);
		String seq2 = makeSequence(entry2);
		if (seq1.length() != seq2.length()) {
			return false;
		}
		return seq1.equalsIgnoreCase(seq2);
	}

	private static String makeSequence(String entry) {
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
	public static boolean compareSummaryLinesEqual(String entry1, String entry2) {
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
}
