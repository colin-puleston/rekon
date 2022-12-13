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
class DynamicClassifier {

	private PotentialDynamicSubsumers definedsFilter;
	private SubsumptionChecker subsumptionChecker = new SubsumptionChecker();

	DynamicClassifier(Ontology ontology) {

		definedsFilter = new PotentialDynamicSubsumers(ontology.getMatchables().getAll());
	}

	void classify(MatchableNodes dynamicMatchables, Collection<MatchableNode> defineds) {

		for (MatchableNode c : dynamicMatchables.getAll()) {

			classifyCandidate(c, defineds);
		}
	}

	private void classifyCandidate(MatchableNode c, Collection<MatchableNode> defineds) {

		do {

			if (defineds == null) {

				checkCandidateSubsumptions(c);
			}
			else {

				checkCandidateSubsumptions(c, defineds);
			}
		}
		while (checkReclassifiable(c));
	}

	private void checkCandidateSubsumptions(MatchableNode c) {

		for (NodeDefinition defn : definedsFilter.getPotentialsFor(c.getProfile())) {

			subsumptionChecker.check(defn, c);
		}
	}

	private void checkCandidateSubsumptions(MatchableNode c, Collection<MatchableNode> defineds) {

		for (MatchableNode defined : defineds) {

			for (NodePattern defn : defined.getDefinitions()) {

				subsumptionChecker.check(defined, defn, c);
			}
		}
	}

	private boolean checkReclassifiable(MatchableNode c) {

		if (c.getName().getNodeClassifier().absorbNewInferredSubsumers()) {

			NodePattern p = c.getProfile();

			if (p.potentialSignatureUpdates()) {

				p.resetSignatureRefs();
			}

			return true;
		}

		return false;
	}
}
