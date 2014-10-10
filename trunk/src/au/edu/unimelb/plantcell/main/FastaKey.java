package au.edu.unimelb.plantcell.main;


public class FastaKey {
	private String sample_id;
	private int fasta_id;
	
	public FastaKey(String sample_id, int fasta_id) {
		this.sample_id = sample_id;
		this.fasta_id  = fasta_id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FastaKey)) {
			return false;
		}
		FastaKey fk = (FastaKey) o;
		return (this.sample_id.equals(fk.sample_id) && this.fasta_id == fk.fasta_id); 
	}
	
	@Override
	public int hashCode() {
		return sample_id.hashCode() ^ fasta_id;
	}

	public boolean hasSampleID(String cur) {
		return this.sample_id.equals(cur);
	}

	public int getFastaID() {
		return this.fasta_id;
	}

	public boolean has(final String id, final int fasta_id) {
		return (hasSampleID(id) && getFastaID() == fasta_id);
	}

	public String getSampleID() {
		return this.sample_id;
	}
}
