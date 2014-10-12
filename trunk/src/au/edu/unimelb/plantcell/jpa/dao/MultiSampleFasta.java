package au.edu.unimelb.plantcell.jpa.dao;

import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import au.edu.unimelb.plantcell.main.FastaKey;

/**
 * Fasta files describing more than one OneKP sample need a special table to provide
 * support for finding a particular sample's sequences, quickly. This is needed to report
 * entire proteomes or transcriptomes to the end-user. We rely on the fact that each sample
 * occurs in exactly one contiguous region of the fasta file.
 * 
 * This table is not populated for datasets which have separate files per sample eg. k25
 * 
 * This record stores key metadata to enable the web service operations for the k25s dataset to succeed
 * and is populated by a program called 'makeMultiSampleRecords' based on an existing (fully populated) database
 * 
 * @author acassin
 *
 */
@Entity
@Table(name="MULTISAMPLEFASTA")
public class MultiSampleFasta {
	private final static Logger logger = Logger.getLogger("MultiSampleFasta");
	private int logged = 0;
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	private String onekp_sample_id;	// must be four letters eg. ABCD (null not permitted)
	private int    n;		// number of sequences for the specified sample AND Fasta file
	private long   start;	// where do the sequences start in the fasta file
	private long   end;		// and where do they stop? (contiguity is required)
	private int    fasta_id;		// foreign key into FastaFile table
	
	public MultiSampleFasta() {
		setSampleID("");
		setN(0);
		setStart(Long.MAX_VALUE);
		setEnd(0);
		setFastaID(-1);
	}
	
	public int getFastaID() {
		return fasta_id;
	}
	
	public void setFastaID(int new_fasta_id) {
		fasta_id = new_fasta_id;
	}
	
	public String getSampleID() {
		return onekp_sample_id;
	}
	
	public void setSampleID(final String new_id) {
		this.onekp_sample_id = new_id;
	}
	
	public long getStart() {
		return this.start;
	}
	
	public void setStart(long new_start) {
		this.start = new_start;
	}
	
	public long getEnd() {
		return this.end;
	}
	
	public void setEnd(long new_end) {
		this.end = new_end;
	}
	
	public int getN() {
		return this.n;
	}
	
	public void setN(int n) {
		this.n = n;
	}
	
	public void updateToIncludeSequence(final SequenceReference sr) throws IllegalArgumentException {
		assert(sr != null);
		this.n++;
		String sample_id = getSampleID();
		if (!(sr.getSequenceID().indexOf(sample_id) >= 0)) {
			throw new IllegalArgumentException("No sample ID in sequence ID!");
		}
		if (sample_id.equals("ABIJ") && logged < 10) {
			logger.info(sample_id+": fasta="+getFastaID()+" "+sr.getStart()+ " - "+(sr.getStart()+sr.getLength()));
			logged++;
		}
		long start = sr.getStart();
		if (start < getStart()) {
			setStart(start);
			logger.info("Adjusted start to "+start);
		}
		long end = start + sr.getLength();
		if (end > getEnd()) {
			setEnd(end);
			//logger.info("Adjusted end to "+end);
		}
	}

	public void setFasta(FastaKey cur) {
		setFastaID(cur.getFastaID());
		setSampleID(cur.getSampleID());
	}
}
