/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.utils.configuration;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import edu.cornell.mannlib.vitro.webapp.utils.jena.criticalsection.LockableModel;
import edu.cornell.mannlib.vitro.webapp.utils.jena.criticalsection.LockedModel;

/**
 * Load one or more Configuration beans from a specified model.
 */
public class ConfigurationBeanLoader {
	
 	private static final String FIND_URI_QUERY = "" +
 	        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
 	        "PREFIX dynapi: <https://vivoweb.org/ontology/vitro-dynamic-api#>\n" + 
 	        "SELECT ?uri\n" + 
 	        "WHERE {\n" + 
 	        "  {\n" + 
 	        "    ?uri a ?type .\n" + 
 	        "    ?type dynapi:implementedBy ?java .\n" + 
 	        "  }\n" + 
 	        "  UNION\n" + 
 	        "  {\n" + 
 	        "    ?uri a ?java .\n" + 
 	        "  }\n" + 
 	        "} ";


    private static final Log log = LogFactory.getLog(ConfigurationBeanLoader.class);


	private static final String JAVA_URI_PREFIX = "java:";

  Map<String, Object> instancesMap = new HashMap<String,Object>();

	
	// ----------------------------------------------------------------------
	// utility methods
	// ----------------------------------------------------------------------

	public static String toJavaUri(Class<?> clazz) {
		return JAVA_URI_PREFIX + clazz.getName();
	}

	public static String toCanonicalJavaUri(String uri) {
		return uri.replace("#", ".");
	}

	public static boolean isJavaUri(String uri) {
		return uri.startsWith(JAVA_URI_PREFIX);
	}

	public static Set<String> toPossibleJavaUris(Class<?> clazz) {
		Set<String> set = new TreeSet<>();
		String[] uriPieces = toJavaUri(clazz).split("\\.");
		for (int hashIndex = 0; hashIndex < uriPieces.length; hashIndex++) {
			set.add(joinWithPeriodsAndAHash(uriPieces, hashIndex));
		}
		return set;
	}

	private static String joinWithPeriodsAndAHash(String[] pieces,
			int hashIndex) {
		StringBuilder buffer = new StringBuilder(pieces[0]);
		for (int i = 1; i < pieces.length; i++) {
			buffer.append(i == hashIndex ? '#' : '.').append(pieces[i]);
		}
		return buffer.toString();
	}

	public static String classnameFromJavaUri(String uri) {
		if (!isJavaUri(uri)) {
			throw new IllegalArgumentException(
					"Not a java class URI: '" + uri + "'");
		}
		return toCanonicalJavaUri(uri).substring(JAVA_URI_PREFIX.length());
	}

	public static boolean isMatchingJavaUri(String uri1, String uri2) {
		return toCanonicalJavaUri(uri1).equals(toCanonicalJavaUri(uri2));
	}

	// ----------------------------------------------------------------------
	// the instance
	// ----------------------------------------------------------------------

	/** Must not be null. */
	private final LockableModel locking;

	/**
	 * May be null, but the loader will be unable to satisfy instances of
	 * RequestModelUser.
	 */
	private final HttpServletRequest req;

	public ConfigurationBeanLoader(Model model) {
		this(new LockableModel(model), null);
	}

	public ConfigurationBeanLoader(LockableModel locking) {
		this(locking, null);
	}

	public ConfigurationBeanLoader(Model model, HttpServletRequest req) {
		this(new LockableModel(model), req);
	}

    public ConfigurationBeanLoader(LockableModel locking, HttpServletRequest req) {
        this.locking = Objects.requireNonNull(locking, "locking may not be null.");
        this.req = req;
    }

	/**
	 * Load the instance with this URI, if it is assignable to this class.
	 */
	public <T> T loadInstance(String uri, Class<T> resultClass) throws ConfigurationBeanLoaderException {
		instancesMap.clear();
		T result = loadSubordinateInstance(uri, resultClass);
		instancesMap.clear();
		return result;
	}

	protected <T> T loadSubordinateInstance(String uri, Class<T> resultClass)
			throws ConfigurationBeanLoaderException {
		if (uri == null) {
			throw new NullPointerException("uri may not be null.");
		}
		if (resultClass == null) {
			throw new NullPointerException("resultClass may not be null.");
		}
		if (instancesMap.containsKey(uri)) {
			try {
				T t = (T) instancesMap.get(uri);
				return t;
		  } catch (ClassCastException e) {
		    throw new ConfigurationBeanLoaderException(uri, e);
		  }
		}
		try {
			ConfigurationRdf<T> parsedRdf = ConfigurationRdfParser
					.parse(locking, uri, resultClass);
			WrappedInstance<T> wrapper = InstanceWrapper
					.wrap(parsedRdf.getConcreteClass());
			wrapper.satisfyInterfaces(req);
			wrapper.checkCardinality(parsedRdf.getPropertyStatements());
			instancesMap.put(uri, wrapper.getInstance());
			wrapper.setProperties(this, parsedRdf.getPropertyStatements());
			wrapper.validate();
			return wrapper.getInstance();
		} catch (Exception e) {
			throw new ConfigurationBeanLoaderException(
					"Failed to load '" + uri + "'", e);
		}
	}

	/**
	 * Find all of the resources with the specified class, and instantiate them.
	 */
	public <T> Set<T> loadAll(Class<T> resultClass)
			throws ConfigurationBeanLoaderException {
		Set<String> uris = new HashSet<>();
		findUris(resultClass, uris);
		Set<T> instances = new HashSet<>();
		for (String uri : uris) {
			instances.add(loadInstance(uri, resultClass));
		}
		return instances;
	}
	
	/**
	 * Find all of the resources with the specified class, and instantiate them.
	 */
	public <T> Map<String, T> loadEach(Class<T> resultClass){
		Set<String> uris = new HashSet<>();
		findUris(resultClass, uris);
		HashMap<String,T> instances = new HashMap<>();
		for (String uri : uris) {
			try {
				instances.put(uri, loadInstance(uri, resultClass));
			} catch (ConfigurationBeanLoaderException e) {
				log.error(e, e);
			}
		}
		return instances;
	}

	private <T> void findUris(Class<T> resultClass, Set<String> uris) {
		try (LockedModel m = locking.read()) {
          for (String typeUri : toPossibleJavaUris(resultClass)) {
                ParameterizedSparqlString pss = new ParameterizedSparqlString(FIND_URI_QUERY);
                pss.setIri("java", typeUri);
                Query query = QueryFactory.create(pss.toString());
                QueryExecution qexec = QueryExecutionFactory.create(query, m);
                try {
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution solution = results.nextSolution();
                        uris.add(solution.getResource("uri").getURI());
                    }
                } finally {
                    qexec.close();
                }
			}
		}
	}
}
