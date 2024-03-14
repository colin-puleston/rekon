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
	private OWLClass owlNothing;

	private MappedNames names;

	private DataTypeConverter dataTypes = new DataTypeConverter(false);
	private NoValueConstructResolver noValueConstructResolver;

	private abstract class ConvertedExpression
								<E extends OWLClassExpression>
								implements InputExpression {

		private OWLAxiom owlAxiom;
		private E owlExpr;

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

		ConvertedExpression(OWLAxiom owlAxiom, E owlExpr) {

			this.owlAxiom = owlAxiom;
			this.owlExpr = owlExpr;
		}

		void resetOwlExpression(E owlExpr) {

			this.owlExpr = owlExpr;
		}

		OWLAxiom getOwlAxiom() {

			return owlAxiom;
		}

		E getOwlExpr() {

			return owlExpr;
		}

		boolean owlExprType(Class<? extends E> type) {

			return type.isAssignableFrom(owlExpr.getClass());
		}

		<T extends E>T owlExprAs(Class<T> type) {

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
	}

	private class ConvertedRelation
					extends ConvertedExpression<OWLRestriction>
					implements InputRelation {

		public InputRelationType getRelationType() {

			if (getOwlPropertyExpr() instanceof OWLProperty) {

				InputRelationType type = getRelationTypeForValidProperty();

				if (type != InputRelationType.OUT_OF_SCOPE) {

					return type;
				}
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

		ConvertedRelation(OWLAxiom owlAxiom, OWLRestriction owlExpr) {

			super(owlAxiom, owlExpr);
		}

		private InputRelationType getRelationTypeForValidProperty() {

			OWLRestriction owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLObjectAllValuesFrom) {

				return InputRelationType.ALL_NODES;
			}

			if (owlExpr instanceof OWLObjectHasValue) {

				return InputRelationType.SOME_NODES;
			}

			if (owlExpr instanceof OWLObjectSomeValuesFrom) {

				if (!getOwlFiller().equals(owlNothing)) {

					return InputRelationType.SOME_NODES;
				}
			}
			else if (owlExpr instanceof OWLDataSomeValuesFrom) {

				if (extractDataType() != null) {

					return InputRelationType.DATA_VALUE;
				}
			}
			else if (owlExpr instanceof OWLDataHasValue) {

				if (extractDataValue() != null) {

					return InputRelationType.DATA_VALUE;
				}
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		private DataValue getDataValueOrNull() {

			OWLRestriction owlRest = getOwlExpr();

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

			return owlObjectAs(getOwlPropertyExpr(), type);
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

		private OWLPropertyExpression getOwlPropertyExpr() {

			return getOwlExpr().getProperty();
		}

		private OWLObject getOwlFiller() {

			return owlObjectAs(getOwlExpr(), HasFiller.class).getFiller();
		}
	}

	private class ConvertedNode
					extends ConvertedExpression<OWLClassExpression>
					implements InputNode {

		private abstract class NaryBooleanHandler<E extends OWLNaryBooleanClassExpression> {

			boolean owlNothingInOperands() {

				for (OWLClassExpression e : getOperands()) {

					if (e.equals(owlNothing)) {

						return true;
					}
				}

				return false;
			}

			Collection<InputNode> operandsAsNodes() {

				List<InputNode> nodes = new ArrayList<InputNode>();

				for (OWLClassExpression e : getOperands()) {

					nodes.add(new ConvertedNode(getOwlAxiom(), e));
				}

				return nodes;
			}

			abstract Class<E> getOwlExprType();

			private Collection<OWLClassExpression> getOperands() {

				return owlExprAs(getOwlExprType()).getOperands();
			}
		}

		private class ConjunctionHandler extends NaryBooleanHandler<OWLObjectIntersectionOf> {

			Class<OWLObjectIntersectionOf> getOwlExprType() {

				return OWLObjectIntersectionOf.class;
			}
		}

		private class DisjunctionHandler extends NaryBooleanHandler<OWLObjectUnionOf> {

			Class<OWLObjectUnionOf> getOwlExprType() {

				return OWLObjectUnionOf.class;
			}
		}

		public InputNodeType getNodeType() {

			InputNodeType type = getDisjunctionOrRelationNodeType();

			if (type == InputNodeType.OUT_OF_SCOPE && !allowSuperComplexTypesOnly()) {

				type = getConjunctionNodeType();

				if (type == InputNodeType.OUT_OF_SCOPE && !allowComplexTypesOnly()) {

					type = getSimpleNodeType();
				}
			}

			if (type == InputNodeType.OUT_OF_SCOPE) {

				checkLogOutOfScope();
			}

			return type;
		}

		public ClassNode asClassNode() {

			return names.get(owlExprAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return names.get(asOwlIndividuals().asSingleOwlIndividual());
		}

		public Collection<InputNode> asConjuncts() {

			return new ConjunctionHandler().operandsAsNodes();
		}

		public Collection<InputNode> asDisjuncts() {

			return new DisjunctionHandler().operandsAsNodes();
		}

		public InputRelation asRelation() {

			return new ConvertedRelation(getOwlAxiom(), owlExprAs(OWLRestriction.class));
		}

		ConvertedNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

			super(owlAxiom, noValueConstructResolver.resolve(owlAxiom, owlExpr));

			if (owlExpr instanceof OWLObjectOneOf) {

				OwlIndividuals inds = asOwlIndividuals();

				if (inds.multiInScopeIndividuals()) {

					resetOwlExpression(inds.asOwlExpressionUnion());
				}
			}
		}

		boolean allowComplexTypesOnly() {

			return false;
		}

		boolean allowSuperComplexTypesOnly() {

			return false;
		}

		private InputNodeType getSimpleNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLClass) {

				return InputNodeType.CLASS;
			}

			if (owlExpr instanceof OWLObjectOneOf) {

				if (asOwlIndividuals().singleInScopeIndividual()) {

					return InputNodeType.INDIVIDUAL;
				}
			}

			return InputNodeType.OUT_OF_SCOPE;
		}

		private InputNodeType getConjunctionNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLObjectIntersectionOf) {

				if (!new ConjunctionHandler().owlNothingInOperands()) {

					return InputNodeType.CONJUNCTION;
				}
			}

			return InputNodeType.OUT_OF_SCOPE;
		}

		private InputNodeType getDisjunctionOrRelationNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLObjectUnionOf) {

				if (!new DisjunctionHandler().owlNothingInOperands()) {

					return InputNodeType.DISJUNCTION;
				}
			}
			else {

				if (owlExpr instanceof OWLRestriction) {

					return InputNodeType.RELATION;
				}
			}

			return InputNodeType.OUT_OF_SCOPE;
		}

		private OwlIndividuals asOwlIndividuals() {

			return new OwlIndividuals(owlExprAs(OWLObjectOneOf.class));
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

		boolean allowComplexTypesOnly() {

			return true;
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

		boolean allowSuperComplexTypesOnly() {

			return true;
		}
	}

	ExpressionConverter(OWLDataFactory factory, MappedNames names) {

		this.factory = factory;
		this.names = names;

		owlNothing = factory.getOWLNothing();
		noValueConstructResolver = new NoValueConstructResolver(factory);
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

		owlExpr = noValueConstructResolver.resolve(owlAxiom, owlExpr);

		return new ConvertedRelation(owlAxiom, owlExpr);
	}

	private <O>O owlObjectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
