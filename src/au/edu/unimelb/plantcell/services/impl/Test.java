package au.edu.unimelb.plantcell.services.impl;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;
import au.edu.unimelb.plantcell.jpa.dao.SequenceType;
import au.edu.unimelb.plantcell.seqdb.QA;

@Stateless
@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
public class Test {

	@GET
	@Path("all")
	//@RolesAllowed("1kp_admin")
	public String doAllTests() {
		try {
			runTests();
			return "PASS";
		} catch (Exception e) {
			e.printStackTrace();
			return "FAIL: "+e.getMessage();
		}
	}
	
	private void runTests() throws Exception {
		//QA qa = new QA();	// used for debugging only
		QA qa = new QA("http://localhost/onekp/");		
		run25Tests(qa);
		run25STests();
		run39Tests(qa);
		run49Tests();
		run59Tests();
		run69Tests();
	}

	private void run69Tests() {
	}

	private void run59Tests() {
	}

	private void run49Tests() {
	}

	private void run39Tests(final QA qa) throws Exception {
		DatasetDesignation dsd = new DatasetDesignation("k39", "");
		// poor sample, few transcripts
		String id = "FANS_Locus_97_Transcript_9/10_Confidence_0.625_Length_611_1";
		if (!qa.testSequence(dsd, id, SequenceType.AA, "MGSSGINAEYFFFFFFFFLFFLNDKKPFYYITEFSQSTAHFIYACIDIRASALDPVEHLLLQIQLVIVCW")) {
			throw new Exception("FAILED: k39 "+id+" protein sequence test");
		}
		if (!qa.testSequenceEnds(dsd, id, SequenceType.RNA, 
"CAGAGTACATGGGAAGCAGTGGTATCAACGCAGAGTACTTTTTTTTTTTTTTT",
"GCTTGTAGTCGTGGTGATCCTTGTCGTGTTTATGCTCGTGGTCGTTGGTGATCCTTGTCGTGTTTGTGCTC")) {
			throw new Exception("FAILED: k39 "+id+" transcript sequence test");
		}
		
	}

	private void run25STests() {
	}

	private void run25Tests(final QA qa) throws Exception {
		DatasetDesignation dsd = new DatasetDesignation("k25", "");
		
		if (!qa.testSequence(dsd, "ABCD_1", SequenceType.AA, 
				"LPQCPVEKPCLQWVQIYLLDCVCSTWDRFSLALGLTSVVCWGVTEVPQILTNFREKSTEGVSLLFLMTWVVGDVFNLLGCYLEPATLPTQFYMAILYTMTTIVLVLQTIYYDHFSRWRAGDQVEAEEFFPEV")) {
			throw new Exception("FAILED: k25 ABCD_1 protein sequence test");
		}
		if (!qa.testSequence(dsd, "ABCD_78577", SequenceType.AA, "FGDGAVALSPFGVSGCRSLPERIGNGHCRKVFGVSEDLEEADVAVEGVGEDESVVNSRGQGFILSILSNSVTAGVLALTVAAVAVVSGSRRPGFVATAAEVGQVCVLNEERVSSPNEILTPFSAFGNEINGDSHLGKETPPGRTKSSRSLFFASTALSANEGLTKLENKKIDEASQEFVEWLDNVKRDAHSEGVSEGVLEWALEDLTVSKDSVVKRKTMPEVKLILDDYLKRMVDERRISIGASRLKENSELLTEVSQKFGVPVQFLVAIWGIETNYGTYMGNWDIIQSLATMGFTEELAARRNYFRGELLEALLMLEKGTAEQGTGPQGQKRLRGSWAGAMGQCQFMPSSFRDYAIDFDGDGRKDIWESKPDVFASIANYFKEHGWKEGGPIFQKVQVPRKLDKSLVGSGVVKSVLDWEHKHGIHLNPGKAPALPPDAMASLLMPNGPKGDAYLVCDNFRVILRYNSSDLYGFAVCELARLIQERSSNPFPTPHL")) {
			throw new Exception("FAILED: k25 ABCD_78577 protein sequence test");
		}
		if (!qa.testSequence(dsd, "ABCD_78338", SequenceType.RNA, "TCGGCAGATCTCGGTCCTCGGGCGCAAGTGATGAAGAAGACGGCTGAGAAACGTATTGGAGTGCCGCGAACGAAGTTGTACAGGGGAGTTCGGCAGAGGCACTGGGGTAAGTGGGTGGCGGAGATCCGGTTGCCTAGGAACCGGACCCGGCTGTGGCTTGGAACATTCGATACCGCAGAGGAGGCAGCACTGGCCTACGATACGGCGGCATACAAACTGAGGGGCGAGTACGCGCGTCTCAACTTCCCTCGTCAGCAAGGCGACCAGGGGGTATTATCTGGTGGAGCACAGTCATCCGTGGAT")) {
			throw new Exception("FAILED: k25 ABCD_78338 transcript sequence test");
		}
		if (!qa.testSequence(dsd, "ABCD_78338", SequenceType.AA, "SADLGPRAQVMKKTAEKRIGVPRTKLYRGVRQRHWGKWVAEIRLPRNRTRLWLGTFDTAEEAALAYDTAAYKLRGEYARLNFPRQQGDQGVLSGGAQSSVD")) {
			throw new Exception("FAILED: k25 ABCD_78338 protein sequence test");
		}
	}
}
