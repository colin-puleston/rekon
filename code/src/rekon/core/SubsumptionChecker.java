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
class SubsumptionChecker {

	private PatternChecker patternChecker = new PatternChecker();
	private DisjunctionChecker disjunctionChecker = new DisjunctionChecker();

	private abstract class NodeMatcherChecker<C> {

		boolean check(NodeX dn, C defn, NodeX cn, C candidate) {

			if (defn != candidate) {

				if (!dn.subsumes(cn) && subsumption(defn, candidate)) {

					addSubsumption(dn, cn);

					return true;
				}
			}

			return false;
		}

		abstract boolean subsumption(C defn, C candidate);

		private void addSubsumption(NodeX subsumer, NodeX subsumed) {

			subsumed.getNodeClassifier().addNewInferredSubsumer(subsumer);
		}
	}

	private class PatternChecker extends NodeMatcherChecker<Pattern> {

		boolean check(NodeX dn, Pattern defn, PatternMatcher candidate) {

			return check(dn, defn, candidate.getNode(), candidate.getPattern());
		}

		boolean subsumption(Pattern defn, Pattern candidate) {

			return SubsumptionChecker.this.subsumption(defn, candidate);
		}
	}

	private class DisjunctionChecker extends NodeMatcherChecker<DisjunctionMatcher> {

		boolean check(DisjunctionMatcher defn, DisjunctionMatcher candidate) {

			return check(defn.getNode(), defn, candidate.getNode(), candidate);
		}

		boolean subsumption(DisjunctionMatcher defn, DisjunctionMatcher candidate) {

			return defn.subsumesDisjunction(candidate);
		}
	}

	boolean check(PatternMatcher defn, PatternMatcher candidate) {

		return patternChecker.check(defn.getNode(), defn.getPattern(), candidate);
	}

	boolean check(DisjunctionMatcher defn, DisjunctionMatcher candidate) {

		return disjunctionChecker.check(defn, candidate);
	}

	boolean subsumption(Pattern defn, Pattern profile) {

		return defn.subsumes(profile);
	}
}
