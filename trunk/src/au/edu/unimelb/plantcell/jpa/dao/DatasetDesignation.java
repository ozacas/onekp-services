package au.edu.unimelb.plantcell.jpa.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="DATASETDESIGNATION")		// needed for OpenJPA, but not for EclipseLink
public class DatasetDesignation {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	private String label;		// short name which complies with the regular expression: ^\w+$
	
	private String description; // plain text description
	
	
	public DatasetDesignation() {
	}
	
	public DatasetDesignation(String label, String description) {
		setLabel(label);
		setDescription(description);
	}
	
	public void setLabel(final String label) {
		this.label = label;
	}
	
	public void setDescription(final String descr) {
		this.description = descr;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDescription() {
		return description;
	}

	@Transient
	public String getSeqRefTable() throws Exception {
		String name = getLabel();
		if (name.equals("k25")) {
			return "K25_SEQREF";		// must match entity table name in corresponding DAO class
		} else if (name.equals("k25s")) {
			return "K25S_SEQREF";		// must match entity table name in corresponding DAO class
		} else if (name.equals("k39")) {
			return "K39_SEQREF";		// must match entity table name in corresponding DAO class
		} else if (name.equals("K49")) {
			return "K49_SEQREF";		// must match entity table name in corresponding DAO class
		} else if (name.equals("K59")) {
			return "K59_SEQREF";		// must match entity table name in corresponding DAO class
		} else if (name.equals("K69")) {
			return "K69_SEQREF";		// must match entity table name in corresponding DAO class
		} else {
			throw new Exception("Unknown and unsupported 1KP dataset designation: "+name);
		}
	}
}
