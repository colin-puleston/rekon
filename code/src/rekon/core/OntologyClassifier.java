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
	private List<NodeName> nodeNames;

	private NodeMatchers nodeMatchers;

	private List<PatternMatcher> profilePatterns;
	private List<PatternMatcher> definitionPatterns;

	private List<DisjunctionMatcher> disjunctionDefns;
	private PotentialDisjunctionSubsumers disjunctionDefnsFilter;

	private SubsumptionChecker subsumptionChecker = new PostFilteringSubsumptionChecker();

	private class PostFilteringSubsumptionChecker extends SubsumptionChecker {

		boolean subsumption(Pattern defn, Pattern profile) {

			return defn.subsumesRelations(profile);
		}
	}

	private class PatternSubsumedsChecker extends MultiThreadListProcessor<PatternMatcher> {

		private PotentialPatternSubsumeds candidatesFilter;

		PatternSubsumedsChecker(List<PatternMatcher> candidates) {

			candidatesFilter = new PotentialCorePatternSubsumeds(candidates);

			invokeListProcesses(definitionPatterns);
		}

		void processElement(PatternMatcher defn) {

			Pattern p = defn.getPattern();

			for (PatternMatcher c : candidatesFilter.getPotentialsFor(p)) {

				subsumptionChecker.check(defn, c);
			}
		}
	}

	private class DisjunctionSubsumersChecker extends MultiThreadListProcessor<DisjunctionMatcher> {

		private boolean initialPass;

		DisjunctionSubsumersChecker(List<DisjunctionMatcher> candidates, boolean initialPass) {

			this.initialPass = initialPass;

			invokeListProcesses(candidates);
		}

		void processElement(DisjunctionMatcher candidate) {

			for (DisjunctionMatcher defn : disjunctionDefnsFilter.getPotentialsFor(candidate)) {

				subsumptionChecker.check(defn, candidate);
			}

			if (!initialPass) {

				candidate.setNewInferredCommonDisjunctSubsumers();
			}
		}
	}

	private abstract class PassConfig {

		private List<PatternMatcher> patternMatchCandidates = new ArrayList<PatternMatcher>();
		private List<DisjunctionMatcher> disjunctionMatchCandidates = new ArrayList<DisjunctionMatcher>();

		PassConfig(boolean initialPass) {

			findPatternMatchCandidates(initialPass);
			findDisjunctionMatchCandidates(initialPass);
		}

		boolean initialPass() {

			return false;
		}

		boolean potentialInferences() {

			return !patternMatchCandidates.isEmpty() || !disjunctionMatchCandidates.isEmpty();
		}

		void checkSubsumptions() {

			new PatternSubsumedsChecker(patternMatchCandidates);
			new DisjunctionSubsumersChecker(disjunctionMatchCandidates, initialPass());
		}

		abstract boolean potentialPatternMatchCandidate(Pattern p);

		private void findPatternMatchCandidates(boolean initialPass) {

			for (PatternMatcher pp : profilePatterns) {

				Pattern p = pp.getPattern();

				if (potentialPatternMatchCandidate(p) && p.classifiable(initialPass)) {

					patternMatchCandidates.add(pp);
				}
			}
		}

		private void findDisjunctionMatchCandidates(boolean initialPass) {

			for (DisjunctionMatcher d : disjunctionDefns) {

				if (d.classifiable(initialPass)) {

					disjunctionMatchCandidates.add(d);
				}
			}
		}
	}

	private class DefaultPassConfig extends PassConfig {

		DefaultPassConfig() {

			super(false);
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			return true;
		}
	}

	private class PatternRestrictionPassConfig extends PassConfig {

		PatternRestrictionPassConfig() {

			super(true);
		}

		boolean initialPass() {

			return true;
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			p.setRestrictedSignature();

			return true;
		}
	}

	private class PatternExpansionPassConfig extends PassConfig {

		PatternExpansionPassConfig() {

			super(true);
		}

		boolean potentialPatternMatchCandidate(Pattern p) {

			return p.setExpandedSignature();
		}
	}

	OntologyClassifier(
		List<Name> allNames,
		List<NodeName> nodeNames,
		NodeMatchers nodeMatchers) {

		this.allNames = allNames;
		this.nodeNames = nodeNames;
		this.nodeMatchers = nodeMatchers;

		profilePatterns = nodeMatchers.getProfilePatterns();
		definitionPatterns = nodeMatchers.getDefinitionPatternMatchers();

		disjunctionDefns = nodeMatchers.getDisjunctionMatchers();
		disjunctionDefnsFilter = new PotentialDisjunctionSubsumers(disjunctionDefns);

		classify();
	}

	private void classify() {

		perfomExhaustivePasses(new PatternRestrictionPassConfig());
		perfomExhaustivePasses(new PatternExpansionPassConfig());

		NameClassification.completeAllClassifications(allNames);
	}

	private void perfomExhaustivePasses(PassConfig passConfig) {

		while (passConfig.potentialInferences()) {

			passConfig = perfomPass(passConfig);
		}
	}

	private PassConfig perfomPass(PassConfig config) {

		config.checkSubsumptions();

		expandAllNewInferences();

		PassConfig nextPassConfig = new DefaultPassConfig();

		absorbAllNewInferences();
		resetPotentiallyUpdatedSignatureRefs();

		return nextPassConfig;
	}

	private void expandAllNewInferences() {

		NodeClassifier.expandAllNewInferredSubsumers(nodeNames);
	}

	private void absorbAllNewInferences() {

		NodeClassifier.absorbAllNewInferredSubsumers(nodeNames);
	}

	private void resetPotentiallyUpdatedSignatureRefs() {

		List<PatternMatcher> potentiallyUpdateds = new ArrayList<PatternMatcher>();

		for (PatternMatcher p : profilePatterns) {

			if (p.getPattern().potentialSignatureUpdates()) {

				potentiallyUpdateds.add(p);
			}
		}

		for (PatternMatcher p : potentiallyUpdateds) {

			p.getPattern().resetSignatureRefs();
		}
	}
}