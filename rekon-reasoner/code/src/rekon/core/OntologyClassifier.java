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
class OntologyClassifier extends SubsumptionChecker {

	private Ontology ontology;
	private OntologyClassifyListener classifyListener;

	private class MultiSubsumptionChecker extends MultiThreadListProcessor<PatternMatcher> {

		private PotentialSubsumeds candidatesFilter;

		protected void processElement(PatternMatcher defn) {

			for (PatternMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				checkSubsumption(defn, c);
			}
		}

		MultiSubsumptionChecker(List<PatternMatcher> candidates) {

			candidatesFilter = new PotentialCoreSubsumeds(candidates);

			invokeListProcesses(ontology.getAllDefinitions());
		}
	}

	private class Pass {

		private boolean initialPhase;
		private boolean phaseInitialPass;

		private List<PatternMatcher> candidates = new ArrayList<PatternMatcher>();

		Pass(boolean initialPhase, boolean phaseInitialPass) {

			this.initialPhase = initialPhase;
			this.phaseInitialPass = phaseInitialPass;

			if (!phaseInitialPass) {

				findAllCandidates();
			}
		}

		boolean initialisePass() {

			if (phaseInitialPass) {

				ontology.getProfilesExpander().expandAll();

				findAllCandidates();
			}

			return !candidates.isEmpty();
		}

		Pass perfomPass() {

			checkSubsumptions();

			Iterable<NodeX> allNodes = ontology.getAllNodes();

			expandAllNewInferences();

			Pass next = new Pass(initialPhase, false);

			absorbAllNewInferences();

			return next;
		}

		int candidateCount() {

			return candidates.size();
		}

		boolean phaseInitialPass() {

			return phaseInitialPass;
		}

		private void findAllCandidates() {

			for (PatternMatcher m : ontology.getAllProfiles()) {

				if (isCandidate(m.getPattern())) {

					candidates.add(m);
				}
			}
		}

		private boolean isCandidate(Pattern pattern) {

			if (phaseInitialPass) {

				if (initialPhase) {

					return pattern.initialPassMatchableProfile();
				}

				return pattern.expandedProfile();
			}

			return pattern.nonInitialPassMatchableProfile();
		}

		private void checkSubsumptions() {

			new MultiSubsumptionChecker(candidates);
		}

		private void expandAllNewInferences() {

			NodeClassifier.expandAllNewInferredSubsumers(getAllNodes());
		}

		private void absorbAllNewInferences() {

			NodeClassifier.absorbAllNewInferredSubsumers(getAllNodes());
		}
	}

	OntologyClassifier(Ontology ontology, OntologyClassifyListener classifyListener) {

		this.ontology = ontology;
		this.classifyListener = classifyListener;

		classify();
	}

	boolean patternSubsumption(Pattern defn, Pattern candidate) {

		return defn.subsumesRelations(candidate);
	}

	private void classify() {

		boolean initialPhase = true;

		while (performPhase(initialPhase)) {

			initialPhase = false;
		}

		completeClassification();
	}

	private boolean performPhase(boolean initialPhase) {

		Pass pass = new Pass(initialPhase, true);

		while (pass.initialisePass()) {

			if (pass.phaseInitialPass()) {

				classifyListener.onPhaseStart();
			}

			classifyListener.onPassStart(pass.candidateCount());

			pass = pass.perfomPass();
		}

		if (initialPhase) {

			resetAllPhaseInferredSubsumers();

			return true;
		}

		if (pass.phaseInitialPass()) {

			return false;
		}

		return resetAllPhaseInferredSubsumers();
	}

	private boolean resetAllPhaseInferredSubsumers() {

		return NodeClassifier.resetAllPhaseInferredSubsumers(getAllNodes());
	}

	private void completeClassification() {

		classifyListener.onCompletionStart();

		NameClassification.completeAllClassifications(getAllNames());
	}

	private Iterable<Name> getAllNames() {

		return ontology.getAllNames();
	}

	private Iterable<NodeX> getAllNodes() {

		return ontology.getAllNodes();
	}
}
