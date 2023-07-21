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

		NameSet find(Pattern pattern) {

			NameSet matches = new NameSet();

			for (PatternMatcher pn : potentials.getPotentialsFor(pattern)) {

				Name n = pn.getNode();

				if (requiredCandidate(n) && pn.subsumedByPattern(pattern)) {

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

		NameSet find(Pattern pattern, NameSet filterNames) {

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

	PatternSubsumeds(NodeMatchers nodeMatchers) {

		potentials = createPotentials(nodeMatchers);

		classSubsumptions = new ClassSubsumptions();
		allNodeSubsumptions = new NodeSubsumptions();
	}

	void checkAddInstanceOption(InstanceName name) {

		potentials.checkAddInstanceOption(name);
	}

	void checkRemoveInstanceOption(InstanceName name) {

		potentials.checkRemoveInstanceOption(name);
	}

	NameSet inferSubsumedClasses(Pattern pattern) {

		return classSubsumptions.find(pattern, null);
	}

	NameSet inferSubsumedClasses(Pattern pattern, NameSet filterNames) {

		return classSubsumptions.find(pattern, filterNames);
	}

	NameSet inferAllSubsumedNodes(Pattern pattern) {

		return allNodeSubsumptions.find(pattern);
	}

	private PotentialPatternSubsumeds createPotentials(NodeMatchers nodeMatchers) {

		return new PotentialLocalPatternSubsumeds(getPotentialCandidates(nodeMatchers));
	}

	private List<PatternMatcher> getPotentialCandidates(NodeMatchers nodeMatchers) {

		List<PatternMatcher> candidates = new ArrayList<PatternMatcher>();

		for (PatternMatcher pp : nodeMatchers.getProfilePatterns()) {

			if (pp.getNode().mapped()) {

				candidates.add(pp);
			}
		}

		return candidates;
	}
}
