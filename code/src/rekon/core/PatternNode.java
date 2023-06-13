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
class PatternNode extends MatchableNode<PatternNode> {

	private NodePattern profile;
	private Set<NodePattern> definitions = new HashSet<NodePattern>();

	public String toString() {

		return getClass().getSimpleName() + "(" + getName().getLabel() + ")";
	}

	PatternNode(NodeName name) {

		this(name, new NodePattern(name));
	}

	PatternNode(NodeName name, NodePattern profile) {

		super(name);

		this.profile = profile;
	}

	void addDefinition(NodePattern defn) {

		definitions.add(defn);

		addNameSubsumers(defn.getNames());

		profile = profile.combineWith(defn);

		defn.registerDefinitionRefedNames();
	}

	void addProfileRelation(Relation relation) {

		profile = profile.extend(relation);
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

	boolean subsumesNode(NodeName n) {

		return false;
	}

	boolean subsumes(PatternNode other) {

		return subsumes(other.profile);
	}

	boolean subsumes(NodePattern pattern) {

		for (NodePattern d : definitions) {

			if (d.subsumes(pattern)) {

				return true;
			}
		}

		return false;
	}

	boolean subsumedBy(NodePattern pattern) {

		return pattern.subsumes(profile);
	}

	boolean classifiable(boolean initialPass) {

		return profile.classifiable(initialPass);
	}

	void acceptVisitor(MatchableNodeVisitor visitor) {

		visitor.visit(this);
	}

	Class<PatternNode> getMatchableClass() {

		return PatternNode.class;
	}

	private void addNameSubsumers(Names subsumers) {

		NameClassifier classifier = getName().getClassifier();

		for (Name s : subsumers.getNames()) {

			if (!s.rootName()) {

				classifier.addAssertedSubsumer(s);
			}
		}
	}
}
