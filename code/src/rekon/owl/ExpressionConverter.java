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

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class ExpressionConverter {

	private OWLDataFactory factory;

	private MappedNames names;

	private DataTypeConverter dataTypes = new DataTypeConverter(false);
	private NoValueOwlExpressionResolver noValueResolver;

	private abstract class ConvertedExpression implements InputExpression {

		private OWLAxiom owlAxiom;
		private OWLClassExpression owlExpr;

		private boolean outOfScopeLoggingEnabled = true;

		public boolean equals(Object other) {

			if (getClass() == other.getClass()) {

				return owlExpr.equals(((ConvertedExpression)other).owlExpr);
			}

			return false;
		}

		public int hashCode() {

			return owlExpr.hashCode();
		}

		public String toString() {

			return getClass().getSimpleName() + "[" + owlExpr + "]";
		}

		public Object getSourceObject() {

			return owlExpr;
		}

		ConvertedExpression(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			this.owlAxiom = owlAxiom;
			this.owlExpr = owlExpr;
		}

		void resetOwlExpression(OWLClassExpression owlExpr) {

			this.owlExpr = owlExpr;
		}

		OwlIndividuals asOwlIndividuals() {

			return new OwlIndividuals(owlExprAs(OWLObjectOneOf.class));
		}

		OWLAxiom getOwlAxiom() {

			return owlAxiom;
		}

		OWLClassExpression getOwlExpr() {

			return owlExpr;
		}

		boolean owlExprType(Class<? extends OWLClassExpression> type) {

			return type.isAssignableFrom(owlExpr.getClass());
		}

		<T extends OWLClassExpression>T owlExprAs(Class<T> type) {

			return owlObjectAs(owlExpr, type);
		}

		void checkLogOutOfScope() {

			if (outOfScopeLoggingEnabled) {

				Logger logger = Logger.SINGLETON;

				if (owlAxiom != null) {

					logger.logOutOfScopeAxiom(owlAxiom, owlExpr);
				}
				else {

					logger.logOutOfScopeQueryExpression(owlExpr);
				}

				outOfScopeLoggingEnabled = false;
			}
		}

		void disableOutOfScopeLogging() {

			outOfScopeLoggingEnabled = false;
		}
	}

	private class ConvertedRelation extends ConvertedExpression implements InputRelation {

		public InputRelationType getRelationType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLRestriction) {

				return getRelationType((OWLRestriction)owlExpr);
			}

			checkLogOutOfScope();

			return InputRelationType.OUT_OF_SCOPE;
		}

		public boolean hasRelationType(InputRelationType type) {

			return getRelationType() == type;
		}

		public NodeProperty getNodeProperty() {

			return names.get(owlPropertyAs(OWLObjectProperty.class));
		}

		public DataProperty getDataProperty() {

			return names.get(owlPropertyAs(OWLDataProperty.class));
		}

		public InputNode getNodeValue() {

			return new ConvertedNode(getOwlAxiom(), resolveOwlFillerToClassExpression());
		}

		public DataValue getDataValue() {

			DataValue value = getDataValueOrNull();

			if (value != null) {

				return value;
			}

			throw new RuntimeException("Invalid data-value source: " + getOwlExpr());
		}

		ConvertedRelation(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			super(owlAxiom, resolveRelationExpression(owlAxiom, owlExpr));
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

				if (owlExpr instanceof OWLDataSomeValuesFrom) {

					if (extractDataType() != null) {

						return InputRelationType.DATA_VALUE;
					}
				}
				else if (owlExpr instanceof OWLDataHasValue) {

					if (extractDataValue() != null) {

						return InputRelationType.DATA_VALUE;
					}
				}
			}

			checkLogOutOfScope();

			return InputRelationType.OUT_OF_SCOPE;
		}

		private DataValue getDataValueOrNull() {

			OWLRestriction owlRest = getOwlRestriction();

			if (owlRest instanceof OWLDataSomeValuesFrom) {

				return extractDataType();
			}

			if (owlRest instanceof OWLDataHasValue) {

				return extractDataValue();
			}

			throw new RuntimeException("Unexpected OWL-restriction type: " + owlRest);
		}

		private DataValue extractDataType() {

			return dataTypes.get(owlFillerAs(OWLDataRange.class));
		}

		private DataValue extractDataValue() {

			return DataTypeConverter.toDataValue(owlFillerAs(OWLLiteral.class));
		}

		private <P extends OWLProperty>P owlPropertyAs(Class<P> type) {

			return owlObjectAs(getOwlRestriction().getProperty(), type);
		}

		private OWLClassExpression resolveOwlFillerToClassExpression() {

			if (owlExprType(OWLObjectHasValue.class)) {

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

			return owlExprAs(OWLRestriction.class);
		}
	}

	private class ConvertedNode extends ConvertedExpression implements InputNode {

		public InputNodeType getNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (allowSimpleTypes()) {

				if (owlExpr instanceof OWLClass) {

					return InputNodeType.CLASS;
				}

				if (owlExpr instanceof OWLObjectOneOf) {

					if (asOwlIndividuals().singleInScopeIndividual()) {

						return InputNodeType.INDIVIDUAL;
					}

					checkLogOutOfScope();

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

			if (validRelationSource(owlExpr)) {

				return InputNodeType.RELATION;
			}

			checkLogOutOfScope();

			return InputNodeType.OUT_OF_SCOPE;
		}

		public ClassNode asClassNode() {

			return names.get(owlExprAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return names.get(asOwlIndividuals().asSingleOwlIndividual());
		}

		public Collection<InputNode> asConjuncts() {

			return toNodes(owlExprAs(OWLObjectIntersectionOf.class).getOperands());
		}

		public Collection<InputNode> asDisjuncts() {

			return toNodes(owlExprAs(OWLObjectUnionOf.class).getOperands());
		}

		public InputRelation asRelation() {

			ConvertedRelation rel = new ConvertedRelation(getOwlAxiom(), getOwlExpr());

			if (!rel.hasRelationType(InputRelationType.OUT_OF_SCOPE)) {

				return rel;
			}

			throw new RuntimeException("Unexpected OWL-object type: " + getOwlExpr());
		}

		ConvertedNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			super(owlAxiom, owlExpr);

			if (owlExpr instanceof OWLObjectOneOf) {

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

		private boolean validRelationSource(OWLClassExpression owlExpr) {

			ConvertedRelation rel = new ConvertedRelation(null, owlExpr);

			rel.disableOutOfScopeLogging();

			return !rel.hasRelationType(InputRelationType.OUT_OF_SCOPE);
		}

		private List<InputNode> toNodes(Set<? extends OWLClassExpression> owlExprs) {

			List<InputNode> nodes = new ArrayList<InputNode>();

			for (OWLClassExpression e : owlExprs) {

				nodes.add(new ConvertedNode(getOwlAxiom(), e));
			}

			return nodes;
		}
	}

	private class ConvertedComplexNode extends ConvertedNode implements InputComplexNode {

		public InputNode toNode() {

			return this;
		}

		public InputComplexNodeType getComplexNodeType() {

			return getNodeType().toComplexNodeType();
		}

		ConvertedComplexNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			super(owlAxiom, owlExpr);
		}

		boolean allowSimpleTypes() {

			return false;
		}
	}

	private class ConvertedComplexSuper
					extends ConvertedComplexNode
					implements InputComplexSuper {

		public InputComplexSuperType getComplexSuperType() {

			return getComplexNodeType().toComplexSuperType();
		}

		ConvertedComplexSuper(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			super(owlAxiom, owlExpr);
		}

		boolean allowConjunctions() {

			return false;
		}
	}

	ExpressionConverter(OWLDataFactory factory, MappedNames names) {

		this.factory = factory;
		this.names = names;

		noValueResolver = new NoValueOwlExpressionResolver(factory);
	}

	InputNode toNode(OWLClassExpression owlExpr) {

		return new ConvertedNode(null, owlExpr);
	}

	InputComplexNode toComplexNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

		return new ConvertedComplexNode(owlAxiom, owlExpr);
	}

	InputComplexSuper toComplexSuper(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

		return new ConvertedComplexSuper(owlAxiom, owlExpr);
	}

	InputRelation toRelation(OWLAxiom owlAxiom, OWLRestriction owlExpr) {

		return new ConvertedRelation(owlAxiom, owlExpr);
	}

	private OWLClassExpression resolveRelationExpression(
									OWLAxiom owlAxiom,
									OWLClassExpression owlExpr) {

		if (owlAxiom != null) {

			return noValueResolver.resolveAxiomExpression(owlAxiom, owlExpr);
		}

		return noValueResolver.resolveQueryExpression(owlExpr);
	}

	private <O>O owlObjectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
