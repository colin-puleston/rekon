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
class OntologyClassifier extends Classifier {

	private Ontology ontology;

	private List<MatchableNode> defineds = new ArrayList<MatchableNode>();

	private abstract class ClassificationPassConfig {

		private List<MatchableNode> candidates = new ArrayList<MatchableNode>();

		ClassificationPassConfig(boolean initialPass) {

			for (MatchableNode m : ontology.getMatchables().getAll()) {

				NodePattern p = m.getProfile();

				if (potentialCandidate(p) && p.classifiable(initialPass)) {

					candidates.add(m);
				}
			}
		}

		boolean anyCandidates() {

			return !candidates.isEmpty();
		}

		List<MatchableNode> getCandidates() {

			return candidates;
		}

		abstract boolean potentialCandidate(NodePattern p);
	}

	private class RestrictedSignaturesInitialPassConfig extends ClassificationPassConfig {

		RestrictedSignaturesInitialPassConfig() {

			super(true);
		}

		boolean potentialCandidate(NodePattern p) {

			p.setRestrictedSignature();

			return true;
		}
	}

	private class ExpandedSignaturesInitialPassConfig extends ClassificationPassConfig {

		ExpandedSignaturesInitialPassConfig() {

			super(true);
		}

		boolean potentialCandidate(NodePattern p) {

			return p.setExpandedSignature();
		}
	}

	private class SubsequentPassConfig extends ClassificationPassConfig {

		SubsequentPassConfig() {

			super(false);
		}

		boolean potentialCandidate(NodePattern p) {

			return true;
		}
	}

	private class ClassificationPass {

		private PotentialSubsumeds candidatesFilter;

		private class SubsumptionsChecker extends MultiThreadListProcessor<MatchableNode> {

			SubsumptionsChecker() {

				invokeListProcesses(defineds);
			}

			void processElement(MatchableNode m) {

				checkDefinedSubsumptions(m);
			}
		}

		ClassificationPass(ClassificationPassConfig passConfig) {

			candidatesFilter = new PotentialOntologySubsumeds(passConfig.getCandidates());
		}

		ClassificationPassConfig perfomPass() {

			new SubsumptionsChecker();

			expandAllNewInferences();

			ClassificationPassConfig nextPassConfig = new SubsequentPassConfig();

			absorbAllNewInferences();
			resetPotentiallyUpdatedSignatureRefs();

			return nextPassConfig;
		}

		private void checkDefinedSubsumptions(MatchableNode defined) {

			for (NodePattern defn : defined.getDefinitions()) {

				for (MatchableNode c : candidatesFilter.getPotentialsFor(defn)) {

					checkSubsumption(defined, defn, c);
				}
			}
		}

		private void expandAllNewInferences() {

			NodeNameClassifier.expandAllNewInferredSubsumers(ontology.getNodeNames());
		}

		private void absorbAllNewInferences() {

			NodeNameClassifier.absorbAllNewInferredSubsumers(ontology.getNodeNames());
		}

		private void resetPotentiallyUpdatedSignatureRefs() {

			ontology.getMatchables().resetAllPotentiallyUpdatedSignatureRefs();
		}
	}

	OntologyClassifier(Ontology ontology) {

		this.ontology = ontology;

		for (MatchableNode m : ontology.getMatchables().getAll()) {

			if (m.hasDefinitions()) {

				defineds.add(m);
			}
		}

		classify();
	}

	private void classify() {

		perfomExhaustivePasses(new RestrictedSignaturesInitialPassConfig());
		perfomExhaustivePasses(new ExpandedSignaturesInitialPassConfig());

		NameClassification.completeAllClassifications(ontology.getAllNames());
	}

	private void perfomExhaustivePasses(ClassificationPassConfig passConfig) {

		while (passConfig.anyCandidates()) {

			passConfig = new ClassificationPass(passConfig).perfomPass();
		}
	}
}