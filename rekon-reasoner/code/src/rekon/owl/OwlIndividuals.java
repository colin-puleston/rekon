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
class OwlIndividuals {

	private OWLObjectOneOf owlOneOf;

	OwlIndividuals(OWLObjectOneOf owlOneOf) {

		this.owlOneOf = owlOneOf;
	}

	boolean singleInScopeIndividual() {

		return getDisjuncts().size() == 1 && allIndividualsInScope();
	}

	boolean allIndividualsInScope() {

		for (OWLIndividual d : getDisjuncts()) {

			if (!(d instanceof OWLNamedIndividual)) {

				return false;
			}
		}

		return true;
	}

	OWLNamedIndividual asSingleOwlIndividual() {

		Set<OWLIndividual> disjuncts = getDisjuncts();

		if (disjuncts.size() != 1) {

			throw new Error("Unexpected multiple individuals: " + disjuncts);
		}

		OWLIndividual i = disjuncts.iterator().next();

		if (i instanceof OWLNamedIndividual) {

			return (OWLNamedIndividual)i;
		}

		throw new Error("Unexpected anonymous individual!");
	}

	OWLClassExpression asOwlExpression() {

		OWLClassExpression e = owlOneOf.asObjectUnionOf();

		if (e instanceof OWLObjectUnionOf) {

			Set<OWLClassExpression> ops = ((OWLObjectUnionOf)e).getOperands();

			if (ops.size() == 1) {

				return ops.iterator().next();
			}
		}

		return e;
	}

	private Set<OWLIndividual> getDisjuncts() {

		return owlOneOf.getIndividuals();
	}
}
