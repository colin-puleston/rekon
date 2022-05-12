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
class DynamicClassifier extends Classifier {

	private PotentialSubsumers definedsFilter;

	DynamicClassifier(Ontology ontology) {

		definedsFilter = new PotentialSubsumers(ontology.getMatchables().getDefineds());
	}

	void classify(MatchableNodes dynamicMatchables, Collection<MatchableNode> defineds) {

		for (MatchableNode c : dynamicMatchables.getAll()) {

			classifyCandidate(c, defineds);
		}
	}

	private void classifyCandidate(MatchableNode c, Collection<MatchableNode> defineds) {

		do {

			checkCandidateSubsumptions(c, defineds);
			c.setNewInferredSubsumptions();
		}
		while (checkReclassifiable(c));
	}

	private void checkCandidateSubsumptions(MatchableNode c, Collection<MatchableNode> defineds) {

		if (defineds == null) {

			defineds = definedsFilter.getPotentialsFor(c.getProfile());
		}

		for (MatchableNode defined : defineds) {

			checkCandidateSubsumptions(defined, c);
		}
	}

	private void checkCandidateSubsumptions(MatchableNode defined, MatchableNode c) {

		for (NodePattern defn : defined.getDefinitions()) {

			checkSubsumptionXXX(defined, defn, c);
		}
	}

	static void checkSubsumptionXXX(MatchableNode defined, NodePattern defn, MatchableNode cand) {

		if (cand != defined && !defined.getName().subsumes(cand.getName())) {

			if (defn.subsumes(cand.getProfile())) {

				cand.checkNewInferredSubsumer(defined);
			}
		}
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
}
