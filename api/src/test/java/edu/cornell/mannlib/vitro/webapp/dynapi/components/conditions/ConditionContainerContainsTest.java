/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.conditions;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.cornell.mannlib.vitro.webapp.dynapi.components.Parameter;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.Data;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.DataStore;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.TestView;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.conversion.InitializationException;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JacksonJsonContainer;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonArray;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonContainer;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonFactory;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.JsonObject;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.types.implementation.JsonContainerObjectParam;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.types.implementation.StringParam;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConditionContainerContainsTest {

    @org.junit.runners.Parameterized.Parameter(0)
    public List<String> keyValues;

    @org.junit.runners.Parameterized.Parameter(1)
    public Class<? extends JacksonJsonContainer> containerClass;

    @org.junit.runners.Parameterized.Parameter(2)
    public boolean result;

    @org.junit.runners.Parameterized.Parameter(3)
    public String testedKey;

    @Before
    public void before() {
        Logger.getLogger(JsonArray.class).setLevel(Level.OFF);
    }

    @After
    public void after() {
        Logger.getLogger(JsonArray.class).setLevel(Level.ERROR);
    }

    @Test
    public void test() throws InitializationException {
        ConditionContainerContains ccc = new ConditionContainerContains();
        String containerName = "container";
        Parameter container = new JsonContainerObjectParam(containerName);
        ccc.setContainer(container);
        String keyName = "key";
        Parameter keyParam = new StringParam(keyName);
        ccc.addInputParameter(keyParam);

        DataStore store = new DataStore();
        Data containerData = createContainer(container);
        store.addData(container.getName(), containerData);

        Data keyData = new Data(keyParam);
        TestView.setObject(keyData, testedKey);
        store.addData(keyParam.getName(), keyData);
        assertEquals(result, ccc.isSatisfied(store));
    }

    private Data createContainer(Parameter container) {
        Data containerData = new Data(container);
        JsonContainer containerObject = JsonFactory.getJson(containerClass);
        String expectedOutputParamName = "output";
        Parameter expectedOutputParam = new StringParam(expectedOutputParamName);
        Data data = new Data(expectedOutputParam);
        if (containerClass.equals(JsonArray.class)) {
            for (int i = 0; i <= Integer.parseInt(keyValues.iterator().next()); i++) {
                containerObject.addValue(data);
            }
        } else {
            for (String key : keyValues) {
                containerObject.addKeyValue(key, data);
            }
        }
        TestView.setObject(containerData, containerObject);
        return containerData;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> requests() {
        return Arrays.asList(new Object[][] {
                { Arrays.asList("0"), JsonArray.class, true, "0" },
                { Arrays.asList("0"), JsonArray.class, false, "1" },
                { Arrays.asList("2"), JsonArray.class, true, "2" },
                { Arrays.asList("2"), JsonArray.class, false, "u10" },

                { Arrays.asList("0"), JsonObject.class, true, "0" },
                { Arrays.asList("key"), JsonObject.class, true, "key" },
                { Arrays.asList("key with space"), JsonObject.class, true, "key with space" },
                { Arrays.asList("key$"), JsonObject.class, true, "key$" },
                { Arrays.asList("key\""), JsonObject.class, true, "key\"" },
                { Arrays.asList("key."), JsonObject.class, true, "key." },
                { Arrays.asList("key\n"), JsonObject.class, true, "key\n" },
                { Arrays.asList("key\t"), JsonObject.class, true, "key\t" },
                { Arrays.asList("key'"), JsonObject.class, true, "key'" },
                { Arrays.asList("key\\"), JsonObject.class, true, "key\\" }, });
    }
}
