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
package de.uni_koblenz.west.statistics.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.turtle.TurtleWriter;

/**
 * Writes compact turtle output with anonymous [] notation for BNode.
 * Beware: identical BNodes must occur in consecutive Statements. Otherwise,
 * the link between two statement groups with the same BNode is lost due to
 * the omission of the BNode ID.
 * 
 * @author Olaf Goerlitz
 */
public class CompactBNodeTurtleWriter extends TurtleWriter {
	
	protected BNode pendingBNodeObj;
	protected Deque<Resource> storedSubjects = new ArrayDeque<Resource>();
	protected Deque<URI> storedPredicates = new ArrayDeque<URI>();
	protected Deque<Value> storedBNodes = new ArrayDeque<Value>();
	protected Set<BNode> seenBNodes = new HashSet<BNode>();
	
	/**
	 * Creates a new TurtleWriter that will write to the supplied OutputStream.
	 * 
	 * @param out The OutputStream to write the Turtle document to.
	 */
	public CompactBNodeTurtleWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new TurtleWriter that will write to the supplied Writer.
	 * 
	 * @param writer The Writer to write the Turtle document to.
	 */
	public CompactBNodeTurtleWriter(Writer writer) {
		super(writer);
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Stores the current writer state before a new BNode block is opened.
	 */
	private void storeState() {
		storedBNodes.push(pendingBNodeObj);
		storedSubjects.push(lastWrittenSubject);
		storedPredicates.push(lastWrittenPredicate);
		lastWrittenSubject = pendingBNodeObj;
		lastWrittenPredicate = null;
	}

	/**
	 * Restores the last writer state before the BNode block was opened.
	 */
	private void restoreState() {
		storedBNodes.pop();
		lastWrittenSubject = storedSubjects.pop();
		lastWrittenPredicate = storedPredicates.pop();
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Handles a statement.
	 * 
	 * @param st The statement.
	 * @throws RDFHandlerException
	 *         If the RDF handler has encountered an unrecoverable error.
	 * @TODO check if the same BNode occurs more than once in object position
	 */
	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();

		try {
			
			// special handling of previous BNode object - try to use '[ ... ]'
			if (pendingBNodeObj != null) {
				// if current subject is the same as previous BNode object
				if (pendingBNodeObj.equals(subj)) {
					storeState();
				} else {
					closePreviousStatement();
				}
			}
			
			// check if previous BNode block is finished 
			if (storedBNodes.size() > 0 && !storedBNodes.peek().equals(subj)) {
				writer.writeEOL();
				writer.decreaseIndentation();
				writer.write("]");
				restoreState();
				statementClosed = false;
			}
			
			// check if subject and/or predicate were written before
			if (subj.equals(lastWrittenSubject)) {
				if (pred.equals(lastWrittenPredicate)) {
					// Identical subject and predicate
					writer.write(" , ");
				}
				else {
					// check if new BNode block was opened
					if (pendingBNodeObj != null) {
						pendingBNodeObj = null;
						writer.write("[");
						writer.writeEOL();
						writer.increaseIndentation();
					} else {
						// Identical subject, new predicate
						writer.write(" ;");
						writer.writeEOL();
					}

					// Write new predicate
					writePredicate(pred);
					writer.write(" ");
					lastWrittenPredicate = pred;
				}
			}
			else {
				// New subject
				closePreviousStatement();

				// Write new subject:
				writer.writeEOL();
				writeResource(subj);
				writer.write(" ");
				lastWrittenSubject = subj;

				// Write new predicate
				writePredicate(pred);
				writer.write(" ");
				lastWrittenPredicate = pred;

				statementClosed = false;
				writer.increaseIndentation();
			}

			// defer BNode object writing until next statement is checked
			if (obj instanceof BNode && storedBNodes.size() == 0) {
				pendingBNodeObj = (BNode) obj;
				// check if this BNode has been processed before
				if (seenBNodes.contains(pendingBNodeObj)) {
					throw new IllegalStateException("Same BNode may occur only once in object position: " + st);
				} else {
					seenBNodes.add(pendingBNodeObj);
				}
			} else {
				writeValue(obj);
			}
			
			// Don't close the line just yet. Maybe the next
			// statement has the same subject and/or predicate.
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}
	
	public void endRDF() throws RDFHandlerException {
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet started");
		}

		try {
			if (storedBNodes.size() > 0) {
				writer.writeEOL();
				writer.decreaseIndentation();
				writer.write("] .");
			} else {
				closePreviousStatement();				
			}
			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		finally {
			writingStarted = false;
		}
	}

}
