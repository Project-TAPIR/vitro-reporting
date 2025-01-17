/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.operations;

import edu.cornell.mannlib.vitro.webapp.dynapi.ResourceAPIPool;

public class ResourceAPIPoolAtomicOperation extends PoolAtomicOperation {

    public ResourceAPIPoolAtomicOperation() {
        this.pool = ResourceAPIPool.getInstance();
    }
}
