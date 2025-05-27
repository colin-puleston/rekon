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
	private Set<NodeProperty> chainInvolvedProperties = new HashSet<NodeProperty>();

	private NoValueSubstitutions noValueSubstitutions;

	private abstract class ConvertedExpression
								<E extends OWLClassExpression>
								implements InputExpression {

		final OwlContainer owlContainer;

		private E owlExpr;

		class BooleanOperands<B extends OWLNaryBooleanClassExpression> {

			private B owlBool;
			private Class<B> owlBoolType;

			BooleanOperands(Class<B> owlBoolType) {

				this.owlBoolType = owlBoolType;

				owlBool = owlObjectAs(getOwlExpr(), owlBoolType);
			}

			Collection<InputNode> toNodes() {

				List<InputNode> nodes = new ArrayList<InputNode>();

				collectNodes(owlBool, nodes);

				return nodes;
			}

			private void collectNodes(B currentOwlExpr, List<InputNode> nodes) {

				for (OWLClassExpression e : currentOwlExpr.getOperands()) {

					if (owlBoolType.isAssignableFrom(e.getClass())) {

						collectNodes(owlBoolType.cast(e), nodes);
					}
					else {

						nodes.add(toReferencedNode(e));
					}
				}
			}
		}

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

		ConvertedExpression(OwlContainer owlContainer, E owlExpr) {

			this.owlContainer = owlContainer;
			this.owlExpr = owlExpr;
		}

		void resetOwlExpression(E owlExpr) {

			this.owlExpr = owlExpr;
		}

		InputNode toReferencedNode(OWLClassExpression owlExpr) {

			return new ConvertedNode(getOwlContainer(), owlExpr);
		}

		OwlContainer getOwlContainer() {

			return owlContainer;
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

			owlContainer.checkLogOutOfScope(owlExpr);
		}
	}

	private class ConvertedNode
					extends ConvertedExpression<OWLClassExpression>
					implements InputNode {

		private class Conjuncts extends BooleanOperands<OWLObjectIntersectionOf> {

			Conjuncts() {

				super(OWLObjectIntersectionOf.class);
			}
		}

		public InputNodeType getNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLClass) {

				if (!owlExpr.equals(owlNothing)) {

					return InputNodeType.CLASS;
				}
			}
			else if (owlExpr instanceof OWLObjectOneOf) {

				if (owlExprRepresentsSingleNamedIndividual()) {

					return InputNodeType.INDIVIDUAL;
				}
			}
			else {

				if (owlExpr instanceof OWLRestriction) {

					return InputNodeType.RELATION;
				}

				if (owlExpr instanceof OWLObjectIntersectionOf) {

					return InputNodeType.CONJUNCTION;
				}
			}

			checkLogOutOfScope();

			return InputNodeType.OUT_OF_SCOPE;
		}

		public ClassNode asClassNode() {

			return names.resolve(owlExprAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return names.resolve(owlExprToSingleNamedIndividual());
		}

		public Collection<InputNode> asConjuncts() {

			return new Conjuncts().toNodes();
		}

		public InputRelation asRelation() {

			return new ConvertedRelation(getOwlContainer(), owlExprAs(OWLRestriction.class));
		}

		ConvertedNode(OwlContainer owlContainer, OWLClassExpression owlExpr) {

			super(owlContainer, noValueSubstitutions.resolve(owlContainer, owlExpr));
		}

		private boolean owlExprRepresentsSingleNamedIndividual() {

			Set<OWLIndividual> inds = owlExprToIndividuals();

			return inds.size() == 1 && inds.iterator().next() instanceof OWLNamedIndividual;
		}

		private OWLNamedIndividual owlExprToSingleNamedIndividual() {

			Set<OWLIndividual> inds = owlExprToIndividuals();

			if (inds.size() != 1) {

				throw new Error("Unexpected multiple individuals: " + inds);
			}

			OWLIndividual i = inds.iterator().next();

			if (i instanceof OWLNamedIndividual) {

				return (OWLNamedIndividual)i;
			}

			throw new Error("Unexpected anonymous individual!");
		}

		private Set<OWLIndividual> owlExprToIndividuals() {

			return owlExprAs(OWLObjectOneOf.class).getIndividuals();
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

			return names.resolve(owlPropertyAs(OWLObjectProperty.class));
		}

		public DataProperty getDataProperty() {

			return names.resolve(owlPropertyAs(OWLDataProperty.class));
		}

		public InputNodeValue getNodeValue() {

			return new ConvertedNodeValue(
							getOwlContainer(),
							resolveOwlFillerToClassExpression());
		}

		public DataValue getDataValue() {

			DataValue value = getDataValueOrNull();

			if (value != null) {

				return value;
			}

			throw new RuntimeException("Invalid data-value source: " + getOwlExpr());
		}

		ConvertedRelation(OwlContainer owlContainer, OWLRestriction owlExpr) {

			super(owlContainer, owlExpr);
		}

		private InputRelationType getRelationTypeForValidProperty() {

			OWLRestriction owlExpr = getOwlExpr();

			if (someRelation(owlExpr)) {

				return InputRelationType.SOME_NODES;
			}

			if (allRelation(owlExpr)) {

				return InputRelationType.ALL_NODES;
			}

			if (dataRelation(owlExpr)) {

				return InputRelationType.DATA_VALUE;
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		private boolean someRelation(OWLRestriction owlExpr) {

			if (owlExpr instanceof OWLObjectSomeValuesFrom) {

				return checkValidSomeValuesFiller();
			}

			return owlExpr instanceof OWLObjectHasValue;
		}

		private boolean allRelation(OWLRestriction owlExpr) {

			if (owlExpr instanceof OWLObjectAllValuesFrom) {

				return checkValidAllValuesFiller();
			}

			return false;
		}

		private boolean dataRelation(OWLRestriction owlExpr) {

			if (owlExpr instanceof OWLDataSomeValuesFrom) {

				return extractDataType() != null;
			}

			if (owlExpr instanceof OWLDataHasValue) {

				return extractDataValue() != null;
			}

			return false;
		}

		private DataValue getDataValueOrNull() {

			OWLRestriction owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLDataSomeValuesFrom) {

				return extractDataType();
			}

			if (owlExpr instanceof OWLDataHasValue) {

				return extractDataValue();
			}

			throw new RuntimeException("Unexpected OWL-restriction type: " + owlExpr);
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

		private boolean checkValidSomeValuesFiller() {

			if (disjunctionFiller() && chainInvolvedProperty()) {

				checkLogInvalidDisjunctionFiller(
					DisjunctionFillerWarning.CHAIN_INVOLVED_SOME_VALUES);

				return false;
			}

			return true;
		}

		private boolean checkValidAllValuesFiller() {

			if (disjunctionFiller() && names.anyIndividuals()) {

				checkLogInvalidDisjunctionFiller(
					DisjunctionFillerWarning.ALL_VALUES_INDIVIDUALS_PRESENT);

				return false;
			}

			return true;
		}

		private void checkLogInvalidDisjunctionFiller(DisjunctionFillerWarning warning) {

			owlContainer.checkLogInvalidDisjunctionFiller(getOwlExpr(), warning);
		}

		private boolean chainInvolvedProperty() {

			return chainInvolvedProperties.contains(getNodeProperty());
		}

		private boolean disjunctionFiller() {

			return getOwlFiller() instanceof OWLObjectUnionOf;
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

	private class ConvertedNodeValue
					extends ConvertedExpression<OWLClassExpression>
					implements InputNodeValue {

		private class Disjuncts extends BooleanOperands<OWLObjectUnionOf> {

			Disjuncts() {

				super(OWLObjectUnionOf.class);
			}
		}

		public Collection<InputNode> getDisjuncts() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLObjectUnionOf) {

				return new Disjuncts().toNodes();
			}

			return Collections.singleton(toReferencedNode(owlExpr));
		}

		ConvertedNodeValue(OwlContainer owlContainer, OWLClassExpression owlExpr) {

			super(owlContainer, owlExpr);

			OWLClassExpression owlExprMod = checkNormaliseOwlExpression(owlExpr);

			if (owlExprMod != owlExpr) {

				resetOwlExpression(owlExprMod);
			}
		}

		private OWLClassExpression checkNormaliseOwlExpression(OWLClassExpression owlExpr) {

			if (owlExpr instanceof OWLObjectUnionOf) {

				return normaliseOwlUnion((OWLObjectUnionOf)owlExpr);
			}

			return owlExpr;
		}

		private OWLClassExpression normaliseOwlUnion(OWLObjectUnionOf union) {

			return new OwlUnionNormaliser(factory, union).normalise();
		}
	}

	ExpressionConverter(OWLDataFactory factory, MappedNames names) {

		this.factory = factory;
		this.names = names;

		owlNothing = factory.getOWLNothing();
		noValueSubstitutions = new NoValueSubstitutions(factory);
	}

	void addChainInvolvedProperty(NodeProperty property) {

		chainInvolvedProperties.add(property);
	}

	void addChainInvolvedProperties(Collection<NodeProperty> properties) {

		chainInvolvedProperties.addAll(properties);
	}

	InputNode toAxiomNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

		return new ConvertedNode(OwlContainer.asContainer(owlAxiom), owlExpr);
	}

	InputRelation toAxiomRelation(OWLAxiom owlAxiom, OWLRestriction owlExpr) {

		OwlContainer owlContainer = OwlContainer.asContainer(owlAxiom);

		return new ConvertedRelation(
						owlContainer,
						noValueSubstitutions.resolve(owlContainer, owlExpr));
	}

	InputNode toQueryNode(OWLClassExpression owlExpr) {

		return new ConvertedNode(OwlContainer.asContainer(owlExpr), owlExpr);
	}

	private <O>O owlObjectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
