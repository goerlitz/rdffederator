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
package de.uni_koblenz.west.splendid.helpers;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sail.SailException;
import org.openrdf.sail.SailReadOnlyException;
import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.helpers.SailConnectionBase;
//import org.openrdf.store.StoreException;

/**
 * Prevents data updates by overriding all modifying methods
 * and throwing {@link SailReadOnlyException}s.
 * 
 * @author Olaf Goerlitz
 */
public abstract class ReadOnlySailConnection extends SailConnectionBase {
	
	// mandatory for Sesame 2 but obsolete in Sesame 3
	public ReadOnlySailConnection(SailBase sailBase) {
		// need sailBase reference for safely closing the connection
		super(sailBase);
	}
	
	@Override
//	public void addStatement(Resource subj, URI pred, Value obj, Resource... contexts) throws StoreException {
	protected void addStatementInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}

	@Override
//	public void clearNamespaces() throws StoreException {
	protected void clearNamespacesInternal() throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}

	@Override
//	public void removeNamespace(String prefix) throws StoreException {
	protected void removeNamespaceInternal(String prefix) throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}

	@Override
//	public void removeStatements(Resource subj, URI pred, Value obj, Resource... context) throws StoreException {
	protected void removeStatementsInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}

	@Override
//	public void setNamespace(String prefix, String name) throws StoreException {
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}

	// Sesame 2 only:
	
	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}
	
	@Override
	protected void commitInternal() throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}
	
	@Override
	protected void rollbackInternal() throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}
	
	@Override
	protected void startTransactionInternal() throws SailException {
		throw new SailReadOnlyException("Data updates are not supported.");
	}
}
