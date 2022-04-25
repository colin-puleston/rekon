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
abstract class Classifier {

	void classify() {

		Collection<MatchableNode> passCandidates = getMatchableCandidates();

		do {

			passCandidates = perfomPass(passCandidates);
		}
		while (!passCandidates.isEmpty());
	}

	Collection<MatchableNode> perfomPass(Collection<MatchableNode> passCandidates) {

		checkSubsumptions(passCandidates);
		setNewInferredSubsumptions(passCandidates);

		if (!dynamicClassification()) {

			expandNewInferredSubsumptions();
		}

		List<MatchableNode> reclassifiables = findAnyReclassifiables();

		resetNewInferredSubsumptions();
		resetAllReclassifiablesReferences(reclassifiables);

		return reclassifiables;
	}

	abstract void checkSubsumptions(Collection<MatchableNode> passCandidates);

	abstract boolean dynamicClassification();

	abstract Collection<NodeName> getAllCandidates();

	abstract Collection<MatchableNode> getMatchableCandidates();

	void checkSubsumption(MatchableNode defined, NodePattern defn, MatchableNode cand) {

		if (cand != defined && !defined.getName().subsumes(cand.getName())) {

			if (defn.subsumes(cand.getProfile())) {

				cand.checkNewInferredSubsumer(defined);
			}
		}
	}

	private void setNewInferredSubsumptions(Collection<MatchableNode> passCandidates) {

		for (MatchableNode c : passCandidates) {

			c.setNewInferredSubsumptions();
		}
	}

	private void expandNewInferredSubsumptions() {

		while (true) {

			boolean expansions = false;

			for (NodeName c : getAllCandidates()) {

				expansions |= c.getClassifier().expandNewInferreds();
			}

			if (!expansions) {

				break;
			}
		}
	}

	private List<MatchableNode> findAnyReclassifiables() {

		List<MatchableNode> reclassifiables = new ArrayList<MatchableNode>();

		for (MatchableNode c : getMatchableCandidates()) {

			if (c.reclassifiable()) {

				reclassifiables.add(c);
			}
		}

		return reclassifiables;
	}

	private void resetNewInferredSubsumptions() {

		for (Name c : getAllCandidates()) {

			c.getClassifier().resetNewInferreds();
		}
	}

	private void resetAllReclassifiablesReferences(List<MatchableNode> reclassifiables) {

		for (MatchableNode m : reclassifiables) {

			m.getProfile().resetAllReferences();

			for (NodePattern d : m.getDefinitions()) {

				d.resetAllReferences();
			}
		}
	}
}
