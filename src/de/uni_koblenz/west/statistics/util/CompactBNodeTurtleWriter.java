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
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.turtle.TurtleWriter;

import de.uni_koblenz.west.vocabulary.VOID2;

/**
 * Writes compact turtle output with anonymous [] notation for BNode.
 * Beware: identical BNodes must occur in consecutive Statements. Otherwise,
 * the link between two statement groups with the same BNode is lost due to
 * the omission of the BNode ID.
 * 
 * @author Olaf Goerlitz
 */
public class CompactBNodeTurtleWriter extends TurtleWriter {
	
	protected boolean newBNodeBlock = false;
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
	 * Special handling of previous BNode object - try to use '[ ... ]'
	 * Open [] block if current subject is the same BNode,
	 * else finish last triple.
	 * 
	 * @param subj the current subject
	 */
	private void handlePendingBNode(Resource subj) throws IOException {
		// special handling of previous BNode object - try to use '[ ... ]'
		if (pendingBNodeObj != null) {
			// if current subject is the same as previous BNode object
			if (pendingBNodeObj.equals(subj)) {
				storedBNodes.push(pendingBNodeObj);
				storedSubjects.push(lastWrittenSubject);
				storedPredicates.push(lastWrittenPredicate);
				lastWrittenSubject = pendingBNodeObj;
				lastWrittenPredicate = null;
				writer.write("[");
				writer.writeEOL();
				writer.increaseIndentation();
				newBNodeBlock = true;
			} else {
				closePreviousStatement();
			}
			pendingBNodeObj = null;
		}
	}
	
	private void handleActiveBNode(Resource subj) throws IOException {
		// special handling of previous BNode object - try to use '[ ... ]'
		if (storedBNodes.size() > 0) {
			// if currently a BNode block is active but not continued.
			if (!subj.equals(storedBNodes.peek())) { // close current block
				writer.writeEOL();
				writer.decreaseIndentation();
				writer.write("]");
				statementClosed = false;
				storedBNodes.pop();
				lastWrittenSubject = storedSubjects.pop();
				lastWrittenPredicate = storedPredicates.pop();
			}
		}
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
			
			// check for pending BNode:
			// i.e. last triple's object was a BNode and not written yet
			handlePendingBNode(subj);
			handleActiveBNode(subj);
			
			// check if subject and/or predicate were written before
			if (subj.equals(lastWrittenSubject)) {
				if (pred.equals(lastWrittenPredicate)) {
					// Identical subject and predicate
					writer.write(" , ");
				}
				else {
					// check if new BNode block was opened
					if (newBNodeBlock) {
						newBNodeBlock = false;
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
			if (obj instanceof BNode) {
				pendingBNodeObj = (BNode) obj;
				// check if this BNode has been processed before
				// if so the previous id was lost due to the shorthand notation
				// writing it again would result in two distinct bnodes
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
			// finish open statements and BNode blocks
			if (storedBNodes.size() == 0) {
				closePreviousStatement();
			} else {
				for (int i = 0; i < storedBNodes.size(); i++) {
					writer.writeEOL();
					writer.decreaseIndentation();
					writer.write("] .");					
				}
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

	static ValueFactory vf = ValueFactoryImpl.getInstance();
	
	public static void main(String[] args) {
		
		Writer write = new StringWriter();
		RDFWriter writer = new CompactBNodeTurtleWriter(write);
		
		BNode dataset = vf.createBNode();
		
		URI sparqlEndpoint = vf.createURI("http://localhost");
		Literal triples    = vf.createLiteral(String.valueOf(100000), XMLSchema.INTEGER);
		Literal properties = vf.createLiteral(String.valueOf(50), XMLSchema.INTEGER);
		
		try {
			writer.startRDF();
			
			// add namespaces which will be automatically shortened
			writer.handleNamespace("owl", "http://www.w3.org/2002/07/owl#");
			writer.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
			writer.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			writer.handleNamespace("void", "http://rdfs.org/ns/void#");

			// general void information
			writer.handleStatement(vf.createStatement(dataset, RDF.TYPE, toURI(VOID2.Dataset)));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.sparqlEndpoint), sparqlEndpoint));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.triples), triples));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.properties), properties));
			
			// write predicate statistics
			List<URI> predicates = new ArrayList<URI>(Arrays.asList(new URI[] {RDF.TYPE}));
			for (URI p : predicates) {
				BNode propPartition = vf.createBNode();
				Literal count = vf.createLiteral(String.valueOf(1000), XMLSchema.INTEGER);
				writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.propertyPartition), propPartition));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.property), p));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.triples), count));
				
				BNode histogram = vf.createBNode();
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.histogram), histogram));
				writer.handleStatement(vf.createStatement(histogram, RDF.TYPE, toURI(VOID2.EquiWidthHist)));
				writer.handleStatement(vf.createStatement(histogram, toURI(VOID2.buckets), vf.createLiteral(String.valueOf(10), XMLSchema.INTEGER)));
				for (int i = 0; i <3; i++) {
					BNode bucket = vf.createBNode();
					Literal value = vf.createLiteral(String.valueOf(i), XMLSchema.INTEGER);
					writer.handleStatement(vf.createStatement(histogram, toURI(VOID2.bucketDef), bucket));
					writer.handleStatement(vf.createStatement(bucket, toURI(VOID2.bucketLoad), value));
				}
			}
			writer.endRDF();
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}
		
		System.out.println(write.toString());
	}
	
	/**
	 * Converts an enum URI string to a Sesame URI.
	 * 
	 * @param uri the URI string to convert. 
	 * @return the converted Sesame URI.
	 */
	private static URI toURI(Enum<?> uri) {
		return vf.createURI(uri.toString());
	}

}
