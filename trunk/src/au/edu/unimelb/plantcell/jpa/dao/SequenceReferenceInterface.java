package au.edu.unimelb.plantcell.jpa.dao;


/**
 * All sequence reference classes for each dataset must implement this interface to support the Queries API
 * 
 * @author acassin
 *
 */
public interface SequenceReferenceInterface {

	public int getID();
	
	public int getLength();
	
	public void setLength(final int new_length);
	
	public long getStart();
	
	public void setStart(final long new_start);
	
	public String getSequenceID();
	
	public void setSequenceID(final String new_id);
	
	public FastaFile getFastaFile() ;
	
	public void setFastaFile(final FastaFile ff);
}
