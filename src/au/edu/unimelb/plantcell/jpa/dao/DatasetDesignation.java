package au.edu.unimelb.plantcell.jpa.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
}
