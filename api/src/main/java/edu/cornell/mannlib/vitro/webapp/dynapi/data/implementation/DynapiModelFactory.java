/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation;

import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Model;

public class DynapiModelFactory {

    private static final Log log = LogFactory.getLog(DynapiModelFactory.class);

    private static DynapiModelFactory INSTANCE = new DynapiModelFactory();

    public static DynapiModelFactory getInstance() {
        return INSTANCE;
    }

    public static Model getModel(String uri) {
        Model model = ModelAccess.getInstance().getOntModel(uri);
        if (model == null) {
            log.error(String.format("ModelAccess provided null on request for model with uri '%s' ", uri));
            throw new RuntimeException();
        }
        return model;
    }
}
