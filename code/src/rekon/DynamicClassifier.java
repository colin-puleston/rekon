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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
class DynamicClassifier extends Classifier {

	private PotentialSubsumers definedsFilter;

	private class CandidateClassifier {

		private Collection<MatchableNode> preFilteredDefineds;

		CandidateClassifier(
			MatchableNodes dynamicMatchables,
			Collection<MatchableNode> preFilteredDefineds) {

			this.preFilteredDefineds = preFilteredDefineds;

			for (MatchableNode c : dynamicMatchables.getAll()) {

				classifyCandidate(c);
			}
		}

		private void classifyCandidate(MatchableNode c) {

			do {

				checkCandidateSubsumptions(c);
				c.setNewInferredSubsumptions();
			}
			while (checkReclassifiable(c));
		}

		private boolean checkReclassifiable(MatchableNode c) {

			Name n = c.getName();

			if (n.reclassifiable()) {

				n.getClassifier().resetNewInferredSubsumers();
				c.resetAllReferences();

				return true;
			}

			return false;
		}

		private void checkCandidateSubsumptions(MatchableNode c) {

			for (MatchableNode defined : filterDefinedsFor(c.getProfile())) {

				for (NodePattern defn : defined.getDefinitions()) {

					checkSubsumption(defined, defn, c);
				}
			}
		}

		private Collection<MatchableNode> filterDefinedsFor(NodePattern p) {

			return preFilteredDefineds != null
					? preFilteredDefineds
					: definedsFilter.getPotentialsFor(p);
		}

	}

	DynamicClassifier(Ontology ontology) {

		definedsFilter = new PotentialSubsumers(ontology.getMatchables().getDefineds());
	}

	void classify(MatchableNodes dynamicMatchables, Collection<MatchableNode> preFilteredDefineds) {

		new CandidateClassifier(dynamicMatchables, preFilteredDefineds);
	}
}
