package au.edu.unimelb.plantcell.jpa.dao;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="K49_SEQREF")
@Access(value=AccessType.PROPERTY)
public class k49_SeqRef {

	@Transient
	private SequenceReference sr;
	
	public k49_SeqRef() {
		sr = new SequenceReference();
	}
	
	public k49_SeqRef(final SequenceReference sr) {
		assert(sr != null);
		this.sr = sr;
	}
	
	@Id
	public int getID() {
		return sr.getID();
	}
	
	@Basic
	public int getLength() {
		return sr.getLength();
	}
	
	public void setLength(final int new_length) {
		sr.setLength(new_length);
	}
	
	@Basic
	public long getStart() {
		return sr.getStart();
	}
	
	public void setStart(final long new_start) {
		sr.setStart(new_start);
	}
	
	@Basic(optional=false)
	@Column(name="SEQ_ID")
	public String getSequenceID() {
		return sr.getSequenceID();
	}
	
	public void setSequenceID(final String new_id) {
		sr.setSequenceID(new_id);
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	public FastaFile getFastaFile() {
		return sr.getFastaFile();
	}
	
	public void setFastaFile(final FastaFile ff) {
		sr.setFastaFile(ff);
	}
}
