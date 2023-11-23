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

	private NodeX rootNode;

	private Patterns patterns = new Patterns();
	private Disjunctions disjunctions = new Disjunctions();

	private ClassSomeRelations classSomeRelations = new ClassSomeRelations();
	private ClassAllRelations classAllRelations = new ClassAllRelations();
	private ClassDataRelations classDataRelations = new ClassDataRelations();

	private IndividualNodeRelations individualNodeRelations = new IndividualNodeRelations();
	private IndividualDataRelations individualDataRelations = new IndividualDataRelations();

	private DataTypes dataTypes;

	private Map<OWLClassExpression, ClassNode> patternClasses = new HashMap<OWLClassExpression, ClassNode>();

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

	private class Patterns extends TypeEntities<PatternSpec, Pattern> {

		Pattern get(OWLClassExpression source) {

			return get(new PatternSpec(source));
		}

		Pattern checkCreate(PatternSpec source) {

			List<OWLClassExpression> conjuncts = source.getConjuncts();

			if (conjuncts.size() == 1) {

				return checkCreate(conjuncts.get(0));
			}

			return checkCreateForConjuncts(conjuncts);
		}

		private Pattern checkCreate(OWLClassExpression source) {

			if (source instanceof OWLClass) {

				return new Pattern(mappedNames.get((OWLClass)source));
			}

			if (source instanceof OWLObjectIntersectionOf) {

				return checkCreateForConjuncts(((OWLObjectIntersectionOf)source).getOperands());
			}

			if (source instanceof OWLRestriction) {

				return checkCreateForRestriction((OWLRestriction)source);
			}

			return null;
		}

		private Pattern checkCreateForConjuncts(Collection<OWLClassExpression> conjuncts) {

			NameSet nodes = new NameSet();
			Set<Relation> rels = new HashSet<Relation>();

			for (OWLClassExpression op : conjuncts) {

				if (op instanceof OWLClass) {

					nodes.absorb(mappedNames.get((OWLClass)op));
				}
				else if (op instanceof OWLObjectUnionOf) {

					NodeX n = checkCreateNodeForUnion((OWLObjectUnionOf)op);

					if (n == null) {

						return null;
					}

					nodes.absorb(n);
				}
				else {

					Relation r = toRelation(op);

					if (r == null) {

						return null;
					}

					rels.add(r);
				}
			}

			if (nodes.isEmpty()) {

				nodes.add(rootNode);
			}

			return new Pattern(nodes, rels);
		}

		private Pattern checkCreateForRestriction(OWLRestriction source) {

			Relation r = toRelation(source);

			return r != null ? new Pattern(rootNode, r) : null;
		}

		private NodeX checkCreateNodeForUnion(OWLObjectUnionOf source) {

			Set<NodeX> disjuncts = disjunctions.get(source);

			if (disjuncts == null || disjuncts.isEmpty()) {

				return null;
			}

			return resolveDisjunctsToNode(disjuncts);
		}
	}

	private class Disjunctions extends TypeEntities<OWLObjectUnionOf, Set<NodeX>> {

		Set<NodeX> checkCreate(OWLObjectUnionOf source) {

			Set<NodeX> disjuncts = new HashSet<NodeX>();

			for (OWLClassExpression op : source.getOperands()) {

				Set<? extends NodeX> djs = toNodeDisjunction(op);

				if (djs == null) {

					return null;
				}

				disjuncts.addAll(djs);
			}

			return disjuncts;
		}
	}

	private abstract class Relations
								<S extends OWLRestriction>
								extends TypeEntities<S, Relation> {
	}

	private abstract class ClassNodeRelations
								<S extends OWLQuantifiedObjectRestriction>
								extends Relations<S> {

		private boolean complement = false;

		Relation get(S source, boolean complement) {

			this.complement = complement;

			return get(source);
		}

		Relation checkCreate(S source) {

			NodeProperty p = extracNodeProperty(source);

			if (p != null) {

				OWLClassExpression filler = source.getFiller();

				if (complement) {

					return checkCreateForComplement(p, filler);
				}

				return checkCreate(p, filler);
			}

			return null;
		}

		Relation checkCreate(NodeProperty prop, OWLClassExpression filler) {

			NodeValue target = toTarget(filler);

			return target != null ? create(prop, target) : null;
		}

		abstract Relation checkCreateForComplement(
								NodeProperty prop,
								OWLClassExpression filler);

		abstract Relation create(NodeProperty prop, NodeValue target);

		abstract NodeValue getNodeValueForEmptyDisjunction();

		Relation createForNoValue(NodeProperty prop) {

			return new AllRelation(prop, mappedNames.getAbsentClassValue());
		}

		private NodeValue toTarget(OWLClassExpression filler) {

			if (filler instanceof OWLObjectUnionOf) {

				return checkResolveNodeValue((OWLObjectUnionOf)filler);
			}

			Set<? extends NodeX> djs = toNodeDisjunction(filler);

			return djs != null ? resolveNodeValue(djs) : null;
		}

		private NodeValue checkResolveNodeValue(OWLObjectUnionOf union) {

			Set<NodeX> disjuncts = disjunctions.get(union);

			return disjuncts != null ? resolveNodeValue(disjuncts) : null;
		}

		private NodeValue resolveNodeValue(Collection<? extends NodeX> disjuncts) {

			if (disjuncts.isEmpty()) {

				return getNodeValueForEmptyDisjunction();
			}

			return new NodeValue(resolveDisjunctsToNode(disjuncts));
		}
	}

	private class ClassSomeRelations extends ClassNodeRelations<OWLObjectSomeValuesFrom> {

		Relation checkCreateForComplement(NodeProperty prop, OWLClassExpression filler) {

			return filler.isOWLThing() ? createForNoValue(prop) : null;
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new SomeRelation(prop, target);
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return null;
		}
	}

	private class ClassAllRelations extends ClassNodeRelations<OWLObjectAllValuesFrom> {

		Relation checkCreate(NodeProperty prop, OWLClassExpression filler) {

			return filler.isOWLNothing()
						? createForNoValue(prop)
						: super.checkCreate(prop, filler);
		}

		Relation checkCreateForComplement(NodeProperty prop, OWLClassExpression filler) {

			return null;
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new AllRelation(prop, target);
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return mappedNames.getAbsentClassValue();
		}
	}

	private class ClassDataRelations extends Relations<OWLDataRestriction> {

		Relation checkCreate(OWLDataRestriction source) {

			DataProperty p = extractDataProperty(source);

			if (p != null) {

				DataValue v = extractDataValue(source);

				if (v != null) {

					return new DataRelation(p, v);
				}
			}

			return null;
		}
	}

	private class IndividualNodeRelations extends TypeEntities<AssertedObjectValue, Relation> {

		Relation get(OWLObjectHasValue source) {

			AssertedObjectValue source2 = toAssertedObjectValue(source);

			return source2 != null ? get(source2) : null;
		}

		Relation checkCreate(AssertedObjectValue source) {

			OWLIndividual v = source.getValue();

			if (v instanceof OWLNamedIndividual) {

				return create(source.getProperty(), (OWLNamedIndividual)v);
			}

			return null;
		}

		private AssertedObjectValue toAssertedObjectValue(OWLObjectHasValue source) {

			OWLObjectPropertyExpression p = source.getProperty();

			if (p instanceof OWLObjectProperty) {

				return new AssertedObjectValue((OWLObjectProperty)p, source.getFiller());
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

	private class IndividualDataRelations extends TypeEntities<AssertedDataValue, Relation> {

		Relation checkCreate(AssertedDataValue source) {

			DataValue target = DataTypes.toDataValueExpression(source.getValue());

			return target != null ? create(source.getProperty(), target) : null;
		}

		private Relation create(OWLDataProperty prop, DataValue target) {

			return new DataRelation(mappedNames.get(prop), target);
		}
	}

	private class PatternSpec {

		private List<OWLClassExpression> conjuncts = new ArrayList<OWLClassExpression>();

		public boolean equals(Object other) {

			return conjuncts.equals(((PatternSpec)other).conjuncts);
		}

		public int hashCode() {

			return conjuncts.hashCode();
		}

		PatternSpec(OWLClassExpression source) {

			if (source instanceof OWLObjectIntersectionOf) {

				conjuncts.addAll(((OWLObjectIntersectionOf)source).getOperands());
			}
			else {

				conjuncts.add(source);
			}
		}

		List<OWLClassExpression> getConjuncts() {

			return conjuncts;
		}
	}

	private class PatternDisjunctionSpec {

		private Set<PatternSpec> disjuncts = new HashSet<PatternSpec>();

		PatternDisjunctionSpec(OWLClassExpression source) {

			if (source instanceof OWLObjectUnionOf) {

				addDisjunctsForUnion((OWLObjectUnionOf)source);
			}
			else {

				addDisjunctFor(source);
			}
		}

		List<Pattern> checkCreate() {

			List<Pattern> patternDisjunction = new ArrayList<Pattern>();

			for (PatternSpec d : disjuncts) {

				Pattern pd = patterns.get(d);

				if (pd == null) {

					return null;
				}

				patternDisjunction.add(pd);
			}

			return patternDisjunction;
		}

		private void addDisjunctsForUnion(OWLObjectUnionOf source) {

			for (OWLClassExpression op : source.getOperands()) {

				addDisjunctFor(op);
			}
		}

		private void addDisjunctFor(OWLClassExpression source) {

			disjuncts.add(new PatternSpec(source));
		}
	}

	MatchComponents(
		MappedNames mappedNames,
		MatchStructures matchStructures,
		boolean dynamic) {

		this.mappedNames = mappedNames;
		this.matchStructures = matchStructures;
		this.dynamic = dynamic;

		rootNode = mappedNames.getRootClassNode();
		dataTypes = new DataTypes(dynamic);
	}

	Pattern toPattern(OWLClassExpression source) {

		return patterns.get(source);
	}

	List<Pattern> toPatternDisjunction(OWLClassExpression source) {

		return new PatternDisjunctionSpec(source).checkCreate();
	}

	Relation toRelation(OWLClassExpression source) {

		return toRelation(source, false);
	}

	Relation toNodeValueRelation(AssertedObjectValue source) {

		return individualNodeRelations.checkCreate(source);
	}

	Relation toDataValueRelation(AssertedDataValue source) {

		return individualDataRelations.checkCreate(source);
	}

	NodeX toNode(OWLClassExpression source) {

		return toClassNode(source);
	}

	private Relation toRelation(OWLClassExpression source, boolean complement) {

		if (source instanceof OWLObjectComplementOf) {

			return toRelation((OWLObjectComplementOf)source, complement);
		}

		if (source instanceof OWLObjectSomeValuesFrom) {

			return classSomeRelations.get((OWLObjectSomeValuesFrom)source, complement);
		}

		if (source instanceof OWLObjectAllValuesFrom) {

			return classAllRelations.get((OWLObjectAllValuesFrom)source, complement);
		}

		if (source instanceof OWLDataSomeValuesFrom || source instanceof OWLDataHasValue) {

			return classDataRelations.get((OWLDataRestriction)source);
		}

		if (source instanceof OWLObjectHasValue) {

			return individualNodeRelations.get((OWLObjectHasValue)source);
		}

		return null;
	}

	private Relation toRelation(OWLObjectComplementOf source, boolean wasComplement) {

		return toRelation(source.getOperand(), !wasComplement);
	}

	private NodeProperty extracNodeProperty(OWLObjectRestriction source) {

		OWLObjectPropertyExpression p = source.getProperty();

		if (p instanceof OWLObjectProperty) {

			return mappedNames.get((OWLObjectProperty)p);
		}

		return null;
	}

	private DataProperty extractDataProperty(OWLDataRestriction source) {

		OWLDataPropertyExpression p = source.getProperty();

		if (p instanceof OWLDataProperty) {

			return mappedNames.get((OWLDataProperty)p);
		}

		return null;
	}

	private DataValue extractDataValue(OWLDataRestriction source) {

		if (source instanceof OWLQuantifiedDataRestriction) {

			return extractDataType((OWLQuantifiedDataRestriction)source);
		}

		if (source instanceof OWLDataHasValue) {

			return extractDataValue((OWLDataHasValue)source);
		}

		throw new Error("Unexpected OWLDataRestriction type");
	}

	private DataValue extractDataValue(OWLDataHasValue source) {

		return DataTypes.toDataValueExpression(source.getFiller());
	}

	private DataValue extractDataType(OWLQuantifiedDataRestriction source) {

		return dataTypes.get(source.getFiller());
	}

	private Set<? extends NodeX> toNodeDisjunction(OWLClassExpression source) {

		if (source instanceof OWLObjectOneOf) {

			return toIndividualDisjunction((OWLObjectOneOf)source);
		}

		NodeX n = toNode(source);

		return n != null ? Collections.singleton(n) : null;
	}

	private ClassNode toClassNode(OWLClassExpression source) {

		if (source instanceof OWLClass) {

			return mappedNames.get((OWLClass)source);
		}

		return toPatternClassNode(source);
	}

	private ClassNode toPatternClassNode(OWLClassExpression source) {

		ClassNode pCls = patternClasses.get(source);

		if (pCls == null) {

			Pattern p = patterns.get(source);

			if (p != null) {

				pCls = matchStructures.addPatternClass();

				matchStructures.addProfilePattern(pCls, p);

				patternClasses.put(source, pCls);
			}
		}

		return pCls;
	}

	private Set<IndividualNode> toIndividualDisjunction(OWLObjectOneOf source) {

		Set<IndividualNode> disjuncts = new HashSet<IndividualNode>();

		for (OWLIndividual i : source.getIndividuals()) {

			if (!(i instanceof OWLNamedIndividual)) {

				return null;
			}

			disjuncts.add(mappedNames.get((OWLNamedIndividual)i));
		}

		return disjuncts;
	}

	private NodeX resolveDisjunctsToNode(Collection<? extends NodeX> disjuncts) {

		if (disjuncts.size() == 1) {

			return disjuncts.iterator().next();
		}

		ClassNode c = matchStructures.addPatternClass();

		matchStructures.addDisjunction(c, disjuncts, false);

		return c;
	}
}
