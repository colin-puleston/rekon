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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
abstract class DisjunctionBasedProfileRelationDeriver {

	private List<Relation> derivedRelations = new ArrayList<Relation>();
	private List<RelationDeriver<?>> derivers = new ArrayList<RelationDeriver<?>>();

	private abstract class RelationDeriver<T extends Value> {

		private Map<PropertyX, Set<T>> propertiesToTargets = new HashMap<PropertyX, Set<T>>();

		RelationDeriver() {

			derivers.add(this);
		}

		boolean checkProcessDisjunctRelation(Relation rel) {

			if (getRelationType().isAssignableFrom(rel.getClass())) {

				processDisjunctRelation(rel);

				return true;
			}

			return false;
		}

		boolean anyPotentialRelations() {

			return !propertiesToTargets.isEmpty();
		}

		void deriveRelations()  {

			for (PropertyX p : propertiesToTargets.keySet()) {

				Set<T> targets = propertiesToTargets.get(p);
				Relation rel = checkCreateRelation(p, targets);

				if (rel != null) {

					derivedRelations.add(rel);
				}
			}
		}

		abstract Class<? extends Relation> getRelationType();

		abstract Class<T> getTargetType();

		abstract Relation checkCreateRelation(PropertyX prop, Set<T> targets);

		private void processDisjunctRelation(Relation rel) {

			PropertyX prop = rel.getProperty();
			Set<T> targets = propertiesToTargets.get(prop);

			if (targets == null) {

				targets = new HashSet<T>();

				propertiesToTargets.put(prop, targets);
			}

			targets.add(getTargetType().cast(rel.getTarget()));
		}
	}

	private abstract class NodeRelationDeriver extends RelationDeriver<NodeValue> {

		Class<NodeValue> getTargetType() {

			return NodeValue.class;
		}

		Relation checkCreateRelation(PropertyX prop, Set<NodeValue> targets) {

			NodeX n = toSingleNode(toNodeDisjuncts(targets));

			return createRelation((NodeProperty)prop, new NodeValue(n));
		}

		abstract Relation createRelation(NodeProperty prop, NodeValue target);

		private Set<NodeX> toNodeDisjuncts(Set<NodeValue> targets) {

			Set<NodeX> disjuncts = getValuesPlusSubsumerNodes(targets);

			removeAllSubsumers(disjuncts);

			return disjuncts;
		}

		private Set<NodeX> getValuesPlusSubsumerNodes(Set<NodeValue> targets) {

			Set<NodeX> nodes = new HashSet<NodeX>();

			for (NodeValue t : targets) {

				NodeX n = t.getValueNode();

				nodes.add(n);
				nodes.addAll(n.getSubsumers().copyNodes());
			}

			return nodes;
		}

		private void removeAllSubsumers(Set<NodeX> nodes) {

			for (NodeX n : new ArrayList<NodeX>(nodes)) {

				nodes.removeAll(n.getSubsumers().copyNodes());
			}
		}

		private NodeX toSingleNode(Set<NodeX> nodes) {

			if (nodes.size() == 1) {

				return nodes.iterator().next();
			}

			return addDerivedValueDisjunction(nodes);
		}
	}

	private class SomeRelationDeriver extends NodeRelationDeriver {

		Class<SomeRelation> getRelationType() {

			return SomeRelation.class;
		}

		Relation createRelation(NodeProperty prop, NodeValue target) {

			return new SomeRelation(prop, target);
		}
	}

	private class AllRelationDeriver extends NodeRelationDeriver {

		Class<AllRelation> getRelationType() {

			return AllRelation.class;
		}

		Relation createRelation(NodeProperty prop, NodeValue target) {

			return new AllRelation(prop, target);
		}
	}

	private abstract class DataRelationDeriver
								<T extends DataValue>
								extends RelationDeriver<T> {

		Class<DataRelation> getRelationType() {

			return DataRelation.class;
		}

		Relation checkCreateRelation(PropertyX prop, Set<T> targets) {

			T t = unionOf(targets);

			return t != null ? new DataRelation((DataProperty)prop, t) : null;
		}

		abstract T unionOf(Set<T> targets);
	}

	private class NumberRelationDeriver extends DataRelationDeriver<NumberValue> {

		Class<NumberValue> getTargetType() {

			return NumberValue.class;
		}

		NumberValue unionOf(Set<NumberValue> targets) {

			return NumberValue.create(targets);
		}
	}

	private class BooleanRelationDeriver extends DataRelationDeriver<BooleanValue> {

		Class<BooleanValue> getTargetType() {

			return BooleanValue.class;
		}

		BooleanValue unionOf(Set<BooleanValue> targets) {

			return targets.size() == 1 ? targets.iterator().next() : null;
		}
	}

	DisjunctionBasedProfileRelationDeriver() {

		new SomeRelationDeriver();
		new AllRelationDeriver();
		new NumberRelationDeriver();
		new BooleanRelationDeriver();
	}

	void deriveFor(DisjunctionMatcher disjunction) {

		if (processDisjuncts(disjunction)) {

			deriveRelations();
		}
	}

	Collection<Relation> getAll() {

		return derivedRelations;
	}

	abstract ClassNode addDerivedValueDisjunction(Collection<NodeX> disjuncts);

	abstract Collection<Relation> resolveRelationExpansions(NodeX node);

	private boolean processDisjuncts(DisjunctionMatcher disjunction) {

		for (NodeX d : disjunction.getDirectDisjuncts().asNodes()) {

			if (!processDisjunct(d)) {

				return false;
			}
		}

		return true;
	}

	private boolean processDisjunct(NodeX disjunct) {

		Collection<Relation> rels = resolveRelationExpansions(disjunct);

		if (rels.isEmpty()) {

			return false;
		}

		for (Relation r : rels) {

			processDisjunctRelation(r);

			if (!anyPotentialRelations()) {

				return false;
			}
		}

		return true;
	}

	private void processDisjunctRelation(Relation rel) {

		for (RelationDeriver<?> d : derivers) {

			if (d.checkProcessDisjunctRelation(rel)) {

				break;
			}
		}
	}

	private void deriveRelations()  {

		for (RelationDeriver<?> d : derivers) {

			d.deriveRelations();
		}
	}

	private boolean anyPotentialRelations() {

		for (RelationDeriver<?> d : derivers) {

			if (d.anyPotentialRelations()) {

				return true;
			}
		}

		return false;
	}
}
