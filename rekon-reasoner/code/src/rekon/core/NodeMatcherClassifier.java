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

/**
 * @author Colin Puleston
 */
abstract class NodeMatcherClassifier {

	private GeneralSubsumptionChecker generalSubsumptions = new GeneralSubsumptionChecker();
	private PatternSubsumptionChecker patternSubsumptions = new PatternSubsumptionChecker();

	private abstract class SubsumptionChecker<D extends NodeMatcher, C extends NodeMatcher> {

		boolean check(D defn, C candidate) {

			NodeX dn = defn.getNode();
			NodeX cn = candidate.getNode();

			if (dn != cn && !dn.subsumes(cn) && subsumption(defn, candidate)) {

				addSubsumption(dn, cn);

				return true;
			}

			return false;
		}

		abstract boolean subsumption(D defn, C candidate);

		private void addSubsumption(NodeX subsumer, NodeX subsumed) {

			subsumed.getNodeClassifier().addNewInferredSubsumer(subsumer);
		}
	}

	private class GeneralSubsumptionChecker
					extends
						SubsumptionChecker<NodeMatcher, NodeMatcher> {

		boolean subsumption(NodeMatcher defn, NodeMatcher candidate) {

			return defn.subsumes(candidate);
		}
	}

	private class PatternSubsumptionChecker
					extends
						SubsumptionChecker<PatternMatcher, PatternMatcher> {

		boolean subsumption(PatternMatcher defn, PatternMatcher candidate) {

			return patternSubsumption(defn.getPattern(), candidate.getPattern());
		}
	}

	boolean checkSubsumption(NodeMatcher defn, NodeMatcher candidate) {

		return generalSubsumptions.check(defn, candidate);
	}

	boolean checkSubsumption(PatternMatcher defn, PatternMatcher candidate) {

		return patternSubsumptions.check(defn, candidate);
	}

	boolean patternSubsumption(Pattern defn, Pattern candidate) {

		return defn.subsumes(candidate);
	}
}
