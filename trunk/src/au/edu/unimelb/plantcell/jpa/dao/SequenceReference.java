package au.edu.unimelb.plantcell.jpa.dao;

import java.io.PrintWriter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Refers to an entry in a single FASTA file, identified by a byte-offset and length.
 * The entry starts with a definition line eg. ^>SEQ_ID SEQ_DESCR_OPTIONAL followed
 * by sequence lines. For this approach to work, the FASTA file must be well-defined
 * 
 * @author acassin
 *
 */
@Entity
@Table(name="SEQUENCEREFERENCE")
public class SequenceReference {
	private static int unique_id = 1;
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private FastaFile fasta;
	
	private long start_offset;	// byte offset from start of fasta file
	private int  length;		// how many BYTES to read from the start offset
		
	@Column(name="SEQ_ID")
	private String seqID;
	
	public FastaFile getFastaFile() {
		return fasta;
	}
	
	public void setFastaFile(final FastaFile ff) {
		assert(ff != null);
		this.fasta = ff;
	}
	
	public long getStart() {
		return start_offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public void setStart(final long new_start) {
		start_offset = new_start;
	}
	
	public void setLength(final int new_length) {
		length = new_length;
	}

	public void setSequenceID(String descr) {
		assert(descr != null && descr.length() > 0);
		seqID = descr;
	}
	
	public String getSequenceID() {
		return seqID;
	}

	public boolean hasSequenceID() {
		return (seqID != null && seqID.length() > 0);
	}

	/**
	 * Saves the current state of this in a format suitable for mysql's load local data infile... (column order is VERY important
	 * and must match schema exactly)
	 * 
	 * @param pw
	 */
	public void save(final PrintWriter pw) {
		assert(pw != null);
		pw.append(String.valueOf(unique_id++));		// we cant use the id field: since we are not using getEntityManager().persist()
		pw.append('\t');
		pw.append(String.valueOf(getLength()));
		pw.append('\t');
		pw.append(getSequenceID());
		pw.append('\t');
		pw.append(String.valueOf(getStart()));
		pw.append('\t');
		pw.println(String.valueOf(fasta.getID()));
	}
}
