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
import rekon.build.*;

/**
 * @author Colin Puleston
 */
class OwlInputObjects {

	private OWLDataFactory factory;

	private MappedNames mappedNames;
	private DataTypes dataTypes = new DataTypes(false);

	private abstract class OwlInputObject<O extends OWLObject> implements InputObject {

		private O owlObject;

		public boolean equals(Object other) {

			if (getClass() == other.getClass()) {

				return owlObject.equals(((OwlInputObject<?>)other).owlObject);
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

		OwlInputObject(O owlObject) {

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

	private abstract class OwlInputName
								<N extends Name, O extends OWLEntity>
								extends OwlInputObject<O>
								implements InputName<N> {

		private Set<O> owlEquivs = new HashSet<O>();

		public N getName() {

			return toOwlInputName(getOwlObject());
		}

		public Collection<N> getEquivs() {

			return toOwlInputNames(owlEquivs);
		}

		OwlInputName(O owlEntity) {

			super(owlEntity);
		}

		void addEquiv(O owlEquiv) {

			if (!owlEquiv.equals(getOwlObject())) {

				owlEquivs.add(owlEquiv);
			}
		}

		List<N> toOwlInputNames(Collection<O> owlEntities) {

			List<N> entities = new ArrayList<N>();

			for (O oe : owlEntities) {

				entities.add(toOwlInputName(oe));
			}

			return entities;
		}

		abstract N toOwlInputName(O owlEntity);
	}

	private abstract class OwlInputHierarchyName
								<N extends Name, O extends OWLEntity>
								extends OwlInputName<N, O>
								implements InputHierarchyName<N> {

		private Set<O> owlSupers = new HashSet<O>();

		public Collection<N> getSupers() {

			return toOwlInputNames(owlSupers);
		}

		OwlInputHierarchyName(O owlEntity) {

			super(owlEntity);
		}

		void addSuper(O owlSuper) {

			if (!owlSuper.equals(getOwlObject())) {

				owlSupers.add(owlSuper);
			}
		}
	}

	private class OwlInputClass
						extends OwlInputHierarchyName<ClassNode, OWLClass>
						implements InputClass  {

		private Set<OWLClassExpression> owlEquivExprs = new HashSet<OWLClassExpression>();
		private Set<OWLClassExpression> superAtomicExprs = new HashSet<OWLClassExpression>();
		private Set<OWLObjectUnionOf> superUnionExprs = new HashSet<OWLObjectUnionOf>();

		public ClassNode getClassNode() {

			return getName();
		}

		public Collection<InputExpression> getEquivExprs() {

			return toInputExprs(owlEquivExprs);
		}

		public Collection<InputExpression> getSuperAtomicExprs() {

			return toInputExprs(superAtomicExprs);
		}

		public Collection<InputExpression> getSuperDisjunctionExprs() {

			return toInputExprs(superUnionExprs);
		}

		OwlInputClass(OWLClass owlEntity) {

			super(owlEntity);
		}

		void addEquivExpr(OWLClassExpression owlEquiv) {

			if (owlEquiv instanceof OWLClass) {

				addEquiv((OWLClass)owlEquiv);
			}
			else {

				owlEquivExprs.add(owlEquiv);

				addConjunctClassesAsSupers(owlEquiv);
			}
		}

		void addSuperExpr(OWLClassExpression sup) {

			if (sup instanceof OWLClass) {

				addSuper((OWLClass)sup);
			}
			else if (sup instanceof OWLObjectIntersectionOf) {

				addSuperIntersection((OWLObjectIntersectionOf)sup);
			}
			else if (sup instanceof OWLObjectUnionOf) {

				superUnionExprs.add((OWLObjectUnionOf)sup);
			}
			else {

				superAtomicExprs.add(sup);
			}
		}

		ClassNode toOwlInputName(OWLClass owlEntity) {

			return mappedNames.get(owlEntity);
		}

		private void addSuperIntersection(OWLObjectIntersectionOf supInt) {

			for (OWLClassExpression sup : supInt.getOperands()) {

				addSuperExpr(sup);
			}
		}

		private void addConjunctClassesAsSupers(OWLClassExpression owlExpr) {

			if (owlExpr instanceof OWLObjectIntersectionOf) {

				addConjunctClassesAsSupers(((OWLObjectIntersectionOf)owlExpr).getOperands());
			}
		}

		private void addConjunctClassesAsSupers(Set<OWLClassExpression> owlExprs) {

			for (OWLClassExpression e : owlExprs) {

				if (e instanceof OWLClass) {

					addSuper((OWLClass)e);
				}
				else if (e instanceof OWLObjectIntersectionOf) {

					addConjunctClassesAsSupers(e);
				}
			}
		}
	}

	private class OwlInputObjectProperty
						extends OwlInputHierarchyName<NodeProperty, OWLObjectProperty>
						implements InputObjectProperty  {

		private Set<OWLObjectProperty> owlInverses = new HashSet<OWLObjectProperty>();
		private Set<List<OWLObjectProperty>> owlChains = new HashSet<List<OWLObjectProperty>>();

		private boolean transitive = false;
		private boolean symmetric = false;

		public Collection<NodeProperty> getInverses() {

			return toOwlInputNames(owlInverses);
		}

		public Collection<List<NodeProperty>> getChains() {

			List<List<NodeProperty>> chains = new ArrayList<List<NodeProperty>>();

			for (List<OWLObjectProperty> c : owlChains) {

				chains.add(toOwlInputNames(c));
			}

			return chains;
		}

		public boolean transitive() {

			return transitive;
		}

		public boolean symmetric() {

			return symmetric;
		}

		OwlInputObjectProperty(OWLObjectProperty owlEntity) {

			super(owlEntity);
		}

		void addInverses(Collection<OWLObjectProperty> owlInvs) {

			owlInvs.remove(getOwlObject());

			owlInverses.addAll(owlInvs);
		}

		void addChain(List<OWLObjectProperty> owlChain) {

			owlChains.add(owlChain);
		}

		void setTransitive() {

			transitive = true;
		}

		void setSymmetric() {

			symmetric = true;
		}

		NodeProperty toOwlInputName(OWLObjectProperty owlEntity) {

			return mappedNames.get(owlEntity);
		}
	}

	private class OwlInputDataProperty
						extends OwlInputHierarchyName<DataProperty, OWLDataProperty>
						implements InputDataProperty  {

		OwlInputDataProperty(OWLDataProperty owlEntity) {

			super(owlEntity);
		}

		DataProperty toOwlInputName(OWLDataProperty owlEntity) {

			return mappedNames.get(owlEntity);
		}
	}

	private class OwlInputIndividual
						extends OwlInputName<IndividualNode, OWLNamedIndividual>
						implements InputIndividual  {

		private Set<OWLClass> owlTypes = new HashSet<OWLClass>();
		private Set<OWLClassExpression> owlTypeExprs = new HashSet<OWLClassExpression>();

		private Set<InputRelation> relations = new HashSet<InputRelation>();

		public IndividualNode getIndividualNode() {

			return getName();
		}

		public Collection<ClassNode> getTypes() {

			List<ClassNode> types = new ArrayList<ClassNode>();

			for (OWLClass ot : owlTypes) {

				types.add(mappedNames.get(ot));
			}

			return types;
		}

		public Collection<InputExpression> getTypeExprs() {

			return toInputExprs(owlTypeExprs);
		}

		public Collection<InputRelation> getRelations() {

			return relations;
		}

		OwlInputIndividual(OWLNamedIndividual owlEntity) {

			super(owlEntity);
		}

		void addTypeExpr(OWLClassExpression owlType) {

			if (owlType instanceof OWLClass) {

				owlTypes.add((OWLClass)owlType);
			}
			else {

				owlTypeExprs.add(owlType);
			}
		}

		void addRelation(InputRelation relation) {

			relations.add(relation);
		}

		IndividualNode toOwlInputName(OWLNamedIndividual owlEntity) {

			return mappedNames.get(owlEntity);
		}
	}

	private class OwlInputExpression
						extends OwlInputObject<OWLClassExpression>
						implements InputExpression  {

		public InputExpressionType getExpressionType() {

			OWLClassExpression owlExpr = getOwlObject();

			if (owlExpr instanceof OWLClass) {

				return InputExpressionType.CLASS;
			}

			if (owlExpr instanceof OWLObjectOneOf) {

				if (anyAnonymousIndividuals((OWLObjectOneOf)owlExpr)) {

					return InputExpressionType.OUT_OF_SCOPE;
				}

				return InputExpressionType.INDIVIDUAL;
			}

			if (owlExpr instanceof OWLObjectIntersectionOf) {

				return InputExpressionType.CONJUNCTION;
			}

			if (owlExpr instanceof OWLObjectUnionOf) {

				return InputExpressionType.DISJUNCTION;
			}

			if (owlExpr instanceof OWLObjectComplementOf) {

				return InputExpressionType.COMPLEMENT;
			}

			if (owlExpr instanceof OWLRestriction) {

				if (!validRelationType((OWLRestriction)owlExpr)) {

					return InputExpressionType.OUT_OF_SCOPE;
				}

				return InputExpressionType.RELATION;
			}

			return InputExpressionType.OUT_OF_SCOPE;
		}

		public ClassNode asClassNode() {

			return mappedNames.get(owlObjectAs(OWLClass.class));
		}

		public IndividualNode asIndividualNode() {

			return mappedNames.get(extractNamedOwlIndividual());
		}

		public Collection<InputExpression> asConjuncts() {

			return toInputExprs(owlObjectAs(OWLObjectIntersectionOf.class).getOperands());
		}

		public Collection<InputExpression> asDisjuncts() {

			return toInputExprs(owlObjectAs(OWLObjectUnionOf.class).getOperands());
		}

		public InputExpression asComplemented() {

			OWLObjectComplementOf c = owlObjectAs(OWLObjectComplementOf.class);

			return new OwlInputExpression(c.getOperand());
		}

		public InputRelation asRelation() {

			return new OwlInputRelation(owlObjectAs(OWLRestriction.class));
		}

		OwlInputExpression(OWLClassExpression owlExpression) {

			super(owlExpression);

			checkResolveIndividualsDisjunction();
		}

		private boolean validRelationType(OWLRestriction owlRest) {

			OwlInputRelation rel = new OwlInputRelation(owlRest);

			return rel.getRelationType() != InputRelationType.OUT_OF_SCOPE;
		}

		private boolean anyAnonymousIndividuals(OWLObjectOneOf owlExpr) {

			for (OWLIndividual i : owlExpr.getIndividuals()) {

				if (i instanceof OWLNamedIndividual) {

					return false;
				}
			}

			return true;
		}

		private void checkResolveIndividualsDisjunction() {

			if (owlObjectType(OWLObjectOneOf.class)) {

				OWLObjectOneOf oo = owlObjectAs(OWLObjectOneOf.class);

				if (oo.getIndividuals().size() > 1) {

					resetOwlObject((OWLObjectUnionOf)oo.asObjectUnionOf());
				}
			}
		}

		private OWLNamedIndividual extractNamedOwlIndividual() {

			OWLObjectOneOf oo = owlObjectAs(OWLObjectOneOf.class);
			Set<OWLIndividual> inds = oo.getIndividuals();

			if (inds.size() != 1) {

				throw new Error("Unexpected multiple individuals!");
			}

			OWLIndividual i = inds.iterator().next();

			if (i instanceof OWLNamedIndividual) {

				return (OWLNamedIndividual)i;
			}

			throw new RuntimeException("Unexpected anonymous individual!");
		}
	}

	private class OwlInputRelation
						extends OwlInputObject<OWLRestriction>
						implements InputRelation  {

		public InputRelationType getRelationType() {

			OWLRestriction owlRest = getOwlObject();

			if (owlRest instanceof HasFiller<?>
				&& owlRest.getProperty() instanceof OWLProperty) {

				if (owlRest instanceof OWLObjectSomeValuesFrom) {

					return InputRelationType.SOME_NODES;
				}

				if (owlRest instanceof OWLObjectAllValuesFrom) {

					return InputRelationType.All_NODES;
				}

				if (owlRest instanceof OWLObjectHasValue) {

					return InputRelationType.SOME_NODES;
				}

				if (owlRest instanceof OWLDataRestriction) {

					return InputRelationType.DATA_VALUE;
				}
			}

			return InputRelationType.OUT_OF_SCOPE;
		}

		public NodeProperty getNodeProperty() {

			return mappedNames.get(owlPropertyAs(OWLObjectProperty.class));
		}

		public DataProperty getDataProperty() {

			return mappedNames.get(owlPropertyAs(OWLDataProperty.class));
		}

		public InputExpression getExpressionValue() {

			return new OwlInputExpression(resolveOwlFillerToClassExpression());
		}

		public DataValue getDataValue() {

			OWLRestriction owlRest = getOwlObject();

			if (owlRest instanceof OWLQuantifiedDataRestriction) {

				return extractDataType();
			}

			if (owlRest instanceof OWLDataHasValue) {

				return extractDataValue();
			}

			throw new RuntimeException(
						"Unexpected OWLDataRestriction type: "
						+ owlRest.getClass());
		}

		OwlInputRelation(OWLRestriction owlRestriction) {

			super(owlRestriction);
		}

		OwlInputRelation(OWLObjectPropertyAssertionAxiom owlAxiom) {

			super(factory.getOWLObjectHasValue(owlAxiom.getProperty(), owlAxiom.getObject()));
		}

		OwlInputRelation(OWLDataPropertyAssertionAxiom owlAxiom) {

			super(factory.getOWLDataHasValue(owlAxiom.getProperty(), owlAxiom.getObject()));
		}

		private DataValue extractDataType() {

			return dataTypes.get(owlFillerAs(OWLDatatypeRestriction.class));
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

	private class OwlInputEquivalence
						extends OwlInputObject<OWLEquivalentClassesAxiom>
						implements InputEquivalence {

		public Collection<InputExpression> getEquivs() {

			return toInputExprs(getOwlObject().getClassExpressions());
		}

		OwlInputEquivalence(OWLEquivalentClassesAxiom owlAxiom) {

			super(owlAxiom);
		}
	}

	private class OwlInputSubSuper
						extends OwlInputObject<OWLSubClassOfAxiom>
						implements InputSubSuper {

		public InputExpression getSuper() {

			return new OwlInputExpression(getOwlObject().getSuperClass());
		}

		public InputExpression getSub() {

			return new OwlInputExpression(getOwlObject().getSubClass());
		}

		OwlInputSubSuper(OWLSubClassOfAxiom owlAxiom) {

			super(owlAxiom);
		}
	}

	OwlInputObjects(OWLDataFactory factory, MappedNames mappedNames) {

		this.factory = factory;
		this.mappedNames = mappedNames;
	}

	InputClass createClass(OWLClass owlEntity) {

		return new OwlInputClass(owlEntity);
	}

	InputObjectProperty createObjectProperty(OWLObjectProperty owlEntity) {

		return new OwlInputObjectProperty(owlEntity);
	}

	InputDataProperty createDataProperty(OWLDataProperty owlEntity) {

		return new OwlInputDataProperty(owlEntity);
	}

	InputIndividual createIndividual(OWLNamedIndividual owlEntity) {

		return new OwlInputIndividual(owlEntity);
	}

	InputExpression createExpression(OWLClassExpression owlExpression) {

		return new OwlInputExpression(owlExpression);
	}

	InputRelation createRelation(OWLRestriction owlRestriction) {

		return new OwlInputRelation(owlRestriction);
	}

	InputEquivalence createEquivalence(OWLEquivalentClassesAxiom owlAxiom) {

		return new OwlInputEquivalence(owlAxiom);
	}

	InputSubSuper createSubSuper(OWLSubClassOfAxiom owlAxiom) {

		return new OwlInputSubSuper(owlAxiom);
	}

	void addEquivExpr(InputClass c, OWLClassExpression owlEquiv) {

		((OwlInputClass)c).addEquivExpr(owlEquiv);
	}

	void addSuperExpr(InputClass c, OWLClassExpression owlSup) {

		((OwlInputClass)c).addSuperExpr(owlSup);
	}

	void addEquiv(InputObjectProperty p, OWLObjectProperty owlEquiv) {

		((OwlInputObjectProperty)p).addEquiv(owlEquiv);
	}

	void addSuper(InputObjectProperty p, OWLObjectProperty owlSup) {

		((OwlInputObjectProperty)p).addSuper(owlSup);
	}

	void addInverses(InputObjectProperty p, Collection<OWLObjectProperty> owlInverses) {

		((OwlInputObjectProperty)p).addInverses(owlInverses);
	}

	void addChain(InputObjectProperty p, List<OWLObjectProperty> owlChain) {

		((OwlInputObjectProperty)p).addChain(owlChain);
	}

	void setTransitive(InputObjectProperty p) {

		((OwlInputObjectProperty)p).setTransitive();
	}

	void setSymmetric(InputObjectProperty p) {

		((OwlInputObjectProperty)p).setSymmetric();
	}

	void addEquiv(InputDataProperty p, OWLDataProperty owlEquiv) {

		((OwlInputDataProperty)p).addEquiv(owlEquiv);
	}

	void addSuper(InputDataProperty p, OWLDataProperty owlSup) {

		((OwlInputDataProperty)p).addSuper(owlSup);
	}

	void addEquiv(InputIndividual i, OWLNamedIndividual owlEquiv) {

		((OwlInputIndividual)i).addEquiv(owlEquiv);
	}

	void addTypeExpr(InputIndividual i, OWLClassExpression owlType) {

		((OwlInputIndividual)i).addTypeExpr(owlType);
	}

	void addRelation(InputIndividual i, OWLObjectPropertyAssertionAxiom owlAxiom) {

		((OwlInputIndividual)i).addRelation(new OwlInputRelation(owlAxiom));
	}

	void addRelation(InputIndividual i, OWLDataPropertyAssertionAxiom owlAxiom) {

		((OwlInputIndividual)i).addRelation(new OwlInputRelation(owlAxiom));
	}

	private List<InputExpression> toInputExprs(Set<? extends OWLClassExpression> owlExprs) {

		List<InputExpression> exprs = new ArrayList<InputExpression>();

		for (OWLClassExpression oe : owlExprs) {

			exprs.add(new OwlInputExpression(oe));
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
