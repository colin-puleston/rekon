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

package rekon.owl.convert;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.*;

import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
class AxiomConverter implements InputAxioms {

	final OWLDataFactory factory;
	final NameMapper names;
	final ExpressionConverter expressions;

	private ClassAxiomConverter classExprAxioms;
	private NodePropertyAxiomConverter nodePropertyAxioms;
	private DataPropertyAxiomConverter dataPropertyAxioms;
	private IndividualAxiomConverter individualAxioms;

	private List<TypeAxiomConverter<?, ?>> typeAxiomConverters = new ArrayList<TypeAxiomConverter<?, ?>>();

	private Set<AxiomType<?>> outOfScopeTypes = new HashSet<AxiomType<?>>();

	private class MultiThreadConverter extends MultiThreadListProcessor<OWLAxiom> {

		protected void processElement(OWLAxiom ax, int threadIndex) {

			checkAddToTypeConverter(ax, threadIndex);
		}

		MultiThreadConverter(OWLOntology ontology) {

			invokeListProcesses(ontology.getAxioms(Imports.EXCLUDED));
		}
	}

	public Iterable<InputClassEquivalence> getClassEquivalences() {

		return classExprAxioms.getClassEquivalences();
	}

	public Iterable<InputClassSubSuper> getClassSubSupers() {

		return classExprAxioms.getClassSubSupers();
	}

	public Iterable<InputNodePropertyEquivalence> getNodePropertyEquivalences() {

		return nodePropertyAxioms.getEquivalences();
	}

	public Iterable<InputNodePropertySubSuper> getNodePropertySubSupers() {

		return nodePropertyAxioms.getSubSupers();
	}

	public Iterable<InputNodePropertyInverse> getNodePropertyInverses() {

		return nodePropertyAxioms.getInverses();
	}

	public Iterable<InputNodePropertyChain> getNodePropertyChains() {

		return nodePropertyAxioms.getChains();
	}

	public Iterable<InputNodePropertySymmetric> getNodePropertySymmetrics() {

		return nodePropertyAxioms.getSymmetrics();
	}

	public Iterable<InputNodePropertyTransitive> getNodePropertyTransitives() {

		return nodePropertyAxioms.getTransitives();
	}

	public Iterable<InputDataPropertyEquivalence> getDataPropertyEquivalences() {

		return dataPropertyAxioms.getEquivalences();
	}

	public Iterable<InputDataPropertySubSuper> getDataPropertySubSupers() {

		return dataPropertyAxioms.getSubSupers();
	}

	public Iterable<InputIndividualEquivalence> getIndividualEquivalences() {

		return individualAxioms.getIndividualEquivalences();
	}

	public Iterable<InputIndividualType> getIndividualTypes() {

		return individualAxioms.getIndividualTypes();
	}

	public Iterable<InputIndividualRelation> getIndividualRelations() {

		return individualAxioms.getIndividualRelations();
	}

	AxiomConverter(
		OWLOntologyManager manager,
		NameMapper names,
		ExpressionConverter expressions) {

		factory = manager.getOWLDataFactory();

		this.names = names;
		this.expressions = expressions;

		nodePropertyAxioms = new NodePropertyAxiomConverter(this);
		dataPropertyAxioms = new DataPropertyAxiomConverter(this);
		classExprAxioms = new ClassAxiomConverter(this);
		individualAxioms = new IndividualAxiomConverter(this);

		initialiseTypeConverters(manager);
	}

	void addTypeAxiomConverter(TypeAxiomConverter<?, ?> converter) {

		typeAxiomConverters.add(converter);
	}

	private void initialiseTypeConverters(OWLOntologyManager manager) {

		for (OWLOntology ont : manager.getOntologies()) {

			new MultiThreadConverter(ont);
		}

		if (!outOfScopeTypes.isEmpty()) {

			WarningLogger.SINGLETON.logOutOfScopeAxiomTypes(outOfScopeTypes);
		}
	}

	protected void checkAddToTypeConverter(OWLAxiom ax, int threadIndex) {

		if (!ignoreAxiomType(ax) && !addToTypeConverter(ax, threadIndex)) {

			outOfScopeTypes.add(ax.getAxiomType());
		}
	}

	private boolean addToTypeConverter(OWLAxiom ax, int threadIndex) {

		for (TypeAxiomConverter<?, ?> c : typeAxiomConverters) {

			if (c.addAxiom(ax, threadIndex)) {

				return true;
			}
		}

		return false;
	}

	private boolean ignoreAxiomType(OWLAxiom ax) {

		return ax instanceof OWLDeclarationAxiom || ax instanceof OWLAnnotationAxiom;
	}
}
