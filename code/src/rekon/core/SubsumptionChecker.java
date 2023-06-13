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

	private PatternNodeChecker patternNodeChecker = new PatternNodeChecker();
	private DisjunctionNodeChecker disjunctionNodeChecker = new DisjunctionNodeChecker();

	private abstract class MatchableNodeChecker<C> {

		boolean check(NodeName dn, C defn, NodeName cn, C candidate) {

			if (defn != candidate) {

				if (!dn.subsumes(cn) && subsumption(defn, candidate)) {

					addSubsumption(dn, cn);

					return true;
				}
			}

			return false;
		}

		abstract boolean subsumption(C defn, C candidate);

		private void addSubsumption(NodeName subsumer, NodeName subsumed) {

			subsumed.getNodeClassifier().addNewInferredSubsumer(subsumer);
		}
	}

	private class PatternNodeChecker extends MatchableNodeChecker<NodePattern> {

		boolean check(NodeName dn, NodePattern defn, PatternNode candidate) {

			return check(dn, defn, candidate.getName(), candidate.getProfile());
		}

		boolean subsumption(NodePattern defn, NodePattern candidate) {

			return SubsumptionChecker.this.subsumption(defn, candidate);
		}
	}

	private class DisjunctionNodeChecker extends MatchableNodeChecker<DisjunctionNode> {

		boolean check(DisjunctionNode defn, DisjunctionNode candidate) {

			return check(defn.getName(), defn, candidate.getName(), candidate);
		}

		boolean subsumption(DisjunctionNode defn, DisjunctionNode candidate) {

			return defn.subsumes(candidate);
		}
	}

	boolean check(PatternNode defined, NodePattern defn, PatternNode candidate) {

		return patternNodeChecker.check(defined.getName(), defn, candidate);
	}

	boolean check(DefinitionPattern defn, PatternNode candidate) {

		return patternNodeChecker.check(defn.getNode(), defn.getDefinition(), candidate);
	}

	boolean check(DisjunctionNode defn, DisjunctionNode candidate) {

		return disjunctionNodeChecker.check(defn, candidate);
	}

	boolean subsumption(NodePattern defn, NodePattern profile) {

		return defn.subsumes(profile);
	}
}
