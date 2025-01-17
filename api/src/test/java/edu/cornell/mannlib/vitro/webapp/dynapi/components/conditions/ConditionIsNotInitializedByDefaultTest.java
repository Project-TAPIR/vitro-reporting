/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.conditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.cornell.mannlib.vitro.webapp.dynapi.AbstractTest;
import edu.cornell.mannlib.vitro.webapp.dynapi.components.Parameter;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.Data;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.DataStore;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.types.implementation.StringParam;
import org.junit.Test;

public class ConditionIsNotInitializedByDefaultTest extends AbstractTest {

    @Test
    public void testConditionIsNotInitilaizedByDefault() {
        DataStore store = new DataStore();
        Parameter testParam = new StringParam("param name");
        Data testData = new Data(testParam);
        testData.setRawString("test value");
        store.addData(testParam.getName(), testData);
        Condition c = new ConditionIsNotInitializedByDefault();
        c.getInputParams().add(testParam);
        assertTrue(c.isSatisfied(store));
        testData.initializeDefault();
        assertFalse(c.isSatisfied(store));
    }
}
