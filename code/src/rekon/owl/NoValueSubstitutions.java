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

import org.semanticweb.owlapi.model.*;

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class NoValueSubstitutions {

	static private final IRI REKON_NO_VALUE_IRI = IRI.create("urn:rekon:RekonNoValue");

	static final Option OPTION = new Option(false, "novalue-substitutions");

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

	private ExpressionResolver expressionResolver = new ExpressionResolver();
	private RestrictionResolver restrictionResolver = new RestrictionResolver();

	private abstract class Resolver<E extends OWLClassExpression> {

		E resolve(OwlContainer container, E expr) {

			if (OPTION.enabled()) {

				OWLRestriction newExpr = checkResolveToRestriction(expr);

				if (newExpr != null) {

					container.logNoValueExprReplacement(expr, newExpr);

					return toTypeExpression(newExpr);
				}
			}

			return expr;
		}

		abstract OWLRestriction checkResolveToRestriction(E expr);

		abstract E toTypeExpression(OWLRestriction expr);
	}

	private class ExpressionResolver extends Resolver<OWLClassExpression> {

		OWLRestriction checkResolveToRestriction(OWLClassExpression expr) {

			if (expr instanceof OWLObjectComplementOf) {

				return checkCreateForNoValue((OWLObjectComplementOf)expr);
			}

			if (expr instanceof OWLObjectAllValuesFrom) {

				return checkCreateForNoValue((OWLObjectAllValuesFrom)expr);
			}

			return null;
		}

		OWLClassExpression toTypeExpression(OWLRestriction expr) {

			return expr;
		}
	}

	private class RestrictionResolver extends Resolver<OWLRestriction> {

		OWLRestriction checkResolveToRestriction(OWLRestriction expr) {

			if (expr instanceof OWLObjectAllValuesFrom) {

				return checkCreateForNoValue((OWLObjectAllValuesFrom)expr);
			}

			return null;
		}

		OWLRestriction toTypeExpression(OWLRestriction expr) {

			return expr;
		}
	}

	NoValueSubstitutions(OWLDataFactory factory) {

		this.factory = factory;

		owlThing = factory.getOWLThing();
		owlNothing = factory.getOWLNothing();

		rekonNoValue = getNoValueClass(factory);
	}

	OWLClassExpression resolve(OwlContainer container, OWLClassExpression expr) {

		return expressionResolver.resolve(container, expr);
	}

	OWLRestriction resolve(OwlContainer container, OWLRestriction expr) {

		return restrictionResolver.resolve(container, expr);
	}

	private OWLRestriction checkCreateForNoValue(OWLObjectComplementOf expr) {

		OWLClassExpression comp = expr.getOperand();

		if (comp instanceof OWLObjectSomeValuesFrom) {

			OWLObjectSomeValuesFrom someComp = (OWLObjectSomeValuesFrom)comp;

			if (noValueComplementedSomeRestriction(someComp)) {

				return createForNoValue(someComp.getProperty());
			}
		}

		return null;
	}

	private OWLRestriction checkCreateForNoValue(OWLObjectAllValuesFrom expr) {

		return noValueAllRestriction(expr) ? createForNoValue(expr.getProperty()) : null;
	}

	private boolean noValueComplementedSomeRestriction(OWLObjectSomeValuesFrom expr) {

		OWLClassExpression f = expr.getFiller();

		if (f instanceof OWLObjectIntersectionOf) {

			return ((OWLObjectIntersectionOf)f).getOperands().isEmpty();
		}

		return f.equals(owlThing);
	}

	private boolean noValueAllRestriction(OWLObjectAllValuesFrom expr) {

		OWLClassExpression f = expr.getFiller();

		if (f instanceof OWLObjectUnionOf) {

			return ((OWLObjectUnionOf)f).getOperands().isEmpty();
		}

		return f.equals(owlNothing);
	}

	private OWLRestriction createForNoValue(OWLObjectPropertyExpression prop) {

		return factory.getOWLObjectSomeValuesFrom(prop, rekonNoValue);
	}
}
