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
import org.semanticweb.owlapi.model.parameters.*;

import rekon.core.*;
import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
class AxiomConverter extends AxiomConversionComponent implements InputAxioms {

	final OWLDataFactory factory;
	final MappedNames names;
	final ExpressionConverter expressions;

	private Set<OWLObjectProperty> chainInvolvedSourceProperties
									= new HashSet<OWLObjectProperty>();

	private ClassAxiomConverter classExprAxioms;
	private NodePropertyAxiomConverter nodePropertyAxioms;
	private DataPropertyAxiomConverter dataPropertyAxioms;
	private IndividualAxiomConverter individualAxioms;

	private List<TypeAxiomResolver<?>> axiomTypeResolvers = new ArrayList<TypeAxiomResolver<?>>();
	private List<TypeAxiomConverter<?, ?>> axiomTypeConverters = new ArrayList<TypeAxiomConverter<?, ?>>();

	private Set<AxiomType<?>> outOfScopeTypes = new HashSet<AxiomType<?>>();

	private class MultiThreadConverter extends MultiThreadListProcessor<OWLAxiom> {

		protected void processElement(OWLAxiom ax, int threadIndex) {

			checkConvertResolved(ax, threadIndex);
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
		MappedNames names,
		ExpressionConverter expressions) {

		factory = manager.getOWLDataFactory();

		this.names = names;
		this.expressions = expressions;

		nodePropertyAxioms = new NodePropertyAxiomConverter(this);
		dataPropertyAxioms = new DataPropertyAxiomConverter(this);
		classExprAxioms = new ClassAxiomConverter(this);
		individualAxioms = new IndividualAxiomConverter(this);

		convertAll(manager);
	}

	void addTypeAxiomResolver(TypeAxiomResolver<?> resolver) {

		axiomTypeResolvers.add(resolver);
	}

	void addTypeAxiomConverter(TypeAxiomConverter<?, ?> converter) {

		axiomTypeConverters.add(converter);
	}

	<I extends InputAxiom,
	H extends TypeAxiomConverter<?, I>>
		List<I> getInputAxioms(Class<H> converterType) {

		for (TypeAxiomConverter<?, ?> h : axiomTypeConverters) {

			if (h.getClass() == converterType) {

				return converterType.cast(h).getInputAxioms();
			}
		}

		throw new Error("No converter of type: " + converterType);
	}

	private void convertAll(OWLOntologyManager manager) {

		for (OWLOntology ont : manager.getOntologies()) {

			new MultiThreadConverter(ont);
		}

		if (!outOfScopeTypes.isEmpty()) {

			WarningLogger.SINGLETON.logOutOfScopeAxiomTypes(outOfScopeTypes);
		}
	}

	protected void checkConvertResolved(OWLAxiom ax, int threadIndex) {

		if (!ignoreType(ax)) {

			for (OWLAxiom rax : resolve(ax)) {

				if (!checkConvert(rax, threadIndex)) {

					addOutOfScopeType(rax);
				}
			}
		}
	}

	private Collection<? extends OWLAxiom> resolve(OWLAxiom ax) {

		for (TypeAxiomResolver<?> r : axiomTypeResolvers) {

			Collection<? extends OWLAxiom> axs = r.checkResolve(ax);

			if (axs != null) {

				return axs;
			}
		}

		return Collections.singleton(ax);
	}

	private boolean checkConvert(OWLAxiom ax, int threadIndex) {

		for (TypeAxiomConverter<?, ?> c : axiomTypeConverters) {

			if (c.checkConvert(ax, threadIndex)) {

				return true;
			}
		}

		return false;
	}

	private boolean ignoreType(OWLAxiom ax) {

		return ax instanceof OWLDeclarationAxiom || ax instanceof OWLAnnotationAxiom;
	}

	private synchronized void addOutOfScopeType(OWLAxiom ax) {

		outOfScopeTypes.add(ax.getAxiomType());
	}
}
