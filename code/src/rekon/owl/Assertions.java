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
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * @author Colin Puleston
 */
class Assertions {

	final OWLClass owlThing;
	final OWLObjectProperty owlTopObjectProperty;
	final OWLDataProperty owlTopDataProperty;

	private Set<OWLOntology> allOntologies;
	private OWLDataFactory factory;

	Assertions(OWLOntologyManager manager) {

		allOntologies = manager.getOntologies();
		factory = manager.getOWLDataFactory();

		owlThing = factory.getOWLThing();
		owlTopObjectProperty = factory.getOWLTopObjectProperty();
		owlTopDataProperty = factory.getOWLTopDataProperty();
	}

	<T extends OWLAxiom>Collection<T> getAxioms(AxiomType<T> axiomType) {

		Set<T> all = new HashSet<T>();

		for (OWLOntology ont : allOntologies) {

			all.addAll(ont.getAxioms(axiomType, Imports.EXCLUDED));
		}

		return all;
	}

	Collection<OWLClass> getAllClasses() {

		Set<OWLClass> all = new HashSet<OWLClass>();

		all.add(owlThing);

		for (OWLOntology ont : allOntologies) {

			all.addAll(ont.getClassesInSignature());
		}

		return all;
	}

	Collection<OWLObjectProperty> getAllObjectProperties() {

		Set<OWLObjectProperty> all = new HashSet<OWLObjectProperty>();

		all.add(owlTopObjectProperty);

		for (OWLOntology ont : allOntologies) {

			all.addAll(ont.getObjectPropertiesInSignature());
		}

		return all;
	}

	Collection<OWLDataProperty> getAllDataProperties() {

		Set<OWLDataProperty> all = new HashSet<OWLDataProperty>();

		all.add(owlTopDataProperty);

		for (OWLOntology ont : allOntologies) {

			all.addAll(ont.getDataPropertiesInSignature());
		}

		return all;
	}

	Collection<OWLNamedIndividual> getAllIndividuals() {

		Set<OWLNamedIndividual> all = new HashSet<OWLNamedIndividual>();

		for (OWLOntology ont : allOntologies) {

			all.addAll(ont.getIndividualsInSignature());
		}

		return all;
	}

	Collection<OWLClass> getEquivalentClasses(OWLClass entity) {

		return filterForType(getEquivalentExprs(entity), OWLClass.class);
	}

	Collection<OWLClassExpression> getEquivalentExprs(OWLClass entity) {

		Collection<OWLClassExpression> equivs = getEquivalentsGroup(entity);

		equivs.remove(entity);

		return equivs;
	}

	Collection<OWLObjectProperty> getEquivalentProperties(OWLObjectProperty entity) {

		Collection<OWLObjectPropertyExpression> equivs = getEquivalentsGroup(entity);

		equivs.remove(entity);

		return filterForType(equivs, OWLObjectProperty.class);
	}

	Collection<OWLDataProperty> getEquivalentProperties(OWLDataProperty entity) {

		Collection<OWLDataPropertyExpression> equivs = getEquivalentsGroup(entity);

		equivs.remove(entity);

		return filterForType(equivs, OWLDataProperty.class);
	}

	Collection<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual entity) {

		Collection<OWLIndividual> equivs = getSameIndividualsGroup(entity);

		equivs.remove(entity);

		return filterForType(equivs, OWLNamedIndividual.class);
	}

	Collection<OWLClass> getSuperClasses(OWLClass entity) {

		Collection<OWLClassExpression> supers = getSuperExprs(entity);

		resolveIntersections(getEquivalentExprs(entity), supers, true);

		return filterForType(supers, OWLClass.class);
	}

	Collection<OWLClassExpression> getSuperExprs(OWLClass entity) {

		Set<OWLClassExpression> supers = new HashSet<OWLClassExpression>();

		resolveIntersections(getRawSuperExprs(entity), supers, false);

		return supers;
	}

	Collection<OWLObjectProperty> getSuperProperties(OWLObjectProperty entity) {

		return filterForType(getSuperExprs(entity), OWLObjectProperty.class);
	}

	Collection<OWLDataProperty> getSuperProperties(OWLDataProperty entity) {

		return filterForType(getSuperExprs(entity), OWLDataProperty.class);
	}

	boolean isTransitive(OWLObjectProperty entity) {

		return EntitySearcher.isTransitive(entity, allOntologies);
	}

	Collection<OWLClass> getTypes(OWLNamedIndividual entity) {

		return filterForType(getTypeExprs(entity), OWLClass.class);
	}

	Collection<OWLClassExpression> getTypeExprs(OWLNamedIndividual entity) {

		return EntitySearcher.getTypes(entity, allOntologies);
	}

	Collection<ObjectValueAssertion> getObjectValues(OWLNamedIndividual entity) {

		return new ObjectValueAssertions(entity, allOntologies).getAll();
	}

	Collection<DataValueAssertion> getDataValues(OWLNamedIndividual entity) {

		return new DataValueAssertions(entity, allOntologies).getAll();
	}

	private Collection<OWLClassExpression> getRawSuperExprs(OWLClass entity) {

		return EntitySearcher.getSuperClasses(entity, allOntologies);
	}

	private Collection<OWLObjectPropertyExpression> getSuperExprs(OWLObjectProperty entity) {

		return EntitySearcher.getSuperProperties(entity, allOntologies);
	}

	private Collection<OWLDataPropertyExpression> getSuperExprs(OWLDataProperty entity) {

		return EntitySearcher.getSuperProperties(entity, allOntologies);
	}

	private Collection<OWLClassExpression> getEquivalentsGroup(OWLClass entity) {

		return EntitySearcher.getEquivalentClasses(entity, allOntologies);
	}

	private Collection<OWLObjectPropertyExpression> getEquivalentsGroup(OWLObjectProperty entity) {

		return EntitySearcher.getEquivalentProperties(entity, allOntologies);
	}

	private Collection<OWLDataPropertyExpression> getEquivalentsGroup(OWLDataProperty entity) {

		return EntitySearcher.getEquivalentProperties(entity, allOntologies);
	}

	private Collection<OWLIndividual> getSameIndividualsGroup(OWLNamedIndividual entity) {

		return EntitySearcher.getSameIndividuals(entity, allOntologies);
	}

	private void resolveIntersections(
					Collection<OWLClassExpression> ins,
					Collection<OWLClassExpression> outs,
					boolean nestedOnly) {

		for (OWLClassExpression in : ins) {

			if (in instanceof OWLObjectIntersectionOf) {

				OWLObjectIntersectionOf i = (OWLObjectIntersectionOf)in;

				resolveIntersections(i.getOperands(), outs, false);
			}
			else {

				if (!nestedOnly) {

					outs.add(in);
				}
			}
		}
	}

	private <I, O extends I>Collection<O> filterForType(Collection<I> ins, Class<O> outType) {

		Set<O> outs = new HashSet<O>();

		for (I in : ins) {

			if (outType.isAssignableFrom(in.getClass())) {

				outs.add(outType.cast(in));
			}
		}

		return outs;
	}
}
