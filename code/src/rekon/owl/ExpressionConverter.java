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

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class ExpressionConverter {

	private OWLDataFactory factory;

	private MappedNames names;
	private DataTypes dataTypes = new DataTypes(false);

	private abstract class ConvertedComponent<O extends OWLObject> implements InputComponent {

		private O owlObject;

		public boolean equals(Object other) {

			if (getClass() == other.getClass()) {

				return owlObject.equals(((ConvertedComponent<?>)other).owlObject);
			}

			return false;
		}

		public int hashCode() {

			return owlObject.hashCode();
		}

		public String toString() {

			return getClass().getSimpleName() + "[" + owlObject + "]";
		}

		public Object getSourceObject() {

			return owlObject;
		}

		ConvertedComponent(O owlObject) {

			this.owlObject = owlObject;
		}

		void resetOwlObject(O owlObject) {

			this.owlObject = owlObject;
		}

		O getOwlObject() {

			return owlObject;
		}

		boolean owlObjectType(Class<? extends O> type) {

			return type.isAssignableFrom(owlObject.getClass());
		}

		<T extends O>T owlObjectAs(Class<T> type) {

			return objectAs(owlObject, type);
		}
	}

	private abstract class ConvertedExpression extends ConvertedComponent<OWLClassExpression> {

		class OwlIndividuals {

			private OWLObjectOneOf disjunction = owlObjectAs(OWLObjectOneOf.class);

			boolean singleInScopeIndividual() {

				return inScopeIndividuals(true);
			}

			boolean multiInScopeIndividuals() {

				return inScopeIndividuals(false);
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

				throw new RuntimeException("Unexpected anonymous individual!");
			}

			OWLClassExpression asOwlObjectUnion() {

				return disjunction.asObjectUnionOf();
			}

			private boolean inScopeIndividuals(boolean single) {

				return allDisjunctsNamed() && singleDisjunct(single);
			}

			private boolean singleDisjunct(boolean single) {

				return (getDisjuncts().size() == 1) == single;
			}

			private boolean allDisjunctsNamed() {

				for (OWLIndividual d : getDisjuncts()) {

					if (!(d instanceof OWLNamedIndividual)) {

						return false;
					}
				}

				return true;
			}

			private Set<OWLIndividual> getDisjuncts() {

				return disjunction.getIndividuals();
			}
		}

		ConvertedExpression(OWLClassExpression owlExpression) {

			super(owlExpression);
		}
	}

	private class ConvertedComplex extends ConvertedExpression implements InputComplex {

		public InputComplexType getComplexType() {

			OWLClassExpression owlExpr = getOwlObject();

			if (owlExpr instanceof OWLObjectIntersectionOf) {

				return InputComplexType.CONJUNCTION;
			}

			if (owlExpr instanceof OWLObjectUnionOf) {

				return InputComplexType.DISJUNCTION;
			}

			if (owlExpr instanceof OWLObjectComplementOf) {

				return InputComplexType.COMPLEMENT;
			}

			if (owlExpr instanceof OWLRestriction) {

				if (validRelationType((OWLRestriction)owlExpr)) {

					return InputComplexType.RELATION;
				}
			}

			return InputComplexType.OUT_OF_SCOPE;
		}

		public boolean hasComplexType(InputComplexType type) {

			return getComplexType() == type;
		}

		public InputNode toNode() {

			return new ConvertedNode(getOwlObject());
		}

		public Collection<InputNode> asConjuncts() {

			return toInputExprs(owlObjectAs(OWLObjectIntersectionOf.class).getOperands());
		}

		public Collection<InputNode> asDisjuncts() {

			return toInputExprs(owlObjectAs(OWLObjectUnionOf.class).getOperands());
		}

		public InputComplex asComplemented() {

			OWLObjectComplementOf c = owlObjectAs(OWLObjectComplementOf.class);

			return new ConvertedComplex(c.getOperand());
		}

		public InputRelation asRelation() {

			return new ConvertedRelation(owlObjectAs(OWLRestriction.class));
		}

		ConvertedComplex(OWLClassExpression owlExpression) {

			super(owlExpression);

			if (owlExpression instanceof OWLObjectOneOf) {

				OwlIndividuals inds = new OwlIndividuals();

				if (inds.multiInScopeIndividuals()) {

					resetOwlObject(inds.asOwlObjectUnion());
				}
			}
		}

		private boolean validRelationType(OWLRestriction owlRest) {

			ConvertedRelation rel = new ConvertedRelation(owlRest);

			return rel.getRelationType() != InputRelationType.OUT_OF_SCOPE;
		}
	}

	private class ConvertedComplexSuper
						extends ConvertedExpression
						implements InputComplexSuper {

		private ConvertedComplex complex;

		public InputComplexSuperType getComplexSuperType() {

			InputComplexType complexType = complex.getComplexType();

			for (InputComplexSuperType supType : InputComplexSuperType.values()) {

				if (supType.toComplexType() == complexType) {

					return supType;
				}
			}

			throw new RuntimeException("Cannot handle expression: " + getOwlObject());
		}

		public InputComplex toComplex() {

			return complex;
		}

		ConvertedComplexSuper(OWLClassExpression owlExpression) {

			super(owlExpression);

			complex = new ConvertedComplex(owlExpression);
		}
	}

	private class ConvertedNode extends ConvertedExpression implements InputNode {

		public InputNodeType getNodeType() {

			OWLClassExpression owlExpr = getOwlObject();

			if (owlExpr instanceof OWLClass) {

				return InputNodeType.CLASS;
			}

			if (isComplexSource(owlExpr)) {

				return InputNodeType.COMPLEX;
			}

			if (owlExpr instanceof OWLObjectOneOf) {

				if (new OwlIndividuals().singleInScopeIndividual()) {

					return InputNodeType.INDIVIDUAL;
				}
			}

			System.out.println("NODE:OUT_OF_SCOPE: " + getOwlObject());
			return InputNodeType.OUT_OF_SCOPE;
		}

		public boolean hasNodeType(InputNodeType type) {

			return getNodeType() == type;
		}

		public boolean hasComplexType(InputComplexType type) {

			return hasNodeType(InputNodeType.COMPLEX) && asComplex().hasComplexType(type);
		}

		public ClassNode asClassNode() {

			return names.get(owlObjectAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return names.get(new OwlIndividuals().asSingleOwlIndividual());
		}

		public InputComplex asComplex() {

			return new ConvertedComplex(getOwlObject());
		}

		ConvertedNode(OWLClassExpression owlExpression) {

			super(owlExpression);
		}

		private boolean isComplexSource(OWLClassExpression owlExpr) {

			ConvertedComplex asComplex = new ConvertedComplex(owlExpr);

			return !asComplex.hasComplexType(InputComplexType.OUT_OF_SCOPE);
		}
	}

	private class ConvertedRelation
						extends ConvertedComponent<OWLRestriction>
						implements InputRelation {

		public InputRelationType getRelationType() {

			OWLRestriction owlRest = getOwlObject();

			if (owlRest instanceof HasFiller<?>
				&& owlRest.getProperty() instanceof OWLProperty) {

				if (owlRest instanceof OWLObjectSomeValuesFrom) {

					return InputRelationType.SOME_NODES;
				}

				if (owlRest instanceof OWLObjectHasValue) {

					return InputRelationType.SOME_NODES;
				}

				if (owlRest instanceof OWLObjectAllValuesFrom) {

					return InputRelationType.ALL_NODES;
				}

				if (owlRest instanceof OWLDataSomeValuesFrom) {

					return InputRelationType.DATA_VALUE;
				}

				if (owlRest instanceof OWLDataHasValue) {

					return InputRelationType.DATA_VALUE;
				}
			}

			System.out.println("RELATION:OUT_OF_SCOPE: " + getOwlObject());
			return InputRelationType.OUT_OF_SCOPE;
		}

		public NodeProperty getNodeProperty() {

			return names.get(owlPropertyAs(OWLObjectProperty.class));
		}

		public DataProperty getDataProperty() {

			return names.get(owlPropertyAs(OWLDataProperty.class));
		}

		public InputNode getExpressionValue() {

			return new ConvertedNode(resolveOwlFillerToClassExpression());
		}

		public DataValue getDataValue() {

			OWLRestriction owlRest = getOwlObject();

			if (owlRest instanceof OWLDataSomeValuesFrom) {

				return extractDataType();
			}

			if (owlRest instanceof OWLDataHasValue) {

				return extractDataValue();
			}

			throw new Error(
						"Unexpected OWLDataRestriction type: "
						+ owlRest.getClass());
		}

		ConvertedRelation(OWLRestriction owlRestriction) {

			super(owlRestriction);
		}

		private DataValue extractDataType() {

			return dataTypes.get(owlFillerAs(OWLDataRange.class));
		}

		private DataValue extractDataValue() {

			return DataTypes.toDataValueExpression(owlFillerAs(OWLLiteral.class));
		}

		private <P extends OWLProperty>P owlPropertyAs(Class<P> type) {

			return objectAs(getOwlObject().getProperty(), type);
		}

		private OWLClassExpression resolveOwlFillerToClassExpression() {

			if (owlObjectType(OWLObjectHasValue.class)) {

				return individualToClassExpression(owlFillerAs(OWLIndividual.class));
			}

			return owlFillerAs(OWLClassExpression.class);
		}

		private OWLClassExpression individualToClassExpression(OWLIndividual i) {

			if (i instanceof OWLNamedIndividual) {

				return factory.getOWLObjectOneOf(Collections.singleton(i));
			}

			throw new RuntimeException("Unexpected anonymous individual!");
		}

		private <F extends OWLObject>F owlFillerAs(Class<F> type) {

			return objectAs(getOwlFiller(), type);
		}

		private OWLObject getOwlFiller() {

			return objectAs(getOwlObject(), HasFiller.class).getFiller();
		}
	}

	ExpressionConverter(OWLDataFactory factory, MappedNames names) {

		this.factory = factory;
		this.names = names;
	}

	InputComplex toComplex(OWLClassExpression owlExpression) {

		return new ConvertedComplex(owlExpression);
	}

	InputComplexSuper toComplexSuper(OWLClassExpression owlExpression) {

		return new ConvertedComplexSuper(owlExpression);
	}

	InputNode toNode(OWLClassExpression owlExpression) {

		return new ConvertedNode(owlExpression);
	}

	InputRelation toRelation(OWLRestriction owlRestriction) {

		return new ConvertedRelation(owlRestriction);
	}

	private List<InputNode> toInputExprs(Set<? extends OWLClassExpression> owlExprs) {

		List<InputNode> exprs = new ArrayList<InputNode>();

		for (OWLClassExpression oe : owlExprs) {

			exprs.add(new ConvertedNode(oe));
		}

		return exprs;
	}

	private <O>O objectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
