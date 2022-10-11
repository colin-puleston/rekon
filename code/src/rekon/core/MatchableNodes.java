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
class MatchableNodes {

	private List<MatchableNode> matchables = new ArrayList<MatchableNode>();
	private List<NodeName> names = new ArrayList<NodeName>();

	private class InverseRelationsAdder {

		private List<MatchableNode> newMatchables = new ArrayList<MatchableNode>();

		InverseRelationsAdder() {

			for (MatchableNode m : matchables) {

				if (m.getName() instanceof IndividualName) {

					addAnyFor(m);
				}
			}

			for (MatchableNode m : newMatchables) {

				add(m);
			}
		}

		private void addAnyFor(MatchableNode m) {

			for (Relation r : m.getProfile().getDirectRelations()) {

				if (r instanceof SomeRelation) {

					addAnyFor(m.getName(), (SomeRelation)r);
				}
			}
		}

		private void addAnyFor(NodeName forwardSource, SomeRelation forwardRel) {

			Collection<ObjectPropertyName> ips = forwardRel.getObjectProperty().getInverses();

			if (!ips.isEmpty()) {

				NodeName invSource = forwardRel.getNodeTarget().toSingleName();

				if (invSource != null) {

					MatchableNode m = resolveMatchable(invSource);
					NodeValue invTarget = new NodeValue(forwardSource);

					for (ObjectPropertyName ip : ips) {

						m.addProfileRelation(new SomeRelation(ip, invTarget));
					}
				}
			}
		}

		private MatchableNode resolveMatchable(NodeName name) {

			MatchableNode m = name.getMatchable();

			if (m == null) {

				m = new MatchableNode(name, new NodePattern(name));

				newMatchables.add(m);
			}

			return m;
		}
	}

	MatchableNode add(NodeName name, NodePattern profile) {

		return add(new MatchableNode(name, profile));
	}

	MatchableNode addDefinitions(ClassName name, Collection<NodePattern> defns) {

		MatchableNode m = name.getMatchable();

		if (m == null) {

			m = add(name, new NodePattern(name));
		}

		for (NodePattern defn : defns) {

			m.addDefinition(defn);
		}

		return m;
	}

	void addAllInverseRelations() {

		new InverseRelationsAdder();
	}

	List<MatchableNode> getAll() {

		return matchables;
	}

	List<MatchableNode> copyAll() {

		return new ArrayList<MatchableNode>(matchables);
	}

	List<NodeName> getAllNames() {

		return names;
	}

	private MatchableNode add(MatchableNode m) {

		matchables.add(m);
		names.add(m.getName());

		return m;
	}
}
