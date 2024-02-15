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
 * FITNESS FOR E PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OUT_OF_SCOPEWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OUT_OF_SCOPE DEALINGS IN
 * THE SOFTWARE.
 */

package rekon.owl;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
class NoValueOwlExpressionResolver {

	static private final IRI REKON_NO_VALUE_IRI = IRI.create("urn:rekon:RekonNoValue");

	static OWLClass getNoValueClass(OWLDataFactory factory) {

		return factory.getOWLClass(REKON_NO_VALUE_IRI);
	}

	static boolean isNoValueClass(OWLClass test) {

		return test.getIRI().equals(REKON_NO_VALUE_IRI);
	}

	private OWLDataFactory factory;

	private OWLClass owlThing;
	private OWLClass owlNothing;

	private OWLClass rekonNoValue;

	NoValueOwlExpressionResolver(OWLDataFactory factory) {

		this.factory = factory;

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		rekonNoValue = getNoValueClass(factory);
	}

	OWLClassExpression resolve(OWLClassExpression expr) {

		OWLClassExpression resolvedExpr = checkCreateForNoValue(expr);

		return resolvedExpr != null ? resolvedExpr : expr;
	}

	private OWLClassExpression checkCreateForNoValue(OWLClassExpression expr) {

		if (expr instanceof OWLObjectAllValuesFrom) {

			return checkCreateForNoValue((OWLObjectAllValuesFrom)expr, owlNothing);
		}

		if (expr instanceof OWLObjectComplementOf) {

			return checkCreateForNoValue((OWLObjectComplementOf)expr);
		}

		return null;
	}

	private OWLClassExpression checkCreateForNoValue(OWLObjectComplementOf expr) {

		OWLClassExpression comp = expr.getOperand();

		if (comp instanceof OWLObjectSomeValuesFrom) {

			return checkCreateForNoValue((OWLObjectSomeValuesFrom)comp, owlThing);
		}

		return null;
	}

	private OWLClassExpression checkCreateForNoValue(
									OWLQuantifiedObjectRestriction expr,
									OWLClass testFiller) {

		if (expr.getFiller().equals(testFiller)) {

			return createForNoValue(expr.getProperty());
		}

		return null;
	}

	private OWLRestriction createForNoValue(OWLObjectPropertyExpression p) {

		return factory.getOWLObjectSomeValuesFrom(p, rekonNoValue);
	}
}