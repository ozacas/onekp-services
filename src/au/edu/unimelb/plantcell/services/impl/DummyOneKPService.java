package au.edu.unimelb.plantcell.services.impl;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.ws.rs.core.Response;

import au.edu.unimelb.plantcell.jpa.dao.DatasetDesignation;

/**
 * A dummy implementation only for test cases... so that it can invoke the Queries instance using
 * this class as the constructor parameter.
 * 
 * @author acassin
 *
 */
@Stateless
public class DummyOneKPService extends OneKPSequenceService {
	private final static Logger logger = Logger.getLogger("DummyOneKPService");
	
	@PersistenceUnit(unitName="seqdb_onekp")			// must match persistence.xml entry
	private EntityManagerFactory emf;
	
	private static EntityManager seqdb_onekp = null;
	
	@Override
	public DatasetDesignation getDesignation() {
		return new DatasetDesignation("k25", "");
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public EntityManager getEntityManager() {
		synchronized (emf) {
			if (seqdb_onekp == null) {
				seqdb_onekp = emf.createEntityManager();
			}
			return seqdb_onekp;
		}
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
