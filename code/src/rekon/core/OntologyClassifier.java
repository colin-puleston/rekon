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

	private Ontology ontology;

	private List<PatternNode> allPatternNodes;
	private List<PatternNode> definedPatternNodes;

	private List<DisjunctionNode> disjunctionNodes;
	private PotentialDisjunctionSubsumers disjunctionDefnsFilter;

	private SubsumptionChecker subsumptionChecker = new PostFilteringSubsumptionChecker();

	private class PostFilteringSubsumptionChecker extends SubsumptionChecker {

		boolean subsumption(NodePattern defn, NodePattern profile) {

			return defn.subsumesRelations(profile);
		}
	}

	private class PatternSubsumedsChecker extends MultiThreadListProcessor<PatternNode> {

		private PotentialPatternSubsumeds candidatesFilter;

		PatternSubsumedsChecker(List<PatternNode> candidates) {

			candidatesFilter = new PotentialOntologyPatternSubsumeds(candidates);

			invokeListProcesses(definedPatternNodes);
		}

		void processElement(PatternNode defined) {

			for (NodePattern defn : defined.getDefinitions()) {

				for (PatternNode c : candidatesFilter.getPotentialsFor(defn)) {

					subsumptionChecker.check(defined, defn, c);
				}
			}
		}
	}

	private class DisjunctionSubsumersChecker extends MultiThreadListProcessor<DisjunctionNode> {

		private boolean initialPass;

		DisjunctionSubsumersChecker(List<DisjunctionNode> candidates, boolean initialPass) {

			this.initialPass = initialPass;

			invokeListProcesses(candidates);
		}

		void processElement(DisjunctionNode candidate) {

			for (DisjunctionNode defn : disjunctionDefnsFilter.getPotentialsFor(candidate)) {

				subsumptionChecker.check(defn, candidate);
			}

			if (!initialPass) {

				candidate.setNewInferredCommonDisjunctSubsumers();
			}
		}
	}

	private abstract class PassConfig {

		private List<PatternNode> patternMatchCandidates = new ArrayList<PatternNode>();
		private List<DisjunctionNode> disjunctionMatchCandidates = new ArrayList<DisjunctionNode>();

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

		abstract boolean potentialPatternMatchCandidate(NodePattern p);

		private void findPatternMatchCandidates(boolean initialPass) {

			for (PatternNode n : allPatternNodes) {

				NodePattern p = n.getProfile();

				if (potentialPatternMatchCandidate(p) && p.classifiable(initialPass)) {

					patternMatchCandidates.add(n);
				}
			}
		}

		private void findDisjunctionMatchCandidates(boolean initialPass) {

			for (DisjunctionNode d : disjunctionNodes) {

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

		boolean potentialPatternMatchCandidate(NodePattern p) {

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

		boolean potentialPatternMatchCandidate(NodePattern p) {

			p.setRestrictedSignature();

			return true;
		}
	}

	private class PatternExpansionPassConfig extends PassConfig {

		PatternExpansionPassConfig() {

			super(true);
		}

		boolean potentialPatternMatchCandidate(NodePattern p) {

			return p.setExpandedSignature();
		}
	}

	OntologyClassifier(Ontology ontology) {

		this.ontology = ontology;

		MatchableNodes matchables = ontology.getMatchables();

		allPatternNodes = matchables.getAllPatternNodes();
		definedPatternNodes = matchables.getDefinedPatternNodes();

		disjunctionNodes = matchables.getDisjunctionNodes();
		disjunctionDefnsFilter = new PotentialDisjunctionSubsumers(disjunctionNodes);

		classify();
	}

	private void classify() {

		perfomExhaustivePasses(new PatternRestrictionPassConfig());
		perfomExhaustivePasses(new PatternExpansionPassConfig());

		NameClassification.completeAllClassifications(ontology.getAllNames());
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

		NodeNameClassifier.expandAllNewInferredSubsumers(ontology.getNodeNames());
	}

	private void absorbAllNewInferences() {

		NodeNameClassifier.absorbAllNewInferredSubsumers(ontology.getNodeNames());
	}

	private void resetPotentiallyUpdatedSignatureRefs() {

		List<PatternNode> potentiallyUpdateds = new ArrayList<PatternNode>();

		for (PatternNode n : allPatternNodes) {

			if (n.getProfile().potentialSignatureUpdates()) {

				potentiallyUpdateds.add(n);
			}
		}

		for (PatternNode n : potentiallyUpdateds) {

			n.getProfile().resetSignatureRefs();
		}
	}
}