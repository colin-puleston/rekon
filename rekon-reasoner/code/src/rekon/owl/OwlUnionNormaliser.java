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
class OwlUnionNormaliser {

	private OWLDataFactory factory;
	private OWLObjectUnionOf union;

	private List<OperandSet> operandSets = new ArrayList<OperandSet>();
	private boolean mergableRestrictions = false;

	private abstract class OperandSet {

		abstract boolean absorb(OWLClassExpression op);

		abstract OWLClassExpression getNormalised();
	}

	private class DefaultOperandSet extends OperandSet {

		private OWLClassExpression operand;

		DefaultOperandSet(OWLClassExpression operand) {

			this.operand = operand;
		}

		boolean absorb(OWLClassExpression operand) {

			return false;
		}

		OWLClassExpression getNormalised() {

			return operand;
		}
	}

	private abstract class RestrictionSet
							<R extends OWLQuantifiedObjectRestriction>
							extends OperandSet {

		final OWLObjectProperty property;

		private R singleRestriction;
		private Set<OWLClassExpression> fillers = new HashSet<OWLClassExpression>();

		RestrictionSet(
			R initialRestriction,
			OWLObjectProperty property) {

			singleRestriction = initialRestriction;

			this.property = property;

			addFiller(initialRestriction);
		}

		boolean absorb(OWLClassExpression op) {

			Class<R> resClass = getRestrictionClass();
			OWLObjectProperty p = getRestrictionPropertyOrNull(op, resClass);

			if (p != null && p.equals(property)) {

				mergableRestrictions |= true;
				singleRestriction = null;

				addFiller(resClass.cast(op));

				return true;
			}

			return false;
		}

		OWLClassExpression getNormalised() {

			if (singleRestriction != null) {

				return singleRestriction;
			}

			return createNewRestriction(factory.getOWLObjectUnionOf(fillers));
		}

		abstract Class<R> getRestrictionClass();

		abstract R createNewRestriction(OWLObjectUnionOf filler);

		private void addFiller(R restriction) {

			fillers.add(restriction.getFiller());
		}
	}

	private class SomeRestrictionSet extends RestrictionSet<OWLObjectSomeValuesFrom> {

		SomeRestrictionSet(
			OWLObjectSomeValuesFrom initialRestriction,
			OWLObjectProperty property) {

			super(initialRestriction, property);
		}

		Class<OWLObjectSomeValuesFrom> getRestrictionClass() {

			return OWLObjectSomeValuesFrom.class;
		}

		OWLObjectSomeValuesFrom createNewRestriction(OWLObjectUnionOf filler) {

			return factory.getOWLObjectSomeValuesFrom(property, filler);
		}
	}

	private class AllRestrictionSet extends RestrictionSet<OWLObjectAllValuesFrom> {

		AllRestrictionSet(
			OWLObjectAllValuesFrom initialRestriction,
			OWLObjectProperty property) {

			super(initialRestriction, property);
		}

		Class<OWLObjectAllValuesFrom> getRestrictionClass() {

			return OWLObjectAllValuesFrom.class;
		}

		OWLObjectAllValuesFrom createNewRestriction(OWLObjectUnionOf filler) {

			return factory.getOWLObjectAllValuesFrom(property, filler);
		}
	}

	OwlUnionNormaliser(OWLDataFactory factory, OWLObjectUnionOf union) {

		this.factory = factory;
		this.union = union;

		absorbOps();
	}

	OWLClassExpression normalise() {

		return mergableRestrictions ? createNormalised() : union;
	}

	private OWLClassExpression createNormalised() {

		Set<OWLClassExpression> ops = new HashSet<OWLClassExpression>();

		for (OperandSet set : operandSets) {

			ops.add(set.getNormalised());
		}

		return ops.size() == 1 ? ops.iterator().next() : factory.getOWLObjectUnionOf(ops);
	}

	private void absorbOps() {

		for (OWLClassExpression op : union.getOperands()) {

			if (!absorbIntoOpSet(op)) {

				operandSets.add(createNewOpSet(op));
			}
		}
	}

	private boolean absorbIntoOpSet(OWLClassExpression op) {

		for (OperandSet set : operandSets) {

			if (set.absorb(op)) {

				return true;
			}
		}

		return false;
	}

	private OperandSet createNewOpSet(OWLClassExpression op) {

		OWLObjectProperty p = getRestrictionPropertyOrNull(op);

		if (p != null) {

			if (op instanceof OWLObjectSomeValuesFrom) {

				return new SomeRestrictionSet((OWLObjectSomeValuesFrom)op, p);
			}

			if (op instanceof OWLObjectAllValuesFrom) {

				return new AllRestrictionSet((OWLObjectAllValuesFrom)op, p);
			}
		}

		return new DefaultOperandSet(op);
	}

	private OWLObjectProperty getRestrictionPropertyOrNull(OWLClassExpression e) {

		return getRestrictionPropertyOrNull(e, OWLQuantifiedObjectRestriction.class);
	}

	private <R extends OWLQuantifiedObjectRestriction>
							OWLObjectProperty getRestrictionPropertyOrNull(
								OWLClassExpression e,
								Class<? extends R> resClass) {

		if (resClass.isAssignableFrom(e.getClass())) {

			OWLObjectPropertyExpression pe = resClass.cast(e).getProperty();

			if (pe instanceof OWLObjectProperty) {

				return (OWLObjectProperty)pe;
			}
		}

		return null;
	}
}
