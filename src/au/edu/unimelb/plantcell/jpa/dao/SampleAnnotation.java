package au.edu.unimelb.plantcell.jpa.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 
 * @author acassin
 *
 */
@Entity
@Table(name="SAMPLEANNOTATION")
public class SampleAnnotation {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private int id;
	
	private String sample_id;		// always four characters
	private String sample_type;		// eg. single or pooled
	private String taxonomic_clade;	// eg. basal eudicots
	private String taxonomic_order;	
	private String taxonomic_family;
	private String species_name;
	
	private String tissue_type;		// optional
	
	private String classification;	// PRIMARY or redundant sample
	
	public String getSampleID() {
		return sample_id;
	}
	
	public void setSampleID(String new_id) {
		this.sample_id = new_id;
	}
	
	public String getSampleType() {
		return sample_type;
	}
	
	public void setSampleType(String new_type) {
		this.sample_type = new_type;
	}
	
	public String getClade() {
		return taxonomic_clade;
	}
	
	public void setClade(String new_clade) {
		this.taxonomic_clade = new_clade;
	}
	
	public String getOrder() {
		return taxonomic_order;
	}
	
	public void setOrder(String new_order) {
		this.taxonomic_order = new_order;
	}
	
	public String getFamily() {
		return taxonomic_family;
	}
	
	public void setFamily(String new_family) {
		this.taxonomic_family = new_family;
	}
	
	public String getSpecies() {
		return species_name;
	}
	
	public void setSpecies(String species_name) {
		this.species_name = species_name;
	}
	
	public String getClassification() {
		return classification;
	}
	
	public void setClassification(String new_classification) {
		this.classification = new_classification;
	}
	
	public String getTissueType() {
		return this.tissue_type;
	}
	
	public void setTissueType(String new_tt) {
		this.tissue_type = new_tt;
	}
}

