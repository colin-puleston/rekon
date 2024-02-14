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

	private abstract class ConvertedExpression implements InputExpression {

		private OWLClassExpression owlExpression;

		public boolean equals(Object other) {

			if (getClass() == other.getClass()) {

				return owlExpression.equals(((ConvertedExpression)other).owlExpression);
			}

			return false;
		}

		public int hashCode() {

			return owlExpression.hashCode();
		}

		public String toString() {

			return getClass().getSimpleName() + "[" + owlExpression + "]";
		}

		public Object getSourceObject() {

			return owlExpression;
		}

		public void notifyComponentOutOfScope() {

			logOutOfScope(false);
		}

		public void notifyComponentOutOfScopeInContext() {

			logOutOfScope(true);
		}

		ConvertedExpression(OWLClassExpression owlExpression) {

			this.owlExpression = owlExpression;
		}

		void resetOwlExpression(OWLClassExpression owlExpression) {

			this.owlExpression = owlExpression;
		}

		OwlIndividuals asOwlIndividuals() {

			return new OwlIndividuals(owlExpressionAs(OWLObjectOneOf.class));
		}

		OWLClassExpression getOwlExpression() {

			return owlExpression;
		}

		boolean owlExpressionType(Class<? extends OWLClassExpression> type) {

			return type.isAssignableFrom(owlExpression.getClass());
		}

		<T extends OWLClassExpression>T owlExpressionAs(Class<T> type) {

			return owlObjectAs(owlExpression, type);
		}

		private void logOutOfScope(boolean inContext) {

			Logger logger = Logger.SINGLETON;

			logger.logOutOfScopeWarningLine("Expression", inContext);
			logger.logLine("EXPRESSION: " + owlExpression);

			logger.logSeparatorLine();
		}
	}

	private class ConvertedRelation extends ConvertedExpression implements InputRelation {

		public InputRelationType getRelationType() {

			OWLClassExpression owlExpr = getOwlExpression();

			if (owlExpr instanceof OWLObjectComplementOf) {

				return getRelationType((OWLObjectComplementOf)owlExpr);
			}

			if (owlExpr instanceof OWLRestriction) {

				return getRelationType((OWLRestriction)owlExpr);
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		public boolean hasRelationType(InputRelationType type) {

			return getRelationType() == type;
		}

		public InputRelation asComplemented() {

			OWLObjectComplementOf c = owlExpressionAs(OWLObjectComplementOf.class);

			return new ConvertedRelation(c.getOperand());
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

			OWLRestriction owlRest = getOwlRestriction();

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

		ConvertedRelation(OWLClassExpression owlExpression) {

			super(owlExpression);
		}

		private InputRelationType getRelationType(OWLObjectComplementOf owlExpr) {

			if (complementedOwlRestriction(owlExpr)) {

				return InputRelationType.COMPLEMENT;
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		private InputRelationType getRelationType(OWLRestriction owlExpr) {

			if (owlExpr.getProperty() instanceof OWLProperty) {

				if (owlExpr instanceof OWLObjectSomeValuesFrom
					|| owlExpr instanceof OWLObjectHasValue) {

					return InputRelationType.SOME_NODES;
				}

				if (owlExpr instanceof OWLObjectAllValuesFrom) {

					return InputRelationType.ALL_NODES;
				}

				if (owlExpr instanceof OWLDataSomeValuesFrom
					|| owlExpr instanceof OWLDataHasValue) {

					return InputRelationType.DATA_VALUE;
				}
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		private boolean complementedOwlRestriction(OWLObjectComplementOf owlComp) {

			return owlComp.getOperand() instanceof OWLRestriction;
		}

		private DataValue extractDataType() {

			return dataTypes.get(owlFillerAs(OWLDataRange.class));
		}

		private DataValue extractDataValue() {

			return DataTypes.toDataValueExpression(owlFillerAs(OWLLiteral.class));
		}

		private <P extends OWLProperty>P owlPropertyAs(Class<P> type) {

			return owlObjectAs(getOwlRestriction().getProperty(), type);
		}

		private OWLClassExpression resolveOwlFillerToClassExpression() {

			if (owlExpressionType(OWLObjectHasValue.class)) {

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

			return owlObjectAs(getOwlFiller(), type);
		}

		private OWLObject getOwlFiller() {

			return owlObjectAs(getOwlRestriction(), HasFiller.class).getFiller();
		}

		private OWLRestriction getOwlRestriction() {

			return owlExpressionAs(OWLRestriction.class);
		}
	}

	private class ConvertedNode extends ConvertedExpression implements InputNode {

		public InputComplex toComplex() {

			return new ConvertedComplex(getOwlExpression());
		}

		public InputNodeType getNodeType() {

			OWLClassExpression owlExpr = getOwlExpression();

			if (allowSimpleTypes()) {

				if (owlExpr instanceof OWLClass) {

					return InputNodeType.CLASS;
				}

				if (owlExpr instanceof OWLObjectOneOf) {

					if (asOwlIndividuals().singleInScopeIndividual()) {

						return InputNodeType.INDIVIDUAL;
					}

					return InputNodeType.OUT_OF_SCOPE;
				}
			}

			if (allowConjunctions()) {

				if (owlExpr instanceof OWLObjectIntersectionOf) {

					return InputNodeType.CONJUNCTION;
				}
			}

			if (owlExpr instanceof OWLObjectUnionOf) {

				return InputNodeType.DISJUNCTION;
			}

			if (asRelationOrNull(owlExpr) != null) {

				return InputNodeType.RELATION;
			}

			return InputNodeType.OUT_OF_SCOPE;
		}

		public ClassNode asClassNode() {

			return names.get(owlExpressionAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return names.get(asOwlIndividuals().asSingleOwlIndividual());
		}

		public Collection<InputNode> asConjuncts() {

			return toNodes(owlExpressionAs(OWLObjectIntersectionOf.class).getOperands());
		}

		public Collection<InputNode> asDisjuncts() {

			return toNodes(owlExpressionAs(OWLObjectUnionOf.class).getOperands());
		}

		public InputRelation asRelation() {

			ConvertedRelation rel = asRelationOrNull(getOwlExpression());

			if (rel != null) {

				return rel;
			}

			throw new Error("Unexpected OWL-object type: " + getOwlExpression());
		}

		ConvertedNode(OWLClassExpression owlExpression) {

			super(owlExpression);

			if (owlExpression instanceof OWLObjectOneOf) {

				OwlIndividuals inds = asOwlIndividuals();

				if (inds.multiInScopeIndividuals()) {

					resetOwlExpression(inds.asOwlExpressionUnion());
				}
			}
		}

		boolean allowSimpleTypes() {

			return true;
		}

		boolean allowConjunctions() {

			return true;
		}

		private ConvertedRelation asRelationOrNull(OWLClassExpression owlExpr) {

			ConvertedRelation rel = new ConvertedRelation(owlExpr);

			return rel.hasRelationType(InputRelationType.OUT_OF_SCOPE) ? null : rel;
		}

		private List<InputNode> toNodes(Set<? extends OWLClassExpression> owlExprs) {

			List<InputNode> nodes = new ArrayList<InputNode>();

			for (OWLClassExpression e : owlExprs) {

				nodes.add(new ConvertedNode(e));
			}

			return nodes;
		}
	}

	private class ConvertedComplex extends ConvertedNode implements InputComplex {

		public InputNode toNode() {

			return this;
		}

		public InputComplexType getComplexType() {

			return getNodeType().toComplexType();
		}

		ConvertedComplex(OWLClassExpression owlExpression) {

			super(owlExpression);
		}

		boolean allowSimpleTypes() {

			return false;
		}
	}

	private class ConvertedComplexSuper extends ConvertedComplex implements InputComplexSuper {

		public InputComplex toComplex() {

			return this;
		}

		public InputComplexSuperType getComplexSuperType() {

			return getComplexType().toComplexSuperType();
		}

		ConvertedComplexSuper(OWLClassExpression owlExpression) {

			super(owlExpression);
		}

		boolean allowConjunctions() {

			return false;
		}
	}

	ExpressionConverter(OWLDataFactory factory, MappedNames names) {

		this.factory = factory;
		this.names = names;
	}

	InputNode toNode(OWLClassExpression owlExpression) {

		return new ConvertedNode(owlExpression);
	}

	InputComplex toComplex(OWLClassExpression owlExpression) {

		return new ConvertedComplex(owlExpression);
	}

	InputComplexSuper toComplexSuper(OWLClassExpression owlExpression) {

		return new ConvertedComplexSuper(owlExpression);
	}

	InputRelation toRelation(OWLClassExpression owlExpression) {

		return new ConvertedRelation(owlExpression);
	}

	private <O>O owlObjectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
