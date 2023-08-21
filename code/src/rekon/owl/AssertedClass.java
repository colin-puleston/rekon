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

import gnu.trove.set.hash.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
class AssertedClass extends AssertedHierarchyEntity<OWLClass> {

	private OWLDataFactory factory;

	private Set<OWLClassExpression> equivExprs = new THashSet<OWLClassExpression>();
	private Set<OWLClassExpression> superExprs = new THashSet<OWLClassExpression>();

	AssertedClass(OWLClass entity, OWLDataFactory factory) {

		super(entity);

		this.factory = factory;
	}

	void addEquivExpr(OWLClassExpression equiv) {

		if (equiv instanceof OWLClass) {

			addEquiv((OWLClass)equiv);
		}
		else {

			equivExprs.add(equiv);
		}
	}

	void addSuperExpr(OWLClassExpression sup) {

		if (sup instanceof OWLClass) {

			addSuper((OWLClass)sup);
		}
		else if (sup instanceof OWLObjectIntersectionOf) {

			addSuperIntersection((OWLObjectIntersectionOf)sup);
		}
		else {

			superExprs.add(sup);
		}
	}

	Collection<OWLClass> getSupers() {

		Set<OWLClass> sups = new THashSet<OWLClass>(super.getSupers());

		for (OWLClassExpression e : equivExprs) {

			collectConjunctClasses(e, sups);
		}

		return sups;
	}

	Collection<OWLClassExpression> getEquivExprs() {

		return equivExprs;
	}

	Collection<OWLClassExpression> getSuperExprs() {

		return superExprs;
	}

	OWLEquivalentClassesAxiom equivExprToAxiom(OWLClassExpression equiv) {

		return factory.getOWLEquivalentClassesAxiom(getEntity(), equiv);
	}

	private void addSuperIntersection(OWLObjectIntersectionOf supInt) {

		for (OWLClassExpression sup : supInt.getOperands()) {

			addSuperExpr(sup);
		}
	}

	private void collectConjunctClasses(OWLClassExpression expr, Set<OWLClass> collected) {

		if (expr instanceof OWLObjectIntersectionOf) {

			collectConjunctClasses(((OWLObjectIntersectionOf)expr).getOperands(), collected);
		}
	}

	private void collectConjunctClasses(Set<OWLClassExpression> exprs, Set<OWLClass> collected) {

		for (OWLClassExpression e : exprs) {

			if (e instanceof OWLClass) {

				collected.add((OWLClass)e);
			}
			else if (e instanceof OWLObjectIntersectionOf) {

				collectConjunctClasses(e, collected);
			}
		}
	}
}
