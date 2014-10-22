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
@Table(name="K59_SEQREF")
@Access(value=AccessType.PROPERTY)
public class k59_SeqRef implements SequenceReferenceInterface {

	@Transient
	private SequenceReference sr;
	
	public k59_SeqRef() {
		sr = new SequenceReference();
	}
	
	public k59_SeqRef(final SequenceReference sr) {
		assert(sr != null);
		this.sr = sr;
	}
	
	@Id
	@Override
	public int getID() {
		return sr.getID();
	}
	
	@Basic
	@Override
	public int getLength() {
		return sr.getLength();
	}
	
	@Override
	public void setLength(final int new_length) {
		sr.setLength(new_length);
	}
	
	@Basic
	@Override
	public long getStart() {
		return sr.getStart();
	}
	
	@Override
	public void setStart(final long new_start) {
		sr.setStart(new_start);
	}
	
	@Basic(optional=false)
	@Column(name="SEQ_ID")
	@Override
	public String getSequenceID() {
		return sr.getSequenceID();
	}
	
	@Override
	public void setSequenceID(final String new_id) {
		sr.setSequenceID(new_id);
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@Override
	public FastaFile getFastaFile() {
		return sr.getFastaFile();
	}
	
	@Override
	public void setFastaFile(final FastaFile ff) {
		sr.setFastaFile(ff);
	}
}
