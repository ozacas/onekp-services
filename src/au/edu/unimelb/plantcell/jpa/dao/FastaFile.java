package au.edu.unimelb.plantcell.jpa.dao;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * 
 * @author acassin
 *
 */
@Entity
@Table(name="FASTAFILE")
public class FastaFile {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	@OneToOne
	private DatasetDesignation dsd;
	
    private String path;		        // where is the fasta file?
    private String onekp_sample_id;		// 1kp sample ID for the fasta file
    @Enumerated(EnumType.STRING)
    private SequenceType sequence_type; // each file consists of sequences of a SINGLE type: AA, RNA, DNA or unknown
	
	public FastaFile() {
	
	}
	
	public DatasetDesignation getDesignation() {
		return dsd;
	}
	
	public void setDesignation(final DatasetDesignation dsd) {
		this.dsd = dsd;
	}
	
	public FastaFile(final File path) {
		setPathAndSampleID(path);
	}
	
	public void setFile(final File path) {
		setPath(path.getAbsolutePath());
	}
	
	public void setPath(final String path) {
		this.path = path;
	}
	
	public void setPathAndSampleID(final File f) {
		setPath(f.getAbsolutePath());
		String name = f.getName();
		Pattern p = Pattern.compile("(\\b[A-Z]{4}\\b)");
		Matcher m = p.matcher(name);
		if (m.find()) {
			setSampleID(m.group(1));
		} else {
			setSampleID("");
		}
	}
	
	public String getSampleID() {
		return onekp_sample_id;
	}
	
	public void setSampleID(String onekp_sample_id) {
		if (onekp_sample_id == null) {
			onekp_sample_id = "";
		}
		this.onekp_sample_id = onekp_sample_id;
	}

	public String getPath() {
		return path;
	}
	
	public int getID() {
		return id;
	}
	
	public void setSequenceType(SequenceType new_st) {
		if (new_st == null) {
			new_st = SequenceType.AA;
		}
		this.sequence_type = new_st;
	}
	
	public SequenceType getSequenceType() {
		return sequence_type;
	}
}
