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
class AllRelationTargetSubsumptions {

	private SourceIndividualTester sourceIndividualTester;
	private List<PatternMatcher> sourceIndividualProfiles = new ArrayList<PatternMatcher>();

	private abstract class AllRelationProcessor {

		boolean processForAllSubsumers(IndividualNode indNode) {

			for (NodeX s : indNode.getSubsumers().asNodes()) {

				PatternMatcher p = s.getProfilePatternMatcher();

				if (p != null && processForSubsumer(p)) {

					return true;
				}
			}

			return false;
		}

		abstract boolean process(AllRelation rel);

		private boolean processForSubsumer(PatternMatcher profile) {

			for (Relation r : profile.getPattern().getDirectRelations()) {

				if (r instanceof AllRelation && process((AllRelation)r)) {

					return true;
				}
			}

			return false;
		}
	}

	private class AllRelationExistanceTester extends AllRelationProcessor {

		boolean process(AllRelation rel) {

			return true;
		}
	}

	private class AllRelationTargetNewSubsumersTester extends AllRelationProcessor {

		boolean process(AllRelation rel) {

			return rel.getTarget().anyNewSubsumers(NodeSelector.ANY);
		}
	}

	private abstract class SourceIndividualTester {

		private AllRelationExistanceTester allRelationExistanceTester
										= new AllRelationExistanceTester();

		boolean test(PatternMatcher indProfile) {

			return test(indProfile, (IndividualNode)indProfile.getNode());
		}

		abstract boolean test(PatternMatcher indProfile, IndividualNode indNode);

		boolean anyAllRelations(IndividualNode indNode) {

			return allRelationExistanceTester.processForAllSubsumers(indNode);
		}
	}

	private class InitialPassSourceIndividualTester extends SourceIndividualTester {

		boolean test(PatternMatcher indProfile, IndividualNode indNode) {

			return anyAllRelations(indNode);
		}
	}

	private class ExpansionPassSourceIndividualTester extends SourceIndividualTester {

		boolean test(PatternMatcher indProfile, IndividualNode indNode) {

			if (indProfile.getPattern().expanded()) {

				return anyAllRelations(indNode);
			}

			return false;
		}
	}

	private class DefaultPassSourceIndividualTester extends SourceIndividualTester {

		private AllRelationTargetNewSubsumersTester allRelationTargetNewSubsumersTester
												= new AllRelationTargetNewSubsumersTester();

		boolean test(PatternMatcher indProfile, IndividualNode indNode) {

			if (indNode.anyNewSubsumers(NodeSelector.ANY)) {

				return anyAllRelations(indNode);
			}

			return anyAllRelationTargetNewSubsumers(indNode);
		}

		private boolean anyAllRelationTargetNewSubsumers(IndividualNode indNode) {

			return allRelationTargetNewSubsumersTester.processForAllSubsumers(indNode);
		}
	}

	private class TargetSubsumptionsFinder {

		private Map<NodeProperty, Set<NodeX>> subsumerAllRelations
								= new HashMap<NodeProperty, Set<NodeX>>();

		private class AllRelationCollector extends AllRelationProcessor {

			AllRelationCollector(PatternMatcher indProfile) {

				processForAllSubsumers((IndividualNode)indProfile.getNode());
			}

			boolean process(AllRelation rel) {

				add(rel);

				return false;
			}

			private void add(AllRelation rel) {

				NodeProperty p = rel.getNodeProperty();
				Set<NodeX> targets = subsumerAllRelations.get(p);

				if (targets == null) {

					targets = new HashSet<NodeX>();

					subsumerAllRelations.put(p, targets);
				}

				targets.add(rel.getNodeValueTarget().getValueNode());
			}
		}

		TargetSubsumptionsFinder(PatternMatcher indProfile) {

			new AllRelationCollector(indProfile);

			for (Relation r : indProfile.getPattern().getDirectRelations()) {

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

			Set<NodeX> targetSubsumers = subsumerAllRelations.get(p);

			if (targetSubsumers != null) {

				for (NodeX s : targetSubsumers) {

					target.getNodeClassifier().checkAddInferredSubsumer(s);
				}
			}
		}
	}

	AllRelationTargetSubsumptions(ClassifyPassType passType) {

		sourceIndividualTester = createSourceIndividualTester(passType);
	}

	void checkAddSourceIndividual(PatternMatcher indProfile) {

		if (sourceIndividualTester.test(indProfile)) {

			sourceIndividualProfiles.add(indProfile);
		}
	}

	int candidateCount() {

		return sourceIndividualProfiles.size();
	}

	void inferNewSubsumptions() {

		for (PatternMatcher p : sourceIndividualProfiles) {

			new TargetSubsumptionsFinder(p);
		}
	}

	private SourceIndividualTester createSourceIndividualTester(ClassifyPassType passType) {

		switch (passType) {

			case INITIAL:
				return new InitialPassSourceIndividualTester();

			case EXPANSION:
				return new ExpansionPassSourceIndividualTester();

			case DEFAULT:
				return new DefaultPassSourceIndividualTester();
		}

		throw new Error("Unrecognised pass-type: " + passType);
	}
}
