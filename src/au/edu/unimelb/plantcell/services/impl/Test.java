package au.edu.unimelb.plantcell.services.impl;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Stateless
@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
public class Test {

	@GET
	@Path("all")
	@RolesAllowed("1kp_admin")
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
		run25Tests();
		run25STests();
		run39Tests();
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

	private void run39Tests() {
	}

	private void run25STests() {
	}

	private void run25Tests() {
	}
}
