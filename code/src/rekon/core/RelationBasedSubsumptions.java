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
class RelationBasedSubsumptions {

	private ClassifyPassType passType;

	private List<TypeSubsumptions<?>> typeSubsumptions = new ArrayList<TypeSubsumptions<?>>();

	private abstract class RelationsProcessor {

		boolean processAll(NodeX node) {

			if (node.matchable()) {

				for (Relation r : getProcessableRelations(node)) {

					if (process(r)) {

						return true;
					}
				}
			}

			return false;
		}

		abstract Set<Relation> getProcessableRelations(NodeX node);

		abstract boolean process(Relation rel);
	}

	private abstract class TypeSubsumptions<N extends NodeX> {

		private List<N> sources = new ArrayList<N>();

		TypeSubsumptions() {

			typeSubsumptions.add(this);
		}

		abstract void checkAddSource(NodeX node);

		void checkAddTypeSource(N node) {

			if (requiredSource(node)) {

				sources.add(node);
			}
		}

		int sourceCount() {

			return sources.size();
		}

		void inferNewSubsumptions() {

			for (N s : sources) {

				inferNewSubsumptions(s);
			}
		}

		abstract boolean initialPassSource(N node);

		abstract boolean expansionPassSource(N node);

		abstract boolean defaultPassSource(N node);

		abstract void inferNewSubsumptions(N source);

		private boolean requiredSource(N node) {

			switch (passType) {

				case INITIAL:
					return initialPassSource(node);

				case EXPANSION:
					return expansionPassSource(node);

				case DEFAULT:
					return defaultPassSource(node);
			}

			throw new Error("Unrecognised pass-type: " + passType);
		}
	}

	private class DomainConstrainedSourceSubsumptions extends TypeSubsumptions<NodeX> {

		private SourceRelationsTester sourceRelationsTester = new SourceRelationsTester();

		private abstract class SourceRelationsProcessor extends RelationsProcessor {

			Set<Relation> getProcessableRelations(NodeX node) {

				return getDirectRelations(node);
			}

			boolean process(Relation rel) {

				PropertyX p = rel.getProperty();

				if (p instanceof NodeProperty) {

					NodeProperty np = (NodeProperty)p;

					if (np.hasDomain()) {

						return processDomain(np.getDomain());
					}
				}

				return false;
			}

			abstract boolean processDomain(ClassNode domain);
		}

		private class SourceRelationsTester extends SourceRelationsProcessor {

			boolean processDomain(ClassNode domain) {

				return true;
			}
		}

		private class SubsumptionsFinder extends SourceRelationsProcessor {

			private NodeX source;

			SubsumptionsFinder(NodeX source) {

				this.source = source;

				processAll(source);
			}

			boolean processDomain(ClassNode domain) {

				source.getNodeClassifier().checkAddInferredSubsumer(domain);

				return false;
			}
		}

		void checkAddSource(NodeX node) {

			checkAddTypeSource(node);
		}

		boolean initialPassSource(NodeX node) {

			return anySourceRelations(node);
		}

		boolean expansionPassSource(NodeX node) {

			return expandedProfile(node) && anySourceRelations(node);
		}

		boolean defaultPassSource(NodeX node) {

			return false;
		}

		void inferNewSubsumptions(NodeX source) {

			new SubsumptionsFinder(source);
		}

		private boolean anySourceRelations(NodeX node) {

			return sourceRelationsTester.processAll(node);
		}
	}

	private class AllRelationTargetSubsumptions extends TypeSubsumptions<IndividualNode> {

		private SourceRelationsTester sourceRelationsTester = new SourceRelationsTester();
		private UpdatedSourceRelationsTester updatedSourceRelationsTester = new UpdatedSourceRelationsTester();

		private List<IndividualNode> sources = new ArrayList<IndividualNode>();

		private abstract class SourceRelationsProcessor extends RelationsProcessor {

			Set<Relation> getProcessableRelations(NodeX node) {

				return getIndirectRelations(node);
			}

			boolean process(Relation rel) {

				return rel instanceof AllRelation && process((AllRelation)rel);
			}

			abstract boolean process(AllRelation rel);
		}

		private class SourceRelationsTester extends SourceRelationsProcessor {

			boolean process(AllRelation rel) {

				return true;
			}
		}

		private class UpdatedSourceRelationsTester extends SourceRelationsProcessor {

			boolean process(AllRelation rel) {

				return rel.getTarget().anyNewSubsumers(NodeSelector.ANY);
			}
		}

		private class SourceRelationsCollector extends SourceRelationsProcessor {

			final Map<NodeProperty, Set<NodeX>> collected
						= new HashMap<NodeProperty, Set<NodeX>>();

			SourceRelationsCollector(IndividualNode source) {

				processAll(source);
			}

			boolean process(AllRelation rel) {

				add(rel);

				return false;
			}

			private void add(AllRelation rel) {

				NodeProperty p = rel.getNodeProperty();
				Set<NodeX> targets = collected.get(p);

				if (targets == null) {

					targets = new HashSet<NodeX>();

					collected.put(p, targets);
				}

				targets.add(rel.getNodeValueTarget().getValueNode());
			}
		}

		private class SubsumptionsFinder {

			private Map<NodeProperty, Set<NodeX>> allRelations;

			SubsumptionsFinder(IndividualNode source) {

				allRelations = new SourceRelationsCollector(source).collected;

				for (Relation r : getDirectRelations(source)) {

					if (r instanceof SomeRelation) {

						checkAddForTarget((SomeRelation)r);
					}
				}
			}

			private void checkAddForTarget(SomeRelation r) {

				NodeX target = r.getNodeValueTarget().getValueNode();

				if (target instanceof IndividualNode) {

					checkAddForAllProperties(r.getNodeProperty(), (IndividualNode)target);
				}
			}

			private void checkAddForAllProperties(NodeProperty p, IndividualNode target) {

				checkAddForProperty(p, target);

				for (Name sp : p.getSubsumers()) {

					checkAddForProperty((NodeProperty)sp, target);
				}
			}

			private void checkAddForProperty(NodeProperty p, IndividualNode target) {

				Set<NodeX> targetSubsumers = allRelations.get(p);

				if (targetSubsumers != null) {

					for (NodeX s : targetSubsumers) {

						target.getNodeClassifier().checkAddInferredSubsumer(s);
					}
				}
			}
		}

		void checkAddSource(NodeX node) {

			if (node instanceof IndividualNode) {

				checkAddTypeSource((IndividualNode)node);
			}
		}

		boolean initialPassSource(IndividualNode node) {

			return anySourceRelations(node);
		}

		boolean expansionPassSource(IndividualNode node) {

			return expandedProfile(node) && anySourceRelations(node);
		}

		boolean defaultPassSource(IndividualNode node) {

			if (node.anyNewSubsumers(NodeSelector.ANY)) {

				return anySourceRelations(node);
			}

			return updatedSourceRelations(node);
		}

		void inferNewSubsumptions(IndividualNode source) {

			new SubsumptionsFinder(source);
		}

		private boolean anySourceRelations(IndividualNode node) {

			return sourceRelationsTester.processAll(node);
		}

		private boolean updatedSourceRelations(IndividualNode node) {

			return updatedSourceRelationsTester.processAll(node);
		}
	}

	RelationBasedSubsumptions(ClassifyPassType passType) {

		this.passType = passType;

		new DomainConstrainedSourceSubsumptions();
		new AllRelationTargetSubsumptions();
	}

	void checkAddInferenceSource(PatternMatcher profile) {

		for (TypeSubsumptions<?> typeSubs : typeSubsumptions) {

			typeSubs.checkAddSource(profile.getNode());
		}
	}

	int candidateCount() {

		int c = 0;

		for (TypeSubsumptions<?> typeSubs : typeSubsumptions) {

			c += typeSubs.sourceCount();
		}

		return c;
	}

	void inferNewSubsumptions() {

		for (TypeSubsumptions<?> typeSubs : typeSubsumptions) {

			typeSubs.inferNewSubsumptions();
		}
	}

	private boolean expandedProfile(NodeX node) {

		return getProfilePattern(node).expandedProfile();
	}

	private Set<Relation> getIndirectRelations(NodeX node) {

		Pattern p = getProfilePattern(node);
		Set<Relation> rels = new HashSet<Relation>();

		rels.addAll(p.getProfileRelations().getAll());
		rels.removeAll(p.getDirectRelations());

		return rels;
	}

	private Set<Relation> getDirectRelations(NodeX node) {

		return getProfilePattern(node).getDirectRelations();
	}

	private Pattern getProfilePattern(NodeX node) {

		return node.getProfilePatternMatcher().getPattern();
	}
}
