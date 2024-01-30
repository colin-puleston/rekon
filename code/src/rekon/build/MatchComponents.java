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

package rekon.build;

import java.util.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class MatchComponents {

	private ClassNode rootClassNode;

	private MatchStructures structures;
	private BuildCustomiser customiser;

	private Patterns patterns = new Patterns();
	private Disjunctions disjunctions = new Disjunctions();

	private SomeRelations someRelations = new SomeRelations();
	private AllRelations allRelations = new AllRelations();
	private DataRelations dataRelations = new DataRelations();

	private Map<InputExpression, ClassNode> patternClasses = new HashMap<InputExpression, ClassNode>();

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

		Pattern get(InputExpression source) {

			return get(new PatternSpec(source));
		}

		Pattern checkCreate(PatternSpec source) {

			List<InputExpression> conjuncts = source.getConjuncts();

			if (conjuncts.size() == 1) {

				return checkCreate(conjuncts.get(0));
			}

			return checkCreateForConjuncts(conjuncts);
		}

		private Pattern checkCreate(InputExpression source) {

			switch (source.getExpressionType()) {

				case CLASS:

					return new Pattern(source.asClassNode());

				case INDIVIDUAL:

					return new Pattern(source.asIndividualNode());

				case CONJUNCTION:

					return checkCreateForConjuncts(source.asConjuncts());

				case RELATION:

					return checkCreateForRelation(source.asRelation());
			}

			return null;
		}

		private Pattern checkCreateForConjuncts(Collection<InputExpression> conjuncts) {

			NameSet nodes = new NameSet();
			Set<Relation> rels = new HashSet<Relation>();

			for (InputExpression op : conjuncts) {

				switch (op.getExpressionType()) {

					case CLASS:

						nodes.absorb(op.asClassNode());
						break;

					case DISJUNCTION:

						NodeX n = checkCreateNodeForDisjunction(op.asDisjuncts());

						if (n == null) {

							return null;
						}

						nodes.absorb(n);
						break;

					case RELATION:
					case COMPLEMENT:

						Relation r = toRelation(op);

						if (r == null) {

							return null;
						}

						rels.add(r);
						break;
				}
			}

			if (nodes.isEmpty()) {

				nodes.add(rootClassNode);
			}

			return new Pattern(nodes, rels);
		}

		private Pattern checkCreateForRelation(InputRelation source) {

			Relation r = toRelation(source, false);

			return r != null ? new Pattern(rootClassNode, r) : null;
		}

		private NodeX checkCreateNodeForDisjunction(Collection<InputExpression> source) {

			Set<NodeX> disjuncts = disjunctions.get(source);

			if (disjuncts == null || disjuncts.isEmpty()) {

				return null;
			}

			return resolveDisjunctsToNode(disjuncts);
		}
	}

	private class Disjunctions extends TypeEntities<Collection<InputExpression>, Set<NodeX>> {

		Set<NodeX> checkCreate(Collection<InputExpression> source) {

			Set<NodeX> disjuncts = new HashSet<NodeX>();

			for (InputExpression op : source) {

				Set<? extends NodeX> djs = toDisjuncts(op);

				if (djs == null) {

					return null;
				}

				disjuncts.addAll(djs);
			}

			return disjuncts;
		}

		private Set<? extends NodeX> toDisjuncts(InputExpression source) {

			if (source.getExpressionType() == InputExpressionType.DISJUNCTION) {

				return toIndividualDisjuncts(source.asDisjuncts());
			}

			NodeX n = toAtomicNode(source);

			return n != null ? Collections.singleton(n) : null;
		}

		private Set<IndividualNode> toIndividualDisjuncts(Collection<InputExpression> source) {

			Set<IndividualNode> disjuncts = new HashSet<IndividualNode>();

			for (InputExpression op : source) {

				if (op.getExpressionType() != InputExpressionType.INDIVIDUAL) {

					return null;
				}

				disjuncts.add(op.asIndividualNode());
			}

			return disjuncts;
		}
	}

	private abstract class Relations extends TypeEntities<InputRelation, Relation> {
	}

	private abstract class NodeRelations extends Relations {

		private boolean complement = false;

		Relation get(InputRelation source, boolean complement) {

			this.complement = complement;

			return get(source);
		}

		Relation checkCreate(InputRelation source) {

			if (source.getRelationType() == handledInputType()) {

				NodeProperty p = source.getNodeProperty();
				InputExpression f = source.getExpressionValue();

				if (complement) {

					return checkCreateForComplement(p, f);
				}

				return checkCreate(p, f);
			}

			return null;
		}

		Relation checkCreate(NodeProperty prop, InputExpression filler) {

			NodeValue target = toNodeValue(filler);

			return target != null ? create(prop, target) : null;
		}

		abstract InputRelationType handledInputType();

		abstract Relation checkCreateForComplement(
								NodeProperty prop,
								InputExpression filler);

		abstract Relation create(NodeProperty prop, NodeValue target);

		abstract NodeValue getNodeValueForEmptyDisjunction();

		Relation createForNoValue(NodeProperty prop) {

			return new AllRelation(prop, OntologyNames.ABSENT_CLASS_VALUE);
		}

		private NodeValue toNodeValue(InputExpression source) {

			if (source.getExpressionType() == InputExpressionType.DISJUNCTION) {

				return toNodeValue(source.asDisjuncts());
			}

			NodeX n = toAtomicNode(source);

			return n != null ? new NodeValue(n) : null;
		}

		private NodeValue toNodeValue(Collection<InputExpression> source) {

			Set<NodeX> disjuncts = disjunctions.get(source);

			if (disjuncts == null) {

				return null;
			}

			if (disjuncts.isEmpty()) {

				return getNodeValueForEmptyDisjunction();
			}

			return new NodeValue(resolveDisjunctsToNode(disjuncts));
		}
	}

	private class SomeRelations extends NodeRelations {

		InputRelationType handledInputType() {

			return InputRelationType.SOME_NODES;
		}

		Relation checkCreateForComplement(NodeProperty prop, InputExpression filler) {

			return isClassNode(filler, rootClassNode) ? createForNoValue(prop) : null;
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new SomeRelation(prop, target);
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return null;
		}
	}

	private class AllRelations extends NodeRelations {

		Relation checkCreate(NodeProperty prop, InputExpression filler) {

			return isClassNode(filler, OntologyNames.ABSENT_CLASS_NODE)
						? createForNoValue(prop)
						: super.checkCreate(prop, filler);
		}

		InputRelationType handledInputType() {

			return InputRelationType.ALL_NODES;
		}

		Relation checkCreateForComplement(NodeProperty prop, InputExpression filler) {

			return null;
		}

		Relation create(NodeProperty prop, NodeValue target) {

			return new AllRelation(prop, target);
		}

		NodeValue getNodeValueForEmptyDisjunction() {

			return OntologyNames.ABSENT_CLASS_VALUE;
		}
	}

	private class DataRelations extends Relations {

		Relation checkCreate(InputRelation source) {

			if (source.getRelationType() == InputRelationType.DATA_VALUE) {

				return new DataRelation(source.getDataProperty(), source.getDataValue());
			}

			return null;
		}
	}

	private class PatternSpec {

		private List<InputExpression> conjuncts = new ArrayList<InputExpression>();

		public boolean equals(Object other) {

			return conjuncts.equals(((PatternSpec)other).conjuncts);
		}

		public int hashCode() {

			return conjuncts.hashCode();
		}

		PatternSpec(InputExpression source) {

			if (source.getExpressionType() == InputExpressionType.CONJUNCTION) {

				conjuncts.addAll(source.asConjuncts());
			}
			else {

				conjuncts.add(source);
			}
		}

		List<InputExpression> getConjuncts() {

			return conjuncts;
		}
	}

	private class PatternDisjunctionSpec {

		private Set<PatternSpec> disjuncts = new HashSet<PatternSpec>();

		PatternDisjunctionSpec(InputExpression source) {

			if (source.getExpressionType() == InputExpressionType.DISJUNCTION) {

				addDisjunctsFor(source.asDisjuncts());
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

		private void addDisjunctsFor(Collection<InputExpression> source) {

			for (InputExpression op : source) {

				addDisjunctFor(op);
			}
		}

		private void addDisjunctFor(InputExpression source) {

			disjuncts.add(new PatternSpec(source));
		}
	}

	MatchComponents(
		OntologyNames names,
		MatchStructures structures,
		BuildCustomiser customiser,
		boolean dynamic) {

		this.structures = structures;
		this.customiser = customiser;
		this.dynamic = dynamic;

		rootClassNode = names.getRootClassNode();
	}

	Pattern toPattern(InputExpression source) {

		return patterns.get(source);
	}

	List<Pattern> toPatternDisjunction(InputExpression source) {

		return new PatternDisjunctionSpec(source).checkCreate();
	}

	Relation toRelation(InputExpression source) {

		return toRelation(source, false);
	}

	Relation toRelation(InputRelation source) {

		return toRelation(source, false);
	}

	private Relation toRelation(InputExpression source, boolean complement) {

		switch (source.getExpressionType()) {

			case COMPLEMENT:

				return toRelation(source.asComplemented(), !complement);

			case RELATION:

				return toRelation(source.asRelation(), complement);
		}

		return null;
	}

	private Relation toRelation(InputRelation source, boolean complement) {

		switch (source.getRelationType()) {

			case SOME_NODES:

				return someRelations.get(source, complement);

			case ALL_NODES:

				return allRelations.get(source, complement);

			case DATA_VALUE:

				return dataRelations.get(source);
		}

		return null;
	}

	private NodeX toAtomicNode(InputExpression source) {

		NodeX customNode = customiser.checkToCustomAtomicNode(source);

		if (customNode != null) {

			return customNode;
		}

		switch (source.getExpressionType()) {

			case CLASS:

				return source.asClassNode();

			case INDIVIDUAL:

				return source.asIndividualNode();
		}

		return toPatternClassNode(source);
	}

	private ClassNode toPatternClassNode(InputExpression source) {

		ClassNode pCls = patternClasses.get(source);

		if (pCls == null) {

			Pattern p = patterns.get(source);

			if (p != null) {

				pCls = structures.addPatternClass();

				structures.addProfilePattern(pCls, p);

				patternClasses.put(source, pCls);
			}
		}

		return pCls;
	}

	private NodeX resolveDisjunctsToNode(Collection<? extends NodeX> disjuncts) {

		if (disjuncts.size() == 1) {

			return disjuncts.iterator().next();
		}

		ClassNode c = structures.addPatternClass();

		structures.addDisjunction(c, disjuncts, false);

		return c;
	}

	private boolean isClassNode(InputExpression expr, ClassNode test) {

		if (expr.getExpressionType() == InputExpressionType.CLASS) {

			return expr.asClassNode() == test;
		}

		return false;
	}
}
