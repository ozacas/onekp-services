package au.edu.unimelb.plantcell.services.impl;

import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;

/**
 * A dummy implementation only for test cases... so that it can invoke the Queries instance using
 * this class as the constructor parameter.
 * 
 * @author acassin
 *
 */
public class DummyOneKPService extends OneKPSequenceService {
	private final static Logger logger = Logger.getLogger("DummyOneKPService");
	
	@Override
	public String getDataset() {
		return "dummy";
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public EntityManager getEntityManager() {
		return null;
	}

	@Override
	public void validateID(String id) throws IOException {
		// NO-OP
	}

	@Override
	public Response getProtein(String id) {
		return Response.ok().build();
	}

	@Override
	public Response getTranscript(String id) {
		return getProtein(id);
	}

	@Override
	public Response getAll(String id) {
		return getProtein(id);
	}

	@Override
	public Response getProteome(String id) {
		return getProtein(id);
	}

	@Override
	public Response getTranscriptome(String id) {
		return getProtein(id);
	}

	@Override
	public Response getSummary(String id) {
		return getProtein(id);
	}

}
