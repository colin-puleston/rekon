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

package rekon.test;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.*;

class AutoQueryGenerator extends QueryProvider {

	private OWLDataFactory factory;

	private int max;
	private int generated = 0;

	private Iterator<OWLEquivalentClassesAxiom> axioms;
	private Iterator<OWLClassExpression> exprs = null;

	private OWLObjectIntersectionOf expander = null;

	AutoQueryGenerator(TestManager manager, int max) {

		this.max = max;

		factory = manager.getFactory();
		axioms = getAllEquivalenceAxioms(manager.rootOntology).iterator();
	}

	OWLClassExpression next() {

		if (max > 0 && generated >= max) {

			return null;
		}

		OWLObjectIntersectionOf nextGen = nextIntersection();

		if (nextGen == null) {

			return null;
		}

		if (expander == null) {

			return processNext(prune(nextGen), nextGen);
		}

		return processNext(expand(nextGen), null);
	}

	private OWLObjectIntersectionOf nextIntersection() {

		while (true) {

			if (exprs == null || !exprs.hasNext()) {

				if (!axioms.hasNext()) {

					break;
				}

				exprs = axioms.next().getClassExpressions().iterator();
			}

			OWLClassExpression nextExpr = exprs.next();

			if (nextExpr instanceof OWLObjectIntersectionOf) {

				return (OWLObjectIntersectionOf)nextExpr;
			}
		}

		return null;
	}

	private OWLObjectIntersectionOf processNext(
										OWLObjectIntersectionOf next,
										OWLObjectIntersectionOf nextExpander) {

		expander = nextExpander;

		generated++;

		return next;
	}

	private OWLObjectIntersectionOf prune(OWLObjectIntersectionOf in) {

		Set<OWLClassExpression> ops = in.getOperands();

		ops.remove(ops.iterator().next());

		return factory.getOWLObjectIntersectionOf(ops);
	}

	private OWLObjectIntersectionOf expand(OWLObjectIntersectionOf in) {

		Set<OWLClassExpression> ops = in.getOperands();

		ops.add(expander.getOperands().iterator().next());

		return factory.getOWLObjectIntersectionOf(ops);
	}

	private Set<OWLEquivalentClassesAxiom> getAllEquivalenceAxioms(OWLOntology rootOntology) {

		return rootOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES, Imports.INCLUDED);
	}
}