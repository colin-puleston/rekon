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
class MatchableNode {

	private NodeName name;

	private NodePattern profile;
	private Set<NodePattern> definitions = new HashSet<NodePattern>();

	private Set<MatchableNode> newInferredSubsumers = new HashSet<MatchableNode>();
	private Set<MatchableNode> allInferredSubsumers = new HashSet<MatchableNode>();

	public String toString() {

		return getClass().getSimpleName() + "(" + name.getLabel() + ")";
	}

	MatchableNode(NodeName name, NodePattern profile) {

		this.name = name;
		this.profile = profile;

		name.setMatchable(this);
	}

	void addDefinition(NodePattern defn) {

		definitions.add(defn);

		profile = profile.combineWith(defn);

		defn.registerDefinitionNames();
	}

	void checkNewInferredSubsumer(MatchableNode m) {

		if (allInferredSubsumers.add(m)) {

			newInferredSubsumers.add(m);
		}
	}

	void setNewInferredSubsumptions() {

		for (MatchableNode subsumer : newInferredSubsumers) {

			name.getClassifier().checkAddInferredSubsumer(subsumer.name);
		}

		newInferredSubsumers.clear();
	}

	void resetSignatureRefs() {

		profile.resetSignatureRefs();
	}

	boolean reclassifiable() {

		return name.reclassifiable() || profile.reclassifiable();
	}

	NodeName getName() {

		return name;
	}

	NodePattern getProfile() {

		return profile;
	}

	boolean hasDefinitions() {

		return !definitions.isEmpty();
	}

	Collection<NodePattern> getDefinitions() {

		return definitions;
	}

	boolean subsumedBy(NodePattern pattern) {

		return pattern.subsumes(profile);
	}

	boolean subsumes(NodePattern desc) {

		for (NodePattern d : definitions) {

			if (d.subsumes(desc)) {

				return true;
			}
		}

		return false;
	}
}
