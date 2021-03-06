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
class PatternSubsumptions {

	static private abstract class SubsumptionsFinder {

		private PotentialSubsumeds potentialSubsumeds;

		SubsumptionsFinder(MatchableNodes matchables) {

			potentialSubsumeds = createPotentialSubsumeds(matchables);
		}

		NameSet find(NodePattern pattern) {

			NameSet matches = new NameSet();

			for (MatchableNode c : potentialSubsumeds.getPotentialsFor(pattern)) {

				if (requiredCandidate(c) && c.subsumedBy(pattern)) {

					matches.add(c.getName());
				}
			}

			return matches;
		}

		abstract Class<? extends NodeName> getNodeType();

		abstract boolean requiredCandidate(MatchableNode c);

		private PotentialSubsumeds createPotentialSubsumeds(MatchableNodes matchables) {

			return new PotentialSubsumeds(getPotentialCandidates(matchables), true);
		}

		private List<MatchableNode> getPotentialCandidates(MatchableNodes matchables) {

			List<MatchableNode> candidates = new ArrayList<MatchableNode>();

			for (MatchableNode c : matchables.getAll()) {

				if (getNodeType().isAssignableFrom(c.getName().getClass())) {

					candidates.add(c);
				}
			}

			return candidates;
		}
	}

	static private class ClassSubsumptions extends SubsumptionsFinder {

		private NameSet filterNames = null;

		ClassSubsumptions(MatchableNodes matchables) {

			super(matchables);
		}

		NameSet find(NodePattern pattern, NameSet filterNames) {

			this.filterNames = filterNames;

			return find(pattern);
		}

		boolean requiredCandidate(MatchableNode c) {

			return filterNames == null || filterNames.contains(c.getName());
		}

		Class<? extends NodeName> getNodeType() {

			return ClassName.class;
		}
	}

	static private class InstanceSubsumptions extends SubsumptionsFinder {

		InstanceSubsumptions(MatchableNodes matchables) {

			super(matchables);
		}

		boolean requiredCandidate(MatchableNode c) {

			return true;
		}

		Class<? extends NodeName> getNodeType() {

			return IndividualName.class;
		}
	}

	private ClassSubsumptions classSubsumptions;
	private InstanceSubsumptions instanceSubsumptions;

	PatternSubsumptions(Ontology ontology) {

		MatchableNodes matchables = ontology.getMatchables();

		classSubsumptions = new ClassSubsumptions(matchables);
		instanceSubsumptions = new InstanceSubsumptions(matchables);
	}

	NameSet inferSubsumedClasses(NodePattern pattern) {

		return classSubsumptions.find(pattern, null);
	}

	NameSet inferSubsumedClasses(NodePattern pattern, NameSet filterNames) {

		return classSubsumptions.find(pattern, filterNames);
	}

	NameSet inferSubsumedInstances(NodePattern pattern) {

		return instanceSubsumptions.find(pattern);
	}
}
