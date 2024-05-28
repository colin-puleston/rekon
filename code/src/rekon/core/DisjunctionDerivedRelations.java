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
class DisjunctionDerivedRelations {

	private List<Relation> derivedRelations = new ArrayList<Relation>();
	private List<RelationDeriver<?>> derivers = new ArrayList<RelationDeriver<?>>();

	private abstract class RelationDeriver<TS> {

		private Class<? extends Relation> relationType;
		private Map<PropertyX, TS> propertiesToTargetSources = new HashMap<PropertyX, TS>();

		RelationDeriver(Class<? extends Relation> relationType) {

			this.relationType = relationType;

			derivers.add(this);
		}

		boolean checkProcessDisjunctRelation(Relation rel) {

			if (relationType.isAssignableFrom(rel.getClass())) {

				processDisjunctRelation(rel);

				return true;
			}

			return false;
		}

		boolean anyPotentialRelations() {

			return !propertiesToTargetSources.isEmpty();
		}

		void deriveRelations()  {

			for (PropertyX p : propertiesToTargetSources.keySet()) {

				TS ts = propertiesToTargetSources.get(p);
				Relation rel = checkCreateRelation(p, ts);

				if (rel != null) {

					derivedRelations.add(rel);
				}
			}
		}

		private void processDisjunctRelation(Relation rel) {

			PropertyX prop = rel.getProperty();
			Value target = rel.getTarget();

			if (propertiesToTargetSources.isEmpty()) {

				propertiesToTargetSources.put(prop, getInitialTargetSource(target));
			}
			else {

				TS ts = propertiesToTargetSources.get(prop);

				if (ts != null) {

					TS uts = updateTargetSource(ts, target);

					if (uts == null) {

						propertiesToTargetSources.remove(prop);
					}
					else {

						if (uts != ts) {

							propertiesToTargetSources.put(prop, uts);
						}
					}
				}
			}
		}

		abstract TS getInitialTargetSource(Value target);

		abstract TS updateTargetSource(TS source, Value target);

		abstract Relation checkCreateRelation(PropertyX prop, TS targetSource);
	}

	private abstract class NodeRelationDeriver extends RelationDeriver<NameSet> {

		NodeRelationDeriver(Class<? extends Relation> relationType) {

			super(relationType);
		}

		NameSet getInitialTargetSource(Value target) {

			return toValueNodePlusSubsumersSet((NodeValue)target);
		}

		NameSet updateTargetSource(NameSet source, Value target) {

			source.retainAll(toValueNodePlusSubsumersSet((NodeValue)target));

			return source.isEmpty() ? null : source;
		}

		Relation checkCreateRelation(PropertyX prop, NameSet targetSource) {

			NodeX n = toSingleNonSubsumerNode(targetSource);

			if (n == null) {

				return null;
			}

			return createRelation((NodeProperty)prop, new NodeValue(n));
		}

		abstract Relation createRelation(NodeProperty prop, NodeValue target);

		private NameSet toValueNodePlusSubsumersSet(NodeValue target) {

			NodeX n = target.getValueNode();
			NameSet nodes = new NameSet(n);

			nodes.addAll(n.getSubsumers());

			return nodes;
		}

		private NodeX toSingleNonSubsumerNode(NameSet nodes) {

			for (Name v : nodes.copyNames()) {

				nodes.removeAll(v.getSubsumers());
			}

			return nodes.size() == 1 ? nodes.getFirstNode() : null;
		}
	}

	private class SomeRelationDeriver extends NodeRelationDeriver {

		SomeRelationDeriver() {

			super(SomeRelation.class);
		}

		Relation createRelation(NodeProperty prop, NodeValue target) {

			return new SomeRelation(prop, target);
		}
	}

	private class AllRelationDeriver extends NodeRelationDeriver {

		AllRelationDeriver() {

			super(AllRelation.class);
		}

		Relation createRelation(NodeProperty prop, NodeValue target) {

			return new AllRelation(prop, target);
		}
	}

	private abstract class DataRelationDeriver
								<T extends DataValue>
								extends RelationDeriver<T> {

		private Class<T> targetType;

		DataRelationDeriver(Class<T> targetType) {

			super(DataRelation.class);

			this.targetType = targetType;
		}

		T getInitialTargetSource(Value target) {

			return targetType.cast(target);
		}

		T updateTargetSource(T source, Value target) {

			return unionWith(source, targetType.cast(target));
		}

		Relation checkCreateRelation(PropertyX prop, T targetSource) {

			return new DataRelation((DataProperty)prop, targetSource);
		}

		abstract T unionWith(T target1, T target2);
	}

	private abstract class NumberRelationDeriver
								<T extends NumberRange<T>>
								extends DataRelationDeriver<T> {

		NumberRelationDeriver(Class<T> targetType) {

			super(targetType);
		}

		T unionWith(T target1, T target2) {

			return target1.unionWith(target2);
		}
	}

	private class IntegerRelationDeriver extends NumberRelationDeriver<IntegerRange> {

		IntegerRelationDeriver() {

			super(IntegerRange.class);
		}
	}

	private class FloatRelationDeriver extends NumberRelationDeriver<FloatRange> {

		FloatRelationDeriver() {

			super(FloatRange.class);
		}
	}

	private class DoubleRelationDeriver extends NumberRelationDeriver<DoubleRange> {

		DoubleRelationDeriver() {

			super(DoubleRange.class);
		}
	}

	private class BooleanRelationDeriver extends DataRelationDeriver<BooleanValue> {

		BooleanRelationDeriver() {

			super(BooleanValue.class);
		}

		BooleanValue unionWith(BooleanValue target1, BooleanValue target2) {

			return target1.equals(target2) ? target1 : null;
		}
	}

	DisjunctionDerivedRelations() {

		new SomeRelationDeriver();
		new AllRelationDeriver();
		new IntegerRelationDeriver();
		new FloatRelationDeriver();
		new DoubleRelationDeriver();
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

	private boolean processDisjuncts(DisjunctionMatcher disjunction) {

		for (NodeX d : disjunction.getDirectDisjuncts().asNodes()) {

			if (!processDisjunct(d)) {

				return false;
			}
		}

		return true;
	}

	private boolean processDisjunct(NodeX disjunct) {

		Collection<Relation> rels = getProfileRelations(disjunct);

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

	private Collection<Relation> getProfileRelations(NodeX n) {

		PatternMatcher pm = n.getProfilePatternMatcher();

		if (pm != null) {

			return pm.getPattern().getProfileRelations().getAll();
		}

		return Collections.emptySet();
	}
}
