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

	boolean check(PatternNode defined, NodePattern defn, PatternNode candidate) {

		return check(defined.getName(), defn, candidate);
	}

	boolean check(DefinitionPattern defn, PatternNode candidate) {

		return check(defn.getNode(), defn.getDefinition(), candidate);
	}

	boolean check(DisjunctionNode defn, DisjunctionNode candidate) {

		if (defn != candidate && subsumption(defn, candidate)) {

			addSubsumption(defn.getName(), candidate.getName());

			return true;
		}

		return false;
	}

	boolean subsumption(NodePattern defn, NodePattern profile) {

		return defn.subsumes(profile);
	}

	boolean subsumption(DisjunctionNode defn, DisjunctionNode candidate) {

		return defn.subsumes(candidate);
	}

	private boolean check(NodeName definedName, NodePattern defn, PatternNode candidate) {

		NodeName cn = candidate.getName();

		if (cn != definedName && !definedName.subsumes(cn)) {

			if (subsumption(defn, candidate.getProfile())) {

				addSubsumption(definedName, cn);

				return true;
			}
		}

		return false;
	}

	private void addSubsumption(NodeName subsumer, NodeName subsumed) {

		subsumed.getNodeClassifier().addNewInferredSubsumer(subsumer);
	}
}
