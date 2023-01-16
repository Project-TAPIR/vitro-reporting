package edu.cornell.mannlib.vitro.webapp.dynapi.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.cornell.mannlib.vitro.webapp.dynapi.ActionPool;
import edu.cornell.mannlib.vitro.webapp.dynapi.Endpoint;
import edu.cornell.mannlib.vitro.webapp.dynapi.ResourceAPIPool;
import edu.cornell.mannlib.vitro.webapp.dynapi.ServletContextTest;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.Data;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.DataStore;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.TestView;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.conversion.ConversionException;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.conversion.InitializationException;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonContainer;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonContainer.Type;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.types.implementation.IntegerParam;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.types.implementation.JsonContainerObjectParam;
import edu.cornell.mannlib.vitro.webapp.utils.configuration.ConfigurationBeanLoaderException;

@RunWith(Parameterized.class)
public class LoopOperationIntegrationTest extends ServletContextTest {

	private static final String OUTPUT_CONTAINER = "output_container";
    private static final String RESOURCES_PATH = "src/test/resources/edu/cornell/mannlib/vitro/webapp/dynapi/components/";
	private static final String TEST_ACTION = RESOURCES_PATH + "loop-operation-test-action.n3";

	Model storeModel;

	@org.junit.runners.Parameterized.Parameter(0)
	public List<Integer> inputValues;
	
	@org.junit.runners.Parameterized.Parameter(1)
	public String expectedValues;


	@Before
	public void beforeEach() {
        Logger.getLogger(ResourceAPIPool.class).setLevel(Level.OFF);
        Logger.getLogger(ActionPool.class).setLevel(Level.OFF);
		storeModel = new OntModelImpl(OntModelSpec.OWL_MEM);
	}
	
    @After
    public void reset() {
        setup();
        ActionPool rpcPool = ActionPool.getInstance();
        rpcPool.init(servletContext);
        rpcPool.reload();
        assertEquals(0, rpcPool.count());
        Logger.getLogger(ResourceAPIPool.class).setLevel(Level.OFF);
        Logger.getLogger(ActionPool.class).setLevel(Level.OFF);
    }

    private ActionPool initWithDefaultModel() throws IOException {
        loadOntology(ontModel);
        ActionPool rpcPool = ActionPool.getInstance();
        rpcPool.init(servletContext);
        return rpcPool;
    }
    
    @Test
    public void test() throws ConfigurationBeanLoaderException, IOException, ConversionException, InitializationException {
        loadModel(ontModel, TEST_ACTION);
        ActionPool rpcPool = initWithDefaultModel();
        Action action = null;
        try { 
            action = rpcPool.getByUri("test:loop_action");
            assertFalse(action instanceof NullAction);
            assertTrue(action.isValid());
            DataStore store = new DataStore();
            addInputContainer(store);
            
            Endpoint.getDependencies(action, store, rpcPool);
            assertTrue(OperationResult.ok().equals(action.run(store))) ;
            assertTrue(store.contains(OUTPUT_CONTAINER));
            Data output = store.getData(OUTPUT_CONTAINER);
            assertEquals(expectedValues,output.getSerializedValue());
        } finally {
            if (action != null) {
                action.removeClient();    
            }
        }
    }

    private void addInputContainer(DataStore store) {
        String containerParamName = "input_container";
        Parameter container = new JsonContainerObjectParam(containerParamName);
        Data containerData = createContainer(container);
        store.addData(containerParamName, containerData);
    }
    
    private Data createContainer(Parameter containerParam) {
        Data containerData = new Data(containerParam);
        JsonContainer container = new JsonContainer(Type.ARRAY);
        String expectedOutputParamName = "item";
        Parameter expectedOutputParam = new IntegerParam(expectedOutputParamName);
        for (Integer value : inputValues) {
            addValue(container, expectedOutputParam, value);
        }
        TestView.setObject(containerData, container);
        return containerData;
    }

    private void addValue(JsonContainer container, Parameter expectedOutputParam, Integer value) {
        Data data = new Data(expectedOutputParam);
        TestView.setObject(data, value);
        container.addValue(data);
    }

	@Parameterized.Parameters
	public static Collection<Object[]> requests() {
		return Arrays.asList(new Object[][] {
		    {Arrays.asList(20,30,40,50,60), "[\"30\",\"40\",\"50\",\"60\",\"70\"]"},
		    {Arrays.asList(-100,-90,-80,-70,-60), "[\"-90\",\"-80\",\"-70\",\"-60\",\"-50\"]"},
		});
	}

	protected void loadModel(Model model, String... files) throws IOException {
		for (String file : files) {
			String rdf = readFile(file);
			model.read(new StringReader(rdf), null, "n3");
		}
	}

	public void loadOntology(OntModel ontModel) throws IOException {
        loadModel(ontModel, IMPLEMENTATION_FILE_PATH);
        loadModel(ontModel, ONTOLOGY_FILE_PATH);
        loadModel(ontModel, getFileList(ABOX_PREFIX));
	}
}