/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi.components.serialization;

import edu.cornell.mannlib.vitro.webapp.utils.configuration.Property;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ArraySerializationType extends SerializationType {

    private SerializationType elementsType = new PrimitiveSerializationType();

    public SerializationType getElementsType() {
        return elementsType;
    }

    @Property(uri = "https://vivoweb.org/ontology/vitro-dynamic-api#hasElementsOfType", maxOccurs = 1)
    public void setElementsType(SerializationType elementsType) {
        this.elementsType = elementsType;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ArraySerializationType)) {
            return false;
        }
        if (object == this) {
            return true;
        }
        ArraySerializationType compared = (ArraySerializationType) object;

        return new EqualsBuilder().append(getName(), compared.getName()).append(getElementsType(), compared
                .getElementsType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 105).append(getName()).append(getElementsType()).toHashCode();
    }
}
