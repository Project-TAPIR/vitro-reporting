/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.operations;

import edu.cornell.mannlib.vitro.webapp.dynapi.RPCPool;

public class RPCPoolBulkOperation extends PoolBulkOperation {

    public RPCPoolBulkOperation() {
        this.pool = RPCPool.getInstance();
    }
}
