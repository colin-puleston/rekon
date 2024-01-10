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
class OntologyClassifier {

	private List<Name> allNames;
	private List<NodeX> nodes;

	private List<PatternMatcher> profilePatterns;
	private List<PatternMatcher> definitionPatterns;
	private List<DisjunctionMatcher> allDisjunctions;
	private List<DisjunctionMatcher> definitionDisjunctions;

	private SubsumptionChecker subsumptionChecker = new PostFilteringSubsumptionChecker();

	private boolean initialPhase = true;
	private boolean nextPhaseRequired = true;

	private class PostFilteringSubsumptionChecker extends SubsumptionChecker {

		boolean subsumption(Pattern defn, Pattern candidate) {

			return defn.subsumesRelations(candidate);
		}
	}

	private class PatternSubsumedsChecker extends MultiThreadListProcessor<PatternMatcher> {

		private PotentialPatternSubsumeds candidatesFilter;

		PatternSubsumedsChecker(List<PatternMatcher> candidates) {

			candidatesFilter = new PotentialCorePatternSubsumeds(candidates);

			invokeListProcesses(definitionPatterns);
		}

		void processElement(PatternMatcher defn) {

			for (PatternMatcher c : candidatesFilter.getPotentialsFor(defn.getPattern())) {

				subsumptionChecker.check(defn, c);
			}
		}
	}

	private class DisjunctionSubsumedsChecker extends MultiThreadListProcessor<DisjunctionMatcher> {

		private PotentialDisjunctionSubsumeds candidatesFilter;

		DisjunctionSubsumedsChecker(List<DisjunctionMatcher> candidates) {

			candidatesFilter = new PotentialDisjunctionSubsumeds(candidates);

			invokeListProcesses(definitionDisjunctions);
		}

		void processElement(DisjunctionMatcher defn) {

			for (DisjunctionMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				subsumptionChecker.check(defn, c);
			}
		}
	}

	private abstract class PassConfig {

		private List<PatternMatcher> patternMatchCandidates = new ArrayList<PatternMatcher>();
		private List<DisjunctionMatcher> disjunctionMatchCandidates = new ArrayList<DisjunctionMatcher>();

		PassConfig() {

			findPatternMatchCandidates();
			findDisjunctionMatchCandidates();
		}

		boolean potentialInferences() {

			return !patternMatchCandidates.isEmpty()
					|| !disjunctionMatchCandidates.isEmpty();
		}

		void checkSubsumptions() {

			new PatternSubsumedsChecker(patternMatchCandidates);
			new DisjunctionSubsumedsChecker(disjunctionMatchCandidates);

			inferNewCommonDisjunctSubsumers();
		}

		abstract boolean initialPhasePass();

		abstract boolean potentialPatternMatchCandidate(Pattern p);

		private void findPatternMatchCandidates() {

			for (PatternMatcher pp : profilePatterns) {

				Pattern p = pp.getPattern();

				if (potentialPatternMatchCandidate(p)
					&& p.classifiable(initialPhasePass())) {

					patternMatchCandidates.add(pp);
				}
			}
		}

		private void findDisjunctionMatchCandidates() {

			for (DisjunctionMatcher d : allDisjunctions) {

				if (d.classifiable(initialPhasePass())) {

					disjunctionMatchCandidates.add(d);
				}
			}
		}

		private void inferNewCommonDisjunctSubsumers() {

			for (DisjunctionMatcher d : disjunctionMatchCandidates) {

				d.inferNewCommonDisjunctSubsumers();
			}
		}
	}

	private class InitialPhasePassConfig extends PassConfig {

		boolean initialPhasePass() {

			return true;
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			boolean expanded = p.updateForProfileExpansion();

			nextPhaseRequired |= expanded;

			return initialPhase || expanded;
		}
	}

	private class DefaultPassConfig extends PassConfig {

		boolean initialPhasePass() {

			return false;
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			return true;
		}
	}

	OntologyClassifier(List<Name> allNames, List<NodeX> nodes, NodeMatchers nodeMatchers) {

		this.allNames = allNames;
		this.nodes = nodes;

		profilePatterns = nodeMatchers.getProfilePatterns();
		definitionPatterns = nodeMatchers.getDefinitionPatterns();
		allDisjunctions = nodeMatchers.getAllDisjunctions();
		definitionDisjunctions = nodeMatchers.getDefinitionDisjunctions();

		classify();
	}

	private void classify() {

		while (initialiseNextPhase()) {

			PassConfig config = new InitialPhasePassConfig();

			while (config.potentialInferences()) {

				config = perfomPass(config);
			}

			initialPhase &= false;
		}

		NameClassification.completeAllClassifications(allNames);
	}

	private boolean initialiseNextPhase() {

		if (nextPhaseRequired) {

			nextPhaseRequired = false;

			if (initialPhase || resetAllPhaseInferredSubsumers()) {

				setAllProfileExpansionCheckRequireds();

				return true;
			}
		}

		return false;
	}

	private boolean resetAllPhaseInferredSubsumers() {

		boolean anyInfs = false;

		for (NodeX n : nodes) {

			if (n.getNodeClassifier().resetPhaseInferredSubsumers()) {

				anyInfs |= true;
			}
		}

		return anyInfs;
	}

	private void setAllProfileExpansionCheckRequireds() {

		for (PatternMatcher p : profilePatterns) {

			p.getPattern().setProfileExpansionCheckRequired();
		}
	}

	private PassConfig perfomPass(PassConfig config) {

		config.checkSubsumptions();

		NodeClassifier.expandAllNewInferredSubsumers(nodes);

		PassConfig nextConfig = new DefaultPassConfig();

		NodeClassifier.absorbAllNewInferredSubsumers(nodes);

		return nextConfig;
	}
}