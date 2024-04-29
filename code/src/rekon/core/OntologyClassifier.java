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

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class OntologyClassifier {

	private Iterable<Name> allNames;
	private Iterable<NodeX> allNodes;

	private List<PatternMatcher> profilePatterns;
	private List<PatternMatcher> definitionPatterns;
	private List<DisjunctionMatcher> allDisjunctions;
	private List<DisjunctionMatcher> definitionDisjunctions;

	private SubsumptionChecker subsumptionChecker = new PostFilteringSubsumptionChecker();

	private boolean initialPhase = true;
	private boolean expansionsInCurrentPhase = false;

	private class PostFilteringSubsumptionChecker extends SubsumptionChecker {

		boolean patternSubsumption(Pattern defn, Pattern candidate) {

			return defn.subsumesRelations(candidate);
		}
	}

	private class PatternSubsumedsChecker extends MultiThreadListProcessor<PatternMatcher> {

		private PotentialPatternSubsumeds candidatesFilter;

		protected void processElement(PatternMatcher defn) {

			for (PatternMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				subsumptionChecker.check(defn, c);
			}
		}

		PatternSubsumedsChecker(List<PatternMatcher> candidates) {

			candidatesFilter = new PotentialCorePatternSubsumeds(candidates);

			invokeListProcesses(definitionPatterns);
		}
	}

	private class DisjunctionSubsumedsChecker extends MultiThreadListProcessor<DisjunctionMatcher> {

		private PotentialDisjunctionSubsumeds candidatesFilter;

		protected void processElement(DisjunctionMatcher defn) {

			for (DisjunctionMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				subsumptionChecker.check(defn, c);
			}
		}

		DisjunctionSubsumedsChecker(List<DisjunctionMatcher> candidates) {

			candidatesFilter = new PotentialDisjunctionSubsumeds(candidates);

			invokeListProcesses(definitionDisjunctions);
		}
	}

	private abstract class PassConfig {

		private List<PatternMatcher> patternMatchCandidates = new ArrayList<PatternMatcher>();
		private List<DisjunctionMatcher> disjunctionMatchCandidates = new ArrayList<DisjunctionMatcher>();
		private List<DisjunctionMatcher> disjunctionClassifyCandidates = new ArrayList<DisjunctionMatcher>();

		private AllRelationTargetSubsumptions allRelationTargetSubsumptions
												= new AllRelationTargetSubsumptions();

		PassConfig() {

			findClassifyCandidates();
		}

		void findClassifyCandidates() {

			findPatternMatchCandidates();
			findDisjunctionClassifyCandidates();
		}

		int candidateCount() {

			return patternMatchCandidates.size()
					+ disjunctionClassifyCandidates.size()
					+ allRelationTargetSubsumptions.candidateCount();
		}

		void checkSubsumptions() {

			new PatternSubsumedsChecker(patternMatchCandidates);
			new DisjunctionSubsumedsChecker(disjunctionMatchCandidates);

			inferNewCommonDisjunctSubsumers();
			allRelationTargetSubsumptions.inferNewSubsumptions();
		}

		abstract boolean initialPass();

		abstract boolean potentialPatternMatchCandidate(Pattern p);

		private void findPatternMatchCandidates() {

			boolean initPass = initialPass();

			for (PatternMatcher pp : profilePatterns) {

				Pattern p = pp.getPattern();

				if (potentialPatternMatchCandidate(p)) {

					if (p.matchable(initPass)) {

						patternMatchCandidates.add(pp);
					}

					if (pp.getNode() instanceof IndividualNode) {

						allRelationTargetSubsumptions.checkAddSourceIndividual(pp, initPass);
					}
				}
			}
		}

		private void findDisjunctionClassifyCandidates() {

			boolean initPass = initialPass();

			for (DisjunctionMatcher d : allDisjunctions) {

				if (d.unprocessedSubsumers(initPass)) {

					disjunctionClassifyCandidates.add(d);

					if (d.matchable(initPass)) {

						disjunctionMatchCandidates.add(d);
					}
				}
			}
		}

		private void inferNewCommonDisjunctSubsumers() {

			for (DisjunctionMatcher d : disjunctionClassifyCandidates) {

				d.inferNewCommonDisjunctSubsumers();
			}
		}
	}

	private class InitialPassConfig extends PassConfig {

		void findClassifyCandidates() {

			setAllProfileExpansionStatuses(true);
			super.findClassifyCandidates();
			setAllProfileExpansionStatuses(false);
		}

		boolean initialPass() {

			return true;
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			boolean expanded = p.processProfileExpansion();

			expansionsInCurrentPhase |= expanded;

			return initialPhase || expanded;
		}

		private void setAllProfileExpansionStatuses(boolean checkRequired) {

			for (PatternMatcher p : profilePatterns) {

				p.getPattern().setProfileExpansionStatus(checkRequired);
			}
		}
	}

	private class DefaultPassConfig extends PassConfig {

		boolean initialPass() {

			return false;
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			return true;
		}
	}

	OntologyClassifier(
		Iterable<Name> allNames,
		Iterable<NodeX> allNodes,
		NodeMatchers nodeMatchers,
		OntologyClassifyListener classifyListener) {

		this.allNames = allNames;
		this.allNodes = allNodes;

		profilePatterns = nodeMatchers.getProfilePatterns();
		definitionPatterns = nodeMatchers.getDefinitionPatterns();
		allDisjunctions = nodeMatchers.getAllDisjunctions();
		definitionDisjunctions = nodeMatchers.getDefinitionDisjunctions();

		classify(classifyListener);
	}

	private void classify(OntologyClassifyListener classifyListener) {

		while (initialPhase || initialiseNextPhase()) {

			PassConfig config = new InitialPassConfig();

			while (true) {

				int candidates = config.candidateCount();

				if (candidates == 0) {

					break;
				}

				if (config.initialPass()) {

					classifyListener.onPhaseStart();
				}

				classifyListener.onPassStart(candidates);

				config = perfomPass(config);
			}

			initialPhase &= false;
		}

		classifyListener.onCompletionStart();

		NameClassification.completeAllClassifications(allNames);
	}

	private boolean initialiseNextPhase() {

		if (expansionsInCurrentPhase && resetAllPhaseInferredSubsumers()) {

			expansionsInCurrentPhase = false;

			return true;
		}

		return false;
	}

	private boolean resetAllPhaseInferredSubsumers() {

		boolean anyInfs = false;

		for (NodeX n : allNodes) {

			if (n.getNodeClassifier().resetPhaseInferredSubsumers()) {

				anyInfs |= true;
			}
		}

		return anyInfs;
	}

	private PassConfig perfomPass(PassConfig config) {

		config.checkSubsumptions();

		NodeClassifier.expandAllNewInferredSubsumers(allNodes);

		PassConfig nextConfig = new DefaultPassConfig();

		NodeClassifier.absorbAllNewInferredSubsumers(allNodes);

		return nextConfig;
	}
}

