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

/**
 * @author Colin Puleston
 */
class MatchComponents {

	private MappedNames mappedNames;
	private MatchStructures matchStructures;

	private NodeName rootNodeName;

	private NodePatterns nodePatterns = new NodePatterns();
	private Disjunctions disjunctions = new Disjunctions();

	private SomeRelations someRelations = new SomeRelations();
	private AllRelations allRelations = new AllRelations();
	private DataRelations dataRelations = new DataRelations();

	private ObjectValueRelations objectValueRelations = new ObjectValueRelations();
	private DataValueRelations dataValueRelations = new DataValueRelations();

	private DataTypes dataTypes;

	private Map<OWLClassExpression, ClassName> patternClasses = new HashMap<OWLClassExpression, ClassName>();

	private boolean dynamic;

	private abstract class TypeEntities<S, E> {

		private Map<S, E> bySource = new HashMap<S, E>();

		E get(S source) {

			return dynamic ? checkCreate(source) : getViaCache(source);
		}

		Collection<E> getAll() {

			return bySource.values();
		}

		abstract E checkCreate(S source);

		private E getViaCache(S source) {

			E expr = bySource.get(source);

			if (expr == null) {

				expr = checkCreate(source);

				if (expr != null) {

					bySource.put(source, expr);
				}
			}

			return expr;
		}
	}

	private class NodePatterns extends TypeEntities<NodePatternSpec, NodePattern> {

		NodePattern get(OWLClassExpression source) {

			return get(new NodePatternSpec(source));
		}

		NodePattern checkCreate(NodePatternSpec source) {

			List<OWLClassExpression> conjuncts = source.getConjuncts();

			if (conjuncts.size() == 1) {

				return checkCreate(conjuncts.get(0));
			}

			return checkCreateForConjuncts(conjuncts);
		}

		private NodePattern checkCreate(OWLClassExpression source) {

			if (source instanceof OWLClass) {

				return new NodePattern(mappedNames.get((OWLClass)source));
			}

			if (source instanceof OWLObjectIntersectionOf) {

				return checkCreateForConjuncts(((OWLObjectIntersectionOf)source).getOperands());
			}

			if (source instanceof OWLObjectOneOf) {

				return checkCreateForIndividuals(((OWLObjectOneOf)source).getIndividuals());
			}

			if (source instanceof OWLRestriction) {

				return checkCreateForRestriction((OWLRestriction)source);
			}

			return null;
		}

		private NodePattern checkCreateForConjuncts(Collection<OWLClassExpression> conjuncts) {

			NameSet names = new NameSet();
			Set<Relation> rels = new HashSet<Relation>();

			for (OWLClassExpression op : conjuncts) {

				if (op instanceof OWLClass) {

					names.absorb(mappedNames.get((OWLClass)op));
				}
				else {

					Relation r = toRelation(op);

					if (r == null) {

						return null;
					}

					rels.add(r);
				}
			}

			if (names.isEmpty()) {

				names.add(rootNodeName);
			}

			return new NodePattern(names, rels);
		}

		private NodePattern checkCreateForIndividuals(Collection<OWLIndividual> individuals) {

			if (individuals.size() == 1) {

				OWLIndividual ind = individuals.iterator().next();

				if (ind instanceof OWLNamedIndividual) {

					return new NodePattern(mappedNames.get((OWLNamedIndividual)ind));
				}
			}

			return null;
		}

		private NodePattern checkCreateForRestriction(OWLRestriction source) {

			Relation r = toRelation(source);

			return r != null ? new NodePattern(rootNodeName, r) : null;
		}
	}

	private class Disjunctions extends TypeEntities<OWLObjectUnionOf, NodeValue> {

		NodeValue checkCreate(OWLObjectUnionOf source) {

			Set<ClassName> disjuncts = new HashSet<ClassName>();

			for (OWLClassExpression op : source.getOperands()) {

				ClassName n = valueToClassName(op);

				if (n == null) {

					return null;
				}

				disjuncts.add(n);
			}

			return new NodeValue(disjuncts);
		}
	}

	private abstract class ObjectRelations
								<S extends OWLQuantifiedObjectRestriction>
								extends TypeEntities<S, Relation> {

		private boolean complement = false;

		Relation get(S source, boolean complement) {

			this.complement = complement;

			return get(source);
		}

		Relation checkCreate(S source) {

			OWLObjectPropertyExpression propExpr = source.getProperty();

			if (propExpr instanceof OWLObjectProperty) {

				ObjectPropertyName prop = mappedNames.get((OWLObjectProperty)propExpr);
				OWLClassExpression filler = source.getFiller();

				if (complement) {

					return checkCreateForComplement(prop, filler);
				}

				return checkCreate(prop, filler);
			}

			return null;
		}

		Relation checkCreate(ObjectPropertyName prop, OWLClassExpression filler) {

			ObjectValue target = toTarget(filler);

			return target != null ? create(prop, target) : null;
		}

		abstract Relation checkCreateForComplement(
								ObjectPropertyName prop,
								OWLClassExpression filler);

		abstract Relation create(ObjectPropertyName prop, ObjectValue target);

		Relation createForNoValue(ObjectPropertyName prop) {

			return new AllRelation(prop, Nothing.SINGLETON);
		}

		private ObjectValue toTarget(OWLClassExpression filler) {

			if (filler instanceof OWLObjectUnionOf) {

				return disjunctions.get((OWLObjectUnionOf)filler);
			}

			ClassName n = valueToClassName(filler);

			return n != null ? new NodeValue(n) : null;
		}
	}

	private class SomeRelations extends ObjectRelations<OWLObjectSomeValuesFrom> {

		Relation checkCreateForComplement(ObjectPropertyName prop, OWLClassExpression filler) {

			return filler.isOWLThing() ? createForNoValue(prop) : null;
		}

		Relation create(ObjectPropertyName prop, ObjectValue target) {

			return new SomeRelation(prop, target);
		}
	}

	private class AllRelations extends ObjectRelations<OWLObjectAllValuesFrom> {

		Relation checkCreate(ObjectPropertyName prop, OWLClassExpression filler) {

			return filler.isOWLNothing()
						? createForNoValue(prop)
						: super.checkCreate(prop, filler);
		}

		Relation checkCreateForComplement(ObjectPropertyName prop, OWLClassExpression filler) {

			return null;
		}

		Relation create(ObjectPropertyName prop, ObjectValue target) {

			return new AllRelation(prop, target);
		}
	}

	private class DataRelations extends TypeEntities<OWLDataRestriction, Relation> {

		Relation checkCreate(OWLDataRestriction source) {

			OWLDataPropertyExpression propExpr = source.getProperty();

			if (propExpr instanceof OWLDataProperty) {

				DataValue v = toDataValue(source);

				if (v != null) {

					return create((OWLDataProperty)propExpr, v);
				}
			}

			return null;
		}

		private Relation create(OWLDataProperty prop, DataValue value) {

			return new DataRelation(mappedNames.get(prop), value);
		}
	}

	private class ObjectValueRelations extends TypeEntities<AssertedObjectValue, Relation> {

		Relation checkCreate(AssertedObjectValue source) {

			OWLIndividual v = source.getValue();

			if (v instanceof OWLNamedIndividual) {

				return create(source.getProperty(), (OWLNamedIndividual)v);
			}

			return null;
		}

		private Relation create(OWLObjectProperty prop, OWLNamedIndividual value) {

			return new SomeRelation(mappedNames.get(prop), toTarget(value));
		}

		private NodeValue toTarget(OWLNamedIndividual value) {

			return new NodeValue(mappedNames.get(value));
		}
	}

	private class DataValueRelations extends TypeEntities<AssertedDataValue, Relation> {

		Relation checkCreate(AssertedDataValue source) {

			DataValue target = DataTypes.toDataValueExpression(source.getValue());

			return target != null ? create(source.getProperty(), target) : null;
		}

		private Relation create(OWLDataProperty prop, DataValue target) {

			return new DataRelation(mappedNames.get(prop), target);
		}
	}

	private class NodePatternSpec {

		private List<OWLClassExpression> conjuncts = new ArrayList<OWLClassExpression>();

		public boolean equals(Object other) {

			return conjuncts.equals(((NodePatternSpec)other).conjuncts);
		}

		public int hashCode() {

			return conjuncts.hashCode();
		}

		NodePatternSpec() {
		}

		NodePatternSpec(OWLClassExpression source) {

			expandFor(source);
		}

		void expandFor(OWLClassExpression source) {

			if (source instanceof OWLObjectIntersectionOf) {

				conjuncts.addAll(((OWLObjectIntersectionOf)source).getOperands());
			}
			else {

				conjuncts.add(source);
			}
		}

		NodePatternSpec copyAndExpandFor(OWLClassExpression source) {

			NodePatternSpec copy = new NodePatternSpec(conjuncts);

			copy.expandFor(source);

			return copy;
		}

		List<OWLClassExpression> getConjuncts() {

			return conjuncts;
		}

		private NodePatternSpec(List<OWLClassExpression> conjuncts) {

			this.conjuncts.addAll(conjuncts);
		}
	}

	private class NodePatternDisjunctionSpec {

		private List<NodePatternSpec> disjuncts = new ArrayList<NodePatternSpec>();

		NodePatternDisjunctionSpec(OWLClassExpression source) {

			if (source instanceof OWLObjectIntersectionOf) {

				addFor((OWLObjectIntersectionOf)source);
			}
			else if (source instanceof OWLObjectUnionOf) {

				addFor((OWLObjectUnionOf)source);
			}
			else {

				addSimpleDisjunctFor(source);
			}
		}

		List<NodePattern> checkCreate() {

			List<NodePattern> patternDisjuncts = new ArrayList<NodePattern>();

			for (NodePatternSpec d : disjuncts) {

				NodePattern pd = nodePatterns.get(d);

				if (pd == null) {

					return null;
				}

				patternDisjuncts.add(pd);
			}

			return patternDisjuncts;
		}

		private void addFor(OWLObjectIntersectionOf source) {

			NodePatternSpec disjunct = new NodePatternSpec();
			List<OWLObjectUnionOf> unions = new ArrayList<OWLObjectUnionOf>();

			for (OWLClassExpression op : source.getOperands()) {

				if (op instanceof OWLObjectUnionOf) {

					unions.add((OWLObjectUnionOf)op);
				}
				else {

					disjunct.expandFor(op);
				}
			}

			addForUnions(disjunct, unions, 0);
		}

		private void addFor(OWLObjectUnionOf source) {

			for (OWLClassExpression op : source.getOperands()) {

				addSimpleDisjunctFor(op);
			}
		}

		private void addSimpleDisjunctFor(OWLClassExpression source) {

			disjuncts.add(new NodePatternSpec(source));
		}

		private void addForUnions(NodePatternSpec disjunct, List<OWLObjectUnionOf> unions, int index) {

			if (index == unions.size()) {

				disjuncts.add(disjunct);
			}
			else {

				for (OWLClassExpression op : unions.get(index).getOperands()) {

					addForUnions(disjunct.copyAndExpandFor(op), unions, index + 1);
				}
			}
		}
	}

	MatchComponents(MappedNames mappedNames, MatchStructures matchStructures, boolean dynamic) {

		this.mappedNames = mappedNames;
		this.matchStructures = matchStructures;
		this.dynamic = dynamic;

		rootNodeName = mappedNames.getRootClassName();
		dataTypes = new DataTypes(dynamic);
	}

	NodePattern toNodePattern(OWLClassExpression source) {

		return nodePatterns.get(source);
	}

	List<NodePattern> toNodePatternDisjunction(OWLClassExpression source) {

		return new NodePatternDisjunctionSpec(source).checkCreate();
	}

	Relation toRelation(OWLClassExpression source) {

		return toRelation(source, false);
	}

	Relation toObjectValueRelation(AssertedObjectValue source) {

		return objectValueRelations.checkCreate(source);
	}

	Relation toDataValueRelation(AssertedDataValue source) {

		return dataValueRelations.checkCreate(source);
	}

	private Relation toRelation(OWLClassExpression source, boolean complement) {

		if (source instanceof OWLObjectComplementOf) {

			return toRelation((OWLObjectComplementOf)source, complement);
		}

		if (source instanceof OWLObjectSomeValuesFrom) {

			return someRelations.get((OWLObjectSomeValuesFrom)source, complement);
		}

		if (source instanceof OWLObjectAllValuesFrom) {

			return allRelations.get((OWLObjectAllValuesFrom)source, complement);
		}

		if (source instanceof OWLDataSomeValuesFrom || source instanceof OWLDataHasValue) {

			return dataRelations.get((OWLDataRestriction)source);
		}

		return null;
	}

	private Relation toRelation(OWLObjectComplementOf source, boolean wasComplement) {

		return toRelation(source.getOperand(), !wasComplement);
	}

	private DataValue toDataValue(OWLDataRestriction source) {

		if (source instanceof OWLQuantifiedDataRestriction) {

			return toDataType((OWLQuantifiedDataRestriction)source);
		}

		if (source instanceof OWLDataHasValue) {

			return toDataValue((OWLDataHasValue)source);
		}

		throw new Error("Unexpected OWLDataRestriction type");
	}

	private DataValue toDataType(OWLQuantifiedDataRestriction source) {

		return dataTypes.get(source.getFiller());
	}

	private DataValue toDataValue(OWLDataHasValue source) {

		return DataTypes.toDataValueExpression(source.getFiller());
	}

	private ClassName valueToClassName(OWLClassExpression v) {

		if (v instanceof OWLClass) {

			return mappedNames.get((OWLClass)v);
		}

		return valueToPatternClassName(v);
	}

	private ClassName valueToPatternClassName(OWLClassExpression v) {

		ClassName pCls = patternClasses.get(v);

		if (pCls == null) {

			NodePattern p = nodePatterns.get(v);

			if (p != null) {

				pCls = matchStructures.addPatternClass(p);

				patternClasses.put(v, pCls);
			}
		}

		return pCls;
	}
}
