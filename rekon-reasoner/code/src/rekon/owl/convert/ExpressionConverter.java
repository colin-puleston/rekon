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
class ExpressionConverter {

	private OWLDataFactory factory;

	private OWLClass owlNothing;
	private OWLObjectProperty owlBottomObjectProperty;
	private OWLDataProperty owlBottomDataProperty;

	private NameMapper names;

	private DataTypeConverter dataTypes = new DataTypeConverter(false);
	private Set<NodeProperty> chainedProperties = new HashSet<NodeProperty>();

	private NoValueSubstitutions noValueSubstitutions;

	private abstract class ConvertedExpression
								<E extends OWLClassExpression>
								implements InputExpression {

		final OwlContainer owlContainer;

		private E owlExpr;
		private OutOfScopeExplanation outOfScopeExplanation = OutOfScopeExplanation.INVALID_EXPRESSION_TYPE;

		abstract class BooleanOperands<B extends OWLNaryBooleanClassExpression> {

			private B owlBool;

			BooleanOperands(B owlBool) {

				this.owlBool = owlBool;
			}

			Collection<InputNode> toNodes() {

				List<InputNode> nodes = new ArrayList<InputNode>();

				collectNodes(owlBool, nodes);

				return nodes;
			}

			abstract Class<B> getOwlBoolType();

			private void collectNodes(B currentOwlExpr, List<InputNode> nodes) {

				Class<B> owlBoolType = getOwlBoolType();

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

		void setOutOfScopeSubExpression(OutOfScopeExplanation explanation) {

			outOfScopeExplanation = explanation;
		}

		void checkLogOutOfScope() {

			owlContainer.checkLogOutOfScope(owlExpr, resolveOutOfScopeExplanation());
		}

		private OutOfScopeExplanation resolveOutOfScopeExplanation() {

			OutOfScopeExplanation explanation = outOfScopeExplanation;

			outOfScopeExplanation = OutOfScopeExplanation.INVALID_EXPRESSION_TYPE;

			return explanation;
		}
	}

	private class ConvertedNode
					extends ConvertedExpression<OWLClassExpression>
					implements InputNode {

		private class Conjuncts extends BooleanOperands<OWLObjectIntersectionOf> {

			Conjuncts() {

				super(owlExprAs(OWLObjectIntersectionOf.class));
			}

			Class<OWLObjectIntersectionOf> getOwlBoolType() {

				return OWLObjectIntersectionOf.class;
			}
		}

		public InputNodeType getNodeType() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLClass) {

				if (!owlExpr.equals(owlNothing)) {

					return InputNodeType.CLASS;
				}

				setOutOfScopeSubExpression(OutOfScopeExplanation.INVALID_INBUILT_ENTITY);
			}
			else if (owlExpr instanceof OWLObjectOneOf) {

				if (owlExprRepresentsSingleNamedIndividual()) {

					return InputNodeType.INDIVIDUAL;
				}

				setOutOfScopeSubExpression(OutOfScopeExplanation.INVALID_ONEOF_VALUE);
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

				InputRelationType type = getRelationTypeForSimpleProperty();

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

		private InputRelationType getRelationTypeForSimpleProperty() {

			OWLRestriction owlExpr = getOwlExpr();

			if (checkNodeRelation(owlExpr)) {

				if (checkNonBottomProperty(owlExpr, owlBottomObjectProperty)) {

					return InputRelationType.NODE_VALUED;
				}
			}
			else if (dataRelation(owlExpr)) {

				if (checkNonBottomProperty(owlExpr, owlBottomDataProperty)) {

					return InputRelationType.DATA_VALUED;
				}
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		private boolean checkNodeRelation(OWLRestriction owlExpr) {

			if (owlExpr instanceof OWLObjectSomeValuesFrom) {

				return checkInScopeFillerNode();
			}

			return owlExpr instanceof OWLObjectHasValue;
		}

		private boolean checkNonBottomProperty(OWLRestriction owlExpr, OWLProperty owlBottomProperty) {

			if (owlPropertyAs(OWLProperty.class).equals(owlBottomProperty)) {

				setOutOfScopeSubExpression(OutOfScopeExplanation.INVALID_INBUILT_ENTITY);

				return false;
			}

			return true;
		}

		private boolean dataRelation(OWLRestriction owlExpr) {

			if (owlExpr instanceof OWLDataSomeValuesFrom) {

				return validDataType();
			}

			if (owlExpr instanceof OWLDataHasValue) {

				return validDataValue();
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

		private boolean validDataType() {

			DataValue v = extractDataType();

			if (v == null) {

				setOutOfScopeSubExpression(OutOfScopeExplanation.INVALID_DATATYPE_VALUE);

				return false;
			}

			return true;
		}

		private boolean validDataValue() {

			DataValue v = extractDataValue();

			if (v == null) {

				setOutOfScopeSubExpression(OutOfScopeExplanation.INVALID_DATA_VALUE);

				return false;
			}

			return true;
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

		private boolean checkInScopeFillerNode() {

			if (disjunctionFiller() && chainedPropertyOrSubProperty()) {

				setOutOfScopeSubExpression(OutOfScopeExplanation.DISJUNCTION_FILLER_FOR_CHAINED_PROPERTY);

				return false;
			}

			return true;
		}

		private boolean chainedPropertyOrSubProperty() {

			NodeProperty p = getNodeProperty();

			for (NodeProperty cp : chainedProperties) {

				if (cp.subsumes(p)) {

					return true;
				}
			}

			return false;
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

				super(owlExprAs(OWLObjectUnionOf.class));
			}

			Class<OWLObjectUnionOf> getOwlBoolType() {

				return OWLObjectUnionOf.class;
			}
		}

		public Collection<InputNode> getDisjuncts() {

			OWLClassExpression owlExpr = getOwlExpr();

			if (owlExpr instanceof OWLObjectUnionOf) {

				owlExpr = normaliseOwlUnion((OWLObjectUnionOf)owlExpr);
			}

			if (owlExpr instanceof OWLObjectUnionOf) {

				return new Disjuncts().toNodes();
			}

			return Collections.singleton(toReferencedNode(owlExpr));
		}

		ConvertedNodeValue(OwlContainer owlContainer, OWLClassExpression owlExpr) {

			super(owlContainer, owlExpr);
		}

		private OWLClassExpression normaliseOwlUnion(OWLObjectUnionOf union) {

			return new OwlUnionNormaliser(factory, union).normalise();
		}
	}

	ExpressionConverter(OWLDataFactory factory, NameMapper names) {

		this.factory = factory;
		this.names = names;

		owlNothing = factory.getOWLNothing();
		owlBottomObjectProperty = factory.getOWLBottomObjectProperty();
		owlBottomDataProperty = factory.getOWLBottomDataProperty();

		noValueSubstitutions = new NoValueSubstitutions(factory);
	}

	void addChainedProperty(NodeProperty property) {

		chainedProperties.add(property);
	}

	void addChainedProperties(Collection<NodeProperty> properties) {

		chainedProperties.addAll(properties);
	}

	InputNode toAxiomNode(OWLAxiom owlAxiom, OWLClassExpression owlExpr) {

		return new ConvertedNode(OwlContainer.asContainer(owlAxiom), owlExpr);
	}

	InputRelation toAxiomRelation(OWLObjectPropertyAssertionAxiom owlAxiom) {

		return toAxiomRelation(owlAxiom, toOwlRestriction(owlAxiom));
	}

	InputRelation toAxiomRelation(OWLDataPropertyAssertionAxiom owlAxiom) {

		return toAxiomRelation(owlAxiom, toOwlRestriction(owlAxiom));
	}

	InputNode toQueryNode(OWLClassExpression owlExpr) {

		return new ConvertedNode(OwlContainer.asContainer(owlExpr), owlExpr);
	}

	private InputRelation toAxiomRelation(OWLAxiom owlAxiom, OWLRestriction owlExpr) {

		return new ConvertedRelation(OwlContainer.asContainer(owlAxiom), owlExpr);
	}

	private OWLRestriction toOwlRestriction(OWLObjectPropertyAssertionAxiom owlAxiom) {

		return factory.getOWLObjectHasValue(owlAxiom.getProperty(), owlAxiom.getObject());
	}

	private OWLRestriction toOwlRestriction(OWLDataPropertyAssertionAxiom owlAxiom) {

		return factory.getOWLDataHasValue(owlAxiom.getProperty(), owlAxiom.getObject());
	}

	private <O>O owlObjectAs(OWLObject obj, Class<O> type) {

		if (type.isAssignableFrom(obj.getClass())) {

			return type.cast(obj);
		}

		throw new RuntimeException("Unexpected object type: " + obj.getClass());
	}
}
