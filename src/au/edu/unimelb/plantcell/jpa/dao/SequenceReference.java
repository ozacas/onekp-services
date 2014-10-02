package au.edu.unimelb.plantcell.jpa.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.openjpa.persistence.jdbc.Index;


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
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private FastaFile fasta;
	
	private long start_offset;	// byte offset from start of fasta file
	private int  length;		// how many BYTES to read from the start offset
		
	@Index
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
}
