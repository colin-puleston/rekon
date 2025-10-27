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

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
abstract class PropertyAxiomConverter
					<E extends OWLPropertyExpression, P extends PropertyX>
					extends CategoryAxiomConverter {

	abstract class ConvertedPropertyAttribute
						<P extends PropertyX>
						extends ConvertedAxiom {

		private P property;

		public P getProperty() {

			return property;
		}

		ConvertedPropertyAttribute(OWLAxiom source, P property) {

			super(source);

			this.property = property;
		}
	}

	class OwlPropertyLink extends OwlLink<E, P> {

		OwlPropertyLink(OWLAxiom source, Set<E> equivs) {

			super(source, equivs);
		}

		OwlPropertyLink(OWLAxiom source, E sub, E sup) {

			super(source, sub, sup);
		}

		boolean invalidEndPointExprType(E expr) {

			return !(expr instanceof OWLProperty);
		}

		boolean invalidEndPointInbuiltEntity(E expr) {

			return expr.equals(getOWLBottomProperty());
		}

		P asName(E expr) {

			return toProperty(expr);
		}
	}

	abstract class PropertyLinkConverter
						<S extends OWLAxiom, I extends InputAxiom>
						extends TypeAxiomConverter<S, I> {

		PropertyLinkConverter() {

			super(parentConverter);
		}

		I checkConvert(S source) {

			OwlPropertyLink owlLink = createOwlLink(source);

			return owlLink.checkValidEndPoints() ? createInputAxiom(owlLink) : null;
		}

		abstract OwlPropertyLink createOwlLink(S source);

		abstract I createInputAxiom(OwlPropertyLink owlLink);
	}

	abstract class PropertyAttributeConverter
						<S extends OWLAxiom, I extends InputAxiom>
						extends TypeAxiomConverter<S, I> {

		PropertyAttributeConverter() {

			super(parentConverter);
		}

		I checkConvert(S source) {

			E expr = getPropertyExpr(source);
			P p = toPropertyOrNull(expr);

			if (p != null) {

				I ax = createInputAxiom(p, source);

				if (ax != null) {

					return ax;
				}
			}

			return null;
		}

		abstract E getPropertyExpr(S source);

		abstract I createInputAxiom(P prop, S source);
	}

	PropertyAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);
	}

	abstract E getOWLBottomProperty();

	abstract P toPropertyOrNull(E expr);

	List<P> toPropertiesOrNull(List<E> exprs) {

		List<P> props = new ArrayList<P>();

		for (E e : exprs) {

			P p = toPropertyOrNull(e);

			if (p == null) {

				return null;
			}

			props.add(p);
		}

		return props;
	}

	ClassNode toClassNode(OWLClassExpression e) {

		return e instanceof OWLClass ? names.resolve((OWLClass)e) : null;
	}

	private P toProperty(E expr) {

		P p = toPropertyOrNull(expr);

		if (p != null) {

			return p;
		}

		throw new Error("Cannot handle property-expression: " + expr);
	}
}
