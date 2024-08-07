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

/**
 * @author Colin Puleston
 */
class AxiomConverter extends AxiomConversionComponent implements InputAxioms {

	final OWLDataFactory factory;
	final MappedNames names;
	final ExpressionConverter expressions;

	private ClassExprAxiomConverter classExprAxioms;
	private PropertyAxiomConverter propertyAxioms;
	private IndividualAxiomConverter individualAxioms;

	private List<TypeAxiomResolver<?>> axiomTypeResolvers = new ArrayList<TypeAxiomResolver<?>>();
	private List<TypeAxiomConverter<?, ?>> axiomTypeConverters = new ArrayList<TypeAxiomConverter<?, ?>>();

	public Iterable<InputClassEquivalence> getClassEquivalences() {

		return classExprAxioms.getClassEquivalences();
	}

	public Iterable<InputClassComplexEquivalence> getClassComplexEquivalences() {

		return classExprAxioms.getClassComplexEquivalences();
	}

	public Iterable<InputComplexEquivalence> getComplexEquivalences() {

		return classExprAxioms.getComplexEquivalences();
	}

	public Iterable<InputClassSubSuper> getClassSubSupers() {

		return classExprAxioms.getClassSubSupers();
	}

	public Iterable<InputClassSubComplexSuper> getClassSubComplexSupers() {

		return classExprAxioms.getClassSubComplexSupers();
	}

	public Iterable<InputComplexSubClassSuper> getComplexSubClassSupers() {

		return classExprAxioms.getComplexSubClassSupers();
	}

	public Iterable<InputComplexSubSuper> getComplexSubSupers() {

		return classExprAxioms.getComplexSubSupers();
	}

	public Iterable<InputNodePropertyEquivalence> getNodePropertyEquivalences() {

		return propertyAxioms.getNodePropertyEquivalences();
	}

	public Iterable<InputNodePropertySubSuper> getNodePropertySubSupers() {

		return propertyAxioms.getNodePropertySubSupers();
	}

	public Iterable<InputNodePropertyDomain> getNodePropertyDomains() {

		return propertyAxioms.getNodePropertyDomains();
	}

	public Iterable<InputNodePropertyRange> getNodePropertyRanges() {

		return propertyAxioms.getNodePropertyRanges();
	}

	public Iterable<InputNodePropertyChain> getNodePropertyChains() {

		return propertyAxioms.getNodePropertyChains();
	}

	public Iterable<InputNodePropertyTransitive> getNodePropertyTransitives() {

		return propertyAxioms.getNodePropertyTransitives();
	}

	public Iterable<InputDataPropertyEquivalence> getDataPropertyEquivalences() {

		return propertyAxioms.getDataPropertyEquivalences();
	}

	public Iterable<InputDataPropertySubSuper> getDataPropertySubSupers() {

		return propertyAxioms.getDataPropertySubSupers();
	}

	public Iterable<InputIndividualEquivalence> getIndividualEquivalences() {

		return individualAxioms.getIndividualEquivalences();
	}

	public Iterable<InputIndividualClassType> getIndividualClassTypes() {

		return individualAxioms.getIndividualClassTypes();
	}

	public Iterable<InputIndividualComplexType> getIndividualComplexTypes() {

		return individualAxioms.getIndividualComplexTypes();
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

		classExprAxioms = new ClassExprAxiomConverter(this);
		propertyAxioms = new PropertyAxiomConverter(this);
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

				return converterType.cast(h).inputAxioms;
			}
		}

		throw new Error("No converter of type: " + converterType);
	}

	private void convertAll(OWLOntologyManager manager) {

		Set<AxiomType<?>> outOfScopeTypes = new HashSet<AxiomType<?>>();

		for (OWLOntology ont : manager.getOntologies()) {

			for (OWLAxiom ax : ont.getAxioms(Imports.EXCLUDED)) {

				for (OWLAxiom rax : resolve(ax)) {

					if (!ignoreAxiom(rax) && !convert(rax)) {

						outOfScopeTypes.add(rax.getAxiomType());
					}
				}
			}
		}

		if (!outOfScopeTypes.isEmpty()) {

			WarningLogger.SINGLETON.logOutOfScopeAxiomTypes(outOfScopeTypes);
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

	private boolean convert(OWLAxiom ax) {

		for (TypeAxiomConverter<?, ?> c : axiomTypeConverters) {

			if (c.convert(ax)) {

				return true;
			}
		}

		return false;
	}

	private boolean ignoreAxiom(OWLAxiom ax) {

		return ax instanceof OWLDeclarationAxiom || ax instanceof OWLAnnotationAxiom;
	}
}
