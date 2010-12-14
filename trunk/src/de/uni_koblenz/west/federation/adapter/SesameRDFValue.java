/*
 * This file is part of RDF Federator.
 * Copyright 2010 Olaf Goerlitz
 * 
 * RDF Federator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * RDF Federator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with RDF Federator.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * RDF Federator uses libraries from the OpenRDF Sesame Project licensed 
 * under the Aduna BSD-style license. 
 */
package de.uni_koblenz.west.federation.adapter;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;

import de.uni_koblenz.west.statistics.RDFValue;

/**
 * Sesame wrapper for RDF values (URI/Literal) in triple patterns.
 *  
 * @author Olaf Goerlitz
 */
public class SesameRDFValue implements RDFValue {
	
	protected Value value;
	
	protected SesameRDFValue(Value value) {
		if (value == null)
			throw new IllegalArgumentException("value must not be null");
		this.value = value;
	}

	@Override
	public String getDataType() {
		if (this.value instanceof Literal) {
			URI uri = ((Literal) this.value).getDatatype();
			if (uri != null)
				return uri.stringValue();
		}
		return null;
	}

	@Override
	public boolean hasDataType() {
		if (this.value instanceof Literal) {
			URI uri = ((Literal) this.value).getDatatype();
			// also ignore string datatype
			if (uri != null && !XMLSchema.STRING.equals(uri))
				return true;
		}
		return false;
	}

	@Override
	public boolean isLiteral() {
		return this.value instanceof Literal;
	}

	@Override
	public boolean isURI() {
		return this.value instanceof URI;
	}

	@Override
	public String stringValue() {
		return value.stringValue();
	}
	
	@Override
	public String toString() {
		return stringValue() + " [" + getDataType() + "]";
	}

}
