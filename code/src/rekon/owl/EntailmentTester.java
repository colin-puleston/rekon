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

/**
 * @author Colin Puleston
 */
class EntailmentTester {

	private RekonOps ops;

	private IndividualResolver individualResolver = new IndividualResolver();
	private ObjectPropertyResolver objectPropertyResolver = new ObjectPropertyResolver();
	private DataPropertyResolver dataPropertyResolver = new DataPropertyResolver();

	private abstract class NamedEntityResolver<E extends OWLObject, N extends E> {

		Set<N> resolveAll(Set<E> exprs) {

			Set<N> nameds = new HashSet<N>();

			for (E expr : exprs) {

				N named = resolve(expr);

				if (named == null) {

					return null;
				}

				nameds.add(named);
			}

			return nameds;
		}

		N resolve(E expr) {

			Class<N> type = getNamedType();

			return expr.getClass() == type ? type.cast(expr) : null;
		}

		abstract Class<N> getNamedType();
	}

	private class ObjectPropertyResolver
					extends
						NamedEntityResolver<OWLObjectPropertyExpression, OWLObjectProperty> {

		Class<OWLObjectProperty> getNamedType() {

			return OWLObjectProperty.class;
		}
	}

	private class DataPropertyResolver
					extends
						NamedEntityResolver<OWLDataPropertyExpression, OWLDataProperty> {

		Class<OWLDataProperty> getNamedType() {

			return OWLDataProperty.class;
		}
	}

	private class IndividualResolver
					extends
						NamedEntityResolver<OWLIndividual, OWLNamedIndividual> {

		Class<OWLNamedIndividual> getNamedType() {

			return OWLNamedIndividual.class;
		}
	}

	EntailmentTester(RekonOps ops) {

		this.ops = ops;
	}

	boolean entailed(Set<? extends OWLAxiom> axioms) {

		for (OWLAxiom a : axioms) {

			if (!entailed(a)) {

				return false;
			}
		}

		return true;
	}

	boolean entailed(OWLAxiom axiom) {

		if (axiom instanceof OWLEquivalentClassesAxiom) {

			return entailed((OWLEquivalentClassesAxiom)axiom);
		}

		if (axiom instanceof OWLSubClassOfAxiom) {

			return entailed((OWLSubClassOfAxiom)axiom);
		}

		if (axiom instanceof OWLClassAssertionAxiom) {

			return entailed((OWLClassAssertionAxiom)axiom);
		}

		if (axiom instanceof OWLSameIndividualAxiom) {

			return entailed((OWLSameIndividualAxiom)axiom);
		}

		if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {

			return entailed((OWLEquivalentDataPropertiesAxiom)axiom);
		}

		if (axiom instanceof OWLSubDataPropertyOfAxiom) {

			return entailed((OWLSubDataPropertyOfAxiom)axiom);
		}

		return false;
	}

	private boolean entailed(OWLEquivalentClassesAxiom axiom) {

		return ops.classOps.equivalents(axiom.getClassExpressions());
	}

	private boolean entailed(OWLSubClassOfAxiom axiom) {

		return ops.classOps.subsumption(axiom.getSuperClass(), axiom.getSubClass());
	}

	private boolean entailed(OWLClassAssertionAxiom axiom) {

		OWLNamedIndividual ind = individualResolver.resolve(axiom.getIndividual());

		return ind != null && ops.individualOps.hasType(ind, axiom.getClassExpression());
	}

	private boolean entailed(OWLSameIndividualAxiom axiom) {

		Set<OWLNamedIndividual> inds = individualResolver.resolveAll(axiom.getIndividuals());

		return inds != null && ops.individualOps.equivalents(inds);
	}

	private boolean entailed(OWLEquivalentObjectPropertiesAxiom axiom) {

		Set<OWLObjectProperty> props = objectPropertyResolver.resolveAll(axiom.getProperties());

		return ops.objectPropertyOps.equivalents(props);
	}

	private boolean entailed(OWLSubObjectPropertyOfAxiom axiom) {

		OWLObjectProperty sup = objectPropertyResolver.resolve(axiom.getSuperProperty());
		OWLObjectProperty sub = objectPropertyResolver.resolve(axiom.getSubProperty());

		return sup != null && sub != null && ops.objectPropertyOps.subsumption(sup, sub);
	}

	private boolean entailed(OWLEquivalentDataPropertiesAxiom axiom) {

		Set<OWLDataProperty> props = dataPropertyResolver.resolveAll(axiom.getProperties());

		return ops.dataPropertyOps.equivalents(props);
	}

	private boolean entailed(OWLSubDataPropertyOfAxiom axiom) {

		OWLDataProperty sup = dataPropertyResolver.resolve(axiom.getSuperProperty());
		OWLDataProperty sub = dataPropertyResolver.resolve(axiom.getSubProperty());

		return sup != null && sub != null && ops.dataPropertyOps.subsumption(sup, sub);
	}
}
