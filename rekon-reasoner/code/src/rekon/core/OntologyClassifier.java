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

		protected void processElement(PatternMatcher defn, int threadIndex) {

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
		}

		boolean initialisePass() {

			if (phaseInitialPass) {

				if (!initialPhase) {

					expandPhaseInferences();
				}

				ontology.getProfilesExpander().expandAll();

				findAllCandidates();

				if (!initialPhase) {

					absorbPhaseInferenceExpansions();
				}
			}
			else {

				findAllCandidates();

				absorbPassInferences();
			}

			return !candidates.isEmpty();
		}

		void perfomPass() {

			if (phaseInitialPass) {

				classifyListener.onPhaseStart();
			}

			classifyListener.onPassStart(candidates.size());

			checkSubsumptions();
		}

		private void findAllCandidates() {

			for (PatternMatcher m : ontology.getAllProfiles()) {

				if (isCandidate(m.getPattern())) {

					candidates.add(m);
				}
			}
		}

		private boolean isCandidate(Pattern pattern) {

			if (phaseInitialPass && !initialPhase && pattern.expandedProfile()) {

				return true;
			}

			return pattern.matchableProfile(getCandidateMatchSubsumerStatus());
		}

		private SubsumerStatus getCandidateMatchSubsumerStatus() {

			if (phaseInitialPass) {

				if (initialPhase) {

					return SubsumerStatus.CURRENT;
				}

				return SubsumerStatus.PHASE_INFERENCE_EXPANSION;
			}

			return SubsumerStatus.PASS_INFERENCE;
		}

		private void checkSubsumptions() {

			new MultiSubsumptionChecker(candidates);
		}

		private void absorbPassInferences() {

			NodeClassifier.absorbPassInferences(getAllNodes());
		}

		private void expandPhaseInferences() {

			NodeClassifier.expandPhaseInferences(getAllNodes());
		}

		private void absorbPhaseInferenceExpansions() {

			NodeClassifier.absorbPhaseInferenceExpansions(getAllNodes());
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

		boolean initialPass = true;

		while (true) {

			Pass pass = new Pass(initialPhase, initialPass);

			if (!pass.initialisePass()) {

				break;
			}

			pass.perfomPass();

			initialPass = false;
		}

		return !initialPass;
	}

	private void completeClassification() {

		classifyListener.onCompletionStart();

		NameClassification.completeInitialisations(getAllNodes());
		NameClassification.completeInitialisations(getAllProperties());
	}

	private Iterable<NodeX> getAllNodes() {

		return ontology.getAllNodes();
	}

	private Iterable<PropertyX> getAllProperties() {

		return ontology.getAllProperties();
	}
}
