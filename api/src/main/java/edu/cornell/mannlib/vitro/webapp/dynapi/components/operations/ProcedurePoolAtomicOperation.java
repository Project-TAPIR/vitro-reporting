/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.operations;

import edu.cornell.mannlib.vitro.webapp.dynapi.ProcedurePool;

public class ProcedurePoolAtomicOperation extends PoolAtomicOperation {

    public ProcedurePoolAtomicOperation() {
        this.pool = ProcedurePool.getInstance();
    }
}
