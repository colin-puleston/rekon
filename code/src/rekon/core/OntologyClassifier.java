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

	private Collection<NodeName> allNodes;
	private MatchableNodes matchables;

	private class ClassificationPass {

		private Collection<MatchableNode> passCandidates;
		private PotentialSubsumeds candidatesFilter;

		private List<MatchableNode> reclassifiables = new ArrayList<MatchableNode>();

		ClassificationPass(Collection<MatchableNode> passCandidates) {

			this.passCandidates = passCandidates;

			candidatesFilter = new PotentialSubsumeds(passCandidates);
		}

		Collection<MatchableNode> perfomPass() {

			checkSubsumptions();

			setNewInferredSubsumptions();
			expandNewInferredSubsumptions();

			findAnyReclassifiables();

			resetNewInferredSubsumptions();
			resetAllReclassifiablesReferences();

			return reclassifiables;
		}

		private void checkSubsumptions() {

			for (MatchableNode d : matchables.getDefineds()) {

				checkDefinedSubsumptions(d);
			}
		}

		private void checkDefinedSubsumptions(MatchableNode defined) {

			for (NodePattern defn : defined.getDefinitions()) {

				for (MatchableNode c : candidatesFilter.getPotentialsFor(defn)) {

					checkSubsumption(defined, defn, c);
				}
			}
		}

		private void setNewInferredSubsumptions() {

			for (MatchableNode c : passCandidates) {

				c.setNewInferredSubsumptions();
			}
		}

		private void expandNewInferredSubsumptions() {

			while (true) {

				boolean expansions = false;

				for (NodeName c : allNodes) {

					expansions |= c.getClassifier().expandNewInferredSubsumers();
				}

				if (!expansions) {

					break;
				}
			}
		}

		private void findAnyReclassifiables() {

			for (MatchableNode c : matchables.getAll()) {

				if (c.reclassifiable()) {

					reclassifiables.add(c);
				}
			}
		}

		private void resetNewInferredSubsumptions() {

			for (Name n : allNodes) {

				n.getClassifier().resetNewInferredSubsumers();
			}
		}

		private void resetAllReclassifiablesReferences() {

			for (MatchableNode m : reclassifiables) {

				m.resetSignatureRefs();
			}
		}
	}

	OntologyClassifier(Ontology ontology) {

		allNodes = ontology.getNodeNames();
		matchables = ontology.getMatchables();

		classify();
	}

	private void classify() {

		Collection<MatchableNode> passCandidates = matchables.getAll();

		do {

			passCandidates = new ClassificationPass(passCandidates).perfomPass();
		}
		while (!passCandidates.isEmpty());
	}
}
