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

	private List<PatternMatcher> sourceIndividualProfiles = new ArrayList<PatternMatcher>();

	private PotentialSourceIndividualTester potentialSourceIndividualTester
												= new PotentialSourceIndividualTester();

	private abstract class SubsumerAllRelationProcessor {

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

	private class PotentialSourceIndividualTester extends SubsumerAllRelationProcessor {

		boolean test(PatternMatcher indProfile, boolean initialPass) {

			if (initialPass) {

				return true;
			}

			IndividualNode indNode = (IndividualNode)indProfile.getNode();

			if (indNode.anyNewSubsumers()) {

				return true;
			}

			return processForAllSubsumers(indNode);
		}

		boolean process(AllRelation rel) {

			return rel.getTarget().anyNewSubsumers(NodeSelector.ANY);
		}
	}

	private class TargetSubsumptionsFinder {

		private Map<NodeProperty, Set<NodeX>> subsumerAllRelations
								= new HashMap<NodeProperty, Set<NodeX>>();

		private class SubsumerAllRelationCollector extends SubsumerAllRelationProcessor {

			SubsumerAllRelationCollector(PatternMatcher indProfile) {

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

			new SubsumerAllRelationCollector(indProfile);

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

					target.getNodeClassifier().addNewInferredSubsumer(s);
				}
			}
		}
	}

	void checkAddSourceIndividual(PatternMatcher indProfile, boolean initialPass) {

		if (potentialSourceIndividualTester.test(indProfile, initialPass)) {

			sourceIndividualProfiles.add(indProfile);
		}
	}

	boolean potentialInferences() {

		return !sourceIndividualProfiles.isEmpty();
	}

	void inferNewSubsumptions() {

		for (PatternMatcher p : sourceIndividualProfiles) {

			new TargetSubsumptionsFinder(p);
		}
	}
}
