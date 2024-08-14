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

	private abstract class TypeRelationDeriver<T extends Value> {

		private List<RelationSpec> relationSpecs = new ArrayList<RelationSpec>();

		private class RelationSpec {

			private PropertyX property;

			private List<T> inputTargets;
			private List<T> previousInputTargets;

			private int currentDisjunctIndex;
			private boolean updatedForCurrentDisjunct = false;

			RelationSpec(PropertyX property, T firstTarget) {

				this(property, new ArrayList<T>(), Collections.emptyList(), 0);

				inputTargets.add(firstTarget);
			}

			boolean checkAbsorbInputTarget(PropertyX property, T target, int disjunctIndex) {

				if (property == this.property) {

					if (currentDisjunctIndex == disjunctIndex - 1) {

						currentDisjunctIndex = disjunctIndex;

						updatedForCurrentDisjunct = checkUpdateInputTargets(target);
					}
					else {

						if (currentDisjunctIndex == disjunctIndex) {

							if (updatedForCurrentDisjunct) {

								checkSplitSpecForAdditionalInputTarget(target);
							}
							else {

								if (checkUpdateInputTargets(target)) {

									updatedForCurrentDisjunct = true;
								}
							}
						}
						else {

							relationSpecs.remove(this);
						}
					}

					return true;
				}

				return false;
			}

			void checkAddDerivedRelation(int totalDisjuncts) {

				if (totalDisjuncts == currentDisjunctIndex + 1) {

					Relation rel = checkCreateRelation(property, inputTargets);

					if (rel != null) {

						derivedRelations.add(rel);
					}
				}
			}

			private RelationSpec(
						PropertyX property,
						List<T> inputTargets,
						List<T> previousInputTargets,
						int currentDisjunctIndex) {

				this.property = property;
				this.inputTargets = inputTargets;
				this.previousInputTargets = previousInputTargets;
				this.currentDisjunctIndex = currentDisjunctIndex;

				relationSpecs.add(this);
			}

			private boolean checkUpdateInputTargets(T target) {

				List<T> its = new ArrayList<T>(inputTargets);

				if (updateInputTargets(inputTargets, target)) {

					previousInputTargets = its;

					return true;
				}

				return false;
			}

			private void checkSplitSpecForAdditionalInputTarget(T target) {

				List<T> its = new ArrayList<T>(previousInputTargets);

				if (updateInputTargets(its, target)) {

					new RelationSpec(property, its, previousInputTargets, currentDisjunctIndex);
				}
			}

			private boolean updateInputTargets(List<T> targets, T target) {

				for (T t : new ArrayList<T>(targets)) {

					if (t.subsumes(target)) {

						return false;
					}

					if (target.subsumes(t)) {

						targets.remove(t);
					}
				}

				targets.add(target);

				return true;
			}
		}

		boolean checkProcessDisjunctRelation(Relation rel, int disjunctIndex) {

			if (getRelationType().isAssignableFrom(rel.getClass())) {

				if (getTargetType().isAssignableFrom(rel.getTarget().getClass())) {

					processDisjunctRelation(rel, disjunctIndex);

					return true;
				}
			}

			return false;
		}

		void addDerivedRelations(int totalDisjuncts)  {

			for (RelationSpec s : relationSpecs) {

				s.checkAddDerivedRelation(totalDisjuncts);
			}
		}

		abstract Class<? extends Relation> getRelationType();

		abstract Class<T> getTargetType();

		abstract Relation checkCreateRelation(PropertyX prop, Collection<T> inputTargets);

		private void processDisjunctRelation(Relation rel, int disjunctIndex) {

			PropertyX p = rel.getProperty();
			T t = getTarget(rel);

			processInputTarget(p, t, disjunctIndex);

			for (Name s : p.getSubsumers()) {

				processInputTarget((PropertyX)s, t, disjunctIndex);
			}
		}

		private void processInputTarget(PropertyX prop, T target, int disjunctIndex) {

			boolean absorbed = false;

			for (RelationSpec s : copyRelationSpecs()) {

				absorbed |= s.checkAbsorbInputTarget(prop, target, disjunctIndex);
			}

			if (disjunctIndex == 0 && !absorbed) {

				new RelationSpec(prop, target);
			}
		}

		private List<RelationSpec> copyRelationSpecs() {

			return new ArrayList<RelationSpec>(relationSpecs);
		}

		private T getTarget(Relation rel) {

			return getTargetType().cast(rel.getTarget());
		}
	}

	private abstract class NodeRelationDeriver extends TypeRelationDeriver<NodeValue> {

		Class<NodeValue> getTargetType() {

			return NodeValue.class;
		}

		Relation checkCreateRelation(PropertyX prop, Collection<NodeValue> inputTargets) {

			return createRelation((NodeProperty)prop, toSingleTargetValue(inputTargets));
		}

		abstract Relation createRelation(NodeProperty prop, NodeValue target);

		private NodeValue toSingleTargetValue(Collection<NodeValue> inputTargets) {

			return new NodeValue(toSingleTargetNode(extractNodes(inputTargets)));
		}

		private Set<NodeX> extractNodes(Collection<NodeValue> inputTargets) {

			Set<NodeX> nodes = new HashSet<NodeX>();

			for (NodeValue t : inputTargets) {

				nodes.add(t.getValueNode());
			}

			return nodes;
		}

		private NodeX toSingleTargetNode(Collection<NodeX> nodes) {

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
								extends TypeRelationDeriver<T> {

		Class<DataRelation> getRelationType() {

			return DataRelation.class;
		}

		Relation checkCreateRelation(PropertyX prop, Collection<T> inputTargets) {

			T t = unionOf(inputTargets);

			return t != null ? new DataRelation((DataProperty)prop, t) : null;
		}

		abstract T unionOf(Collection<T> inputTargets);
	}

	private class NumberRelationDeriver extends DataRelationDeriver<NumberValue> {

		Class<NumberValue> getTargetType() {

			return NumberValue.class;
		}

		NumberValue unionOf(Collection<NumberValue> inputTargets) {

			return NumberValue.create(inputTargets);
		}
	}

	private class BooleanRelationDeriver extends DataRelationDeriver<BooleanValue> {

		Class<BooleanValue> getTargetType() {

			return BooleanValue.class;
		}

		BooleanValue unionOf(Collection<BooleanValue> inputTargets) {

			return inputTargets.size() == 1 ? inputTargets.iterator().next() : null;
		}
	}

	private class DisjunctionRelationsDeriver {

		private List<TypeRelationDeriver<?>> typeDerivers
					= new ArrayList<TypeRelationDeriver<?>>();

		DisjunctionRelationsDeriver(DisjunctionMatcher disjunction) {

			typeDerivers.add(new SomeRelationDeriver());
			typeDerivers.add(new AllRelationDeriver());
			typeDerivers.add(new NumberRelationDeriver());
			typeDerivers.add(new BooleanRelationDeriver());

			Names disjuncts = disjunction.getDirectDisjuncts();

			processDisjuncts(disjuncts);
			deriveRelations(disjuncts.size());
		}

		private void processDisjuncts(Names disjuncts) {

			int disjunctIndex = 0;

			for (NodeX d : disjuncts.asNodes()) {

				processDisjunct(d, disjunctIndex++);
			}
		}

		private void processDisjunct(NodeX disjunct, int disjunctIndex) {

			for (Relation r : resolveProfileRelations(disjunct)) {

				processDisjunctRelation(r, disjunctIndex);
			}
		}

		private void processDisjunctRelation(Relation rel, int disjunctIndex) {

			for (TypeRelationDeriver<?> d : typeDerivers) {

				if (d.checkProcessDisjunctRelation(rel, disjunctIndex)) {

					break;
				}
			}
		}

		private void deriveRelations(int totalDisjuncts)  {

			for (TypeRelationDeriver<?> d : typeDerivers) {

				d.addDerivedRelations(totalDisjuncts);
			}
		}
	}

	DisjunctionBasedProfileRelationDeriver(NodeX node) {

		for (DisjunctionMatcher d : node.getAllDisjunctionMatchers()) {

			new DisjunctionRelationsDeriver(d);
		}
	}

	Collection<Relation> getAll() {

		return derivedRelations;
	}

	abstract ClassNode addDerivedValueDisjunction(Collection<NodeX> disjuncts);

	abstract Collection<Relation> resolveProfileRelations(NodeX node);
}