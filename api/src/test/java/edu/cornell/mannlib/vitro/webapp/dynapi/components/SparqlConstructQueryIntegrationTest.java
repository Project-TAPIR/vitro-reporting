package edu.cornell.mannlib.vitro.webapp.dynapi.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.cornell.mannlib.vitro.webapp.dynapi.ServletContextTest;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.Data;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.DataStore;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.TestView;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.conversion.ConversionException;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.ConfigurationBeanLoaderException;

@RunWith(Parameterized.class)
public class SparqlConstructQueryIntegrationTest extends ServletContextTest {
	private static final String OUTPUT = "output";
	private static final String QUERY = "query";
	private static final String URI = "uri";

	private static final String RESOURCES_PATH = "src/test/resources/edu/cornell/mannlib/vitro/webapp/dynapi/components/";
    private static final String TEST_ACTION = RESOURCES_PATH + "sparql-construct-test-action.n3";
    private static final String TEST_STORE = RESOURCES_PATH + "sparql-construct-test-store.n3";

    
    Model storeModel;
    
    @org.junit.runners.Parameterized.Parameter(0)
    public String uris;
    
    @org.junit.runners.Parameterized.Parameter(1)
    public String size;
    
    @Before
    public void beforeEach() {
        storeModel = new OntModelImpl(OntModelSpec.OWL_MEM);
    }
    
    @Test
    public void test() throws ConfigurationBeanLoaderException, IOException, ConversionException {
        loadOntology(ontModel);
        loadModel(ontModel, TEST_ACTION);
        loadModel(storeModel, TEST_STORE);
       // servletContext = new ServletContextStub();
        Action action = loader.loadInstance("test:action", Action.class);
        assertTrue(action.isValid());
        Parameters inputParameters = action.getInputParams();
        DataStore store = new DataStore();
        OntModelImpl outputModel = new OntModelImpl(OntModelSpec.OWL_DL_MEM);
        	
        final Parameters outputParams = action.getOutputParams();
		Parameter paramOutput = outputParams.get(OUTPUT);
        assertNotNull(paramOutput);
		Data outputData = new Data(paramOutput);
        TestView.setObject(outputData, outputModel);
        store.addData(OUTPUT, outputData);

        Parameter paramQuery = inputParameters.get(QUERY);
        assertNotNull(paramQuery);
		Data queryData = new Data(paramQuery);
        TestView.setObject(queryData, storeModel);
        store.addData(QUERY, queryData);
        
        final Parameter paramUri = inputParameters.get(URI);
        assertNotNull(paramUri);
		Data uriData = new Data(paramUri);
        store.addData(URI, uriData);

        for (String uri : uris.split(",")) {
        	TestView.setObject(uriData, uri);
            OperationResult opResult = action.run(store);
            assertFalse(opResult.hasError());	
        }
        assertEquals(Integer.parseInt(size), outputModel.size());
    }
    
    @Parameterized.Parameters
    public static Collection<Object[]> requests() {
        return Arrays.asList(new Object[][] {
        	{ "test:uri0", "0"},
        	{ "test:uri1", "1"},
        	{ "test:uri1,test:uri1", "1"},
        	{ "test:uri1,test:uri2", "2"},
            
        });
    }
    
    protected void loadModel(Model model, String... files) throws IOException {
        for (String file : files) {
            String rdf = readFile(file);
            model.read(new StringReader(rdf), null, "n3");
        }
    }
    
    public void loadOntology(OntModel ontModel) throws IOException {
        loadModel(ontModel, "../home/src/main/resources/rdf/tbox/filegraph/dynamic-api-implementation.n3");
        loadModel(ontModel, "../home/src/main/resources/rdf/abox/filegraph/dynamic-api-individuals.n3");
    }
}
