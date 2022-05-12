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
public class MatchableNodes {

	private List<MatchableNode> all = new ArrayList<MatchableNode>();
	private List<MatchableNode> defineds = new ArrayList<MatchableNode>();

	private List<NodeName> names = new ArrayList<NodeName>();

	public MatchableNode checkAddForClass(
							ClassName name,
							Collection<NodePattern> defns,
							Collection<Relation> relations) {

		if (defns.isEmpty()) {

			return checkAddFor(name, relations);
		}

		return addDefinition(name, defns, relations);
	}

	public MatchableNode checkAddForIndividual(
							IndividualName name,
							Collection<Relation> relations) {

		return checkAddFor(name, relations);
	}

	MatchableNode addForFreeClass(ClassName name, Collection<NodePattern> defns) {

		return addDefinition(name, defns, Collections.emptySet());
	}

	Collection<MatchableNode> getAll() {

		return all;
	}

	Collection<MatchableNode> copyAll() {

		return new ArrayList<MatchableNode>(all);
	}

	Collection<MatchableNode> getDefineds() {

		return defineds;
	}

	Collection<NodeName> getAllNames() {

		return names;
	}

	private MatchableNode checkAddFor(NodeName name, Collection<Relation> relations) {

		if (name.getClassifier().multipleAsserteds() || !relations.isEmpty()) {

			return add(name, new NodePattern(name, relations));
		}

		return null;
	}

	private MatchableNode addDefinition(
								ClassName name,
								Collection<NodePattern> defns,
								Collection<Relation> relations) {

		MatchableNode m = add(name, createProfile(name, defns, relations));

		m.addDefinitions(defns);
		defineds.add(m);

		return m;
	}

	private MatchableNode add(NodeName name, NodePattern profile) {

		MatchableNode m = new MatchableNode(name, profile);

		all.add(m);
		names.add(m.getName());

		return m;
	}

	private NodePattern createProfile(
							ClassName name,
							Collection<NodePattern> defns,
							Collection<Relation> relations) {

		NodePattern profile = new NodePattern(name);

		for (NodePattern defn : defns) {

			profile = profile.combineWith(defn);
		}

		return relations.isEmpty() ? profile : profile.extend(relations);
	}
}
