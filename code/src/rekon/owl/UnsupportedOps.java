/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Colin Puleston
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package rekon.owl;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

/**
 * @author Colin Puleston
 */
class UnsupportedOps {

    public BufferingMode getBufferingMode() {

		throw new UnsupportedOperationException();
    }

	public List<OWLOntologyChange> getPendingChanges() {

		throw new UnsupportedOperationException();
	}

	public Set<OWLAxiom> getPendingAxiomAdditions() {

		throw new UnsupportedOperationException();
	}

	public Set<OWLAxiom> getPendingAxiomRemovals() {

		throw new UnsupportedOperationException();
	}

	public OWLOntology getRootOntology() {

		throw new UnsupportedOperationException();
	}

	public void interrupt() {

		throw new UnsupportedOperationException();
	}

	public boolean isPrecomputed(InferenceType inferenceType) {

		throw new UnsupportedOperationException();
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {

		throw new UnsupportedOperationException();
	}

	public boolean isConsistent() {

		throw new UnsupportedOperationException();
	}

	public boolean isSatisfiable(OWLClassExpression classExpression) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLClass> getUnsatisfiableClasses() {

		throw new UnsupportedOperationException();
	}

	public boolean isEntailed(Set<? extends OWLAxiom> axioms) {

		throw new UnsupportedOperationException();
	}

	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLClass> getTopClassNode() {

		throw new UnsupportedOperationException();
	}

	public Node<OWLClass> getBottomClassNode() {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {

		throw new UnsupportedOperationException();
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLDataProperty> getTopDataPropertyNode() {

		throw new UnsupportedOperationException();
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind, OWLObjectPropertyExpression pe) {

		throw new UnsupportedOperationException();
	}

	public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe) {

		throw new UnsupportedOperationException();
	}

	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) {

		throw new UnsupportedOperationException();
	}

	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind) {

		throw new UnsupportedOperationException();
	}

	public long getTimeOut() {

		throw new UnsupportedOperationException();
	}

	public FreshEntityPolicy getFreshEntityPolicy() {

		throw new UnsupportedOperationException();
	}

	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {

		throw new UnsupportedOperationException();
	}

	public void dispose() {

		throw new UnsupportedOperationException();
	}
}
