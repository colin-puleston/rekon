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
class PatternSubsumeds {

	private PotentialPatternSubsumeds potentials;

	private ClassSubsumptions classSubsumptions;
	private NodeSubsumptions allNodeSubsumptions;

	private class NodeSubsumptions {

		NameSet find(NodePattern pattern) {

			NameSet matches = new NameSet();

			for (PatternNode pn : potentials.getPotentialsFor(pattern)) {

				Name n = pn.getName();

				if (requiredCandidate(n) && pn.subsumedBy(pattern)) {

					matches.add(n);
				}
			}

			return matches;
		}

		boolean requiredCandidate(Name n) {

			return true;
		}
	}

	private class ClassSubsumptions extends NodeSubsumptions {

		private NameSet filterNames = null;

		NameSet find(NodePattern pattern, NameSet filterNames) {

			this.filterNames = filterNames;

			return find(pattern);
		}

		boolean requiredCandidate(Name n) {

			if (n instanceof ClassName) {

				return filterNames == null || filterNames.contains(n);
			}

			return false;
		}
	}

	PatternSubsumeds(MatchableNodes matchables) {

		potentials = createPotentials(matchables);

		classSubsumptions = new ClassSubsumptions();
		allNodeSubsumptions = new NodeSubsumptions();
	}

	void checkAddInstanceOption(InstanceName name) {

		potentials.checkAddInstanceOption(name);
	}

	void checkRemoveInstanceOption(InstanceName name) {

		potentials.checkRemoveInstanceOption(name);
	}

	NameSet inferSubsumedClasses(NodePattern pattern) {

		return classSubsumptions.find(pattern, null);
	}

	NameSet inferSubsumedClasses(NodePattern pattern, NameSet filterNames) {

		return classSubsumptions.find(pattern, filterNames);
	}

	NameSet inferAllSubsumedNodes(NodePattern pattern) {

		return allNodeSubsumptions.find(pattern);
	}

	private PotentialPatternSubsumeds createPotentials(MatchableNodes matchables) {

		return new PotentialLocalPatternSubsumeds(getPotentialCandidates(matchables));
	}

	private List<PatternNode> getPotentialCandidates(MatchableNodes matchables) {

		List<PatternNode> candidates = new ArrayList<PatternNode>();

		for (PatternNode pn : matchables.getAllPatternNodes()) {

			if (pn.getName().mapped()) {

				candidates.add(pn);
			}
		}

		return candidates;
	}
}
