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
abstract class ChainBasedProfileRelationsExpander {

	private List<Relation> allExpansions = new ArrayList<Relation>();

	private List<SomeRelation> passExpansions = new ArrayList<SomeRelation>();
	private List<SomeRelation> nextPassExpanders;

	private class ExpansionCollector {

		private PropertyChain chain;
		private int tailSubsIndex = 0;

		private Set<NodeX> visitedTargets = new HashSet<NodeX>();

		ExpansionCollector(PropertyChain chain) {

			this.chain = chain;
		}

		void collectFromTargets(SomeRelation current) {

			NodeValue v = current.getTarget().asNodeValue();

			if (v != null) {

				NodeX t = v.getValueNode();

				if (visitedTargets.add(t)) {

					collectFromTarget(t);
				}
			}
		}

		private void collectFromTarget(NodeX target) {

			for (Relation r : resolveRelationExpansions(target)) {

				if (r instanceof SomeRelation) {

					collectFromRelation((SomeRelation)r);
				}
			}
		}

		private void collectFromRelation(SomeRelation current) {

			if (chain.hasTailSub(current.getProperty(), tailSubsIndex)) {

				if (chain.lastTailSub(tailSubsIndex)) {

					addExpansion(createLinkRelation(current));
				}
				else {

					tailSubsIndex++;
					collectFromTargets(current);
					tailSubsIndex--;
				}
			}
		}

		private SomeRelation createLinkRelation(SomeRelation endSub) {

			return chain.createLinkRelation(endSub.getNodeValueTarget());
		}
	}

	ChainBasedProfileRelationsExpander(SomeRelation relation) {

		nextPassExpanders = Collections.singletonList(relation);

		collectExpansions(getExpansionCollectors(relation));
	}

	Collection<Relation> getAllExpansions() {

		return allExpansions;
	}

	abstract Collection<Relation> resolveRelationExpansions(NodeX node);

	private void collectExpansions(List<ExpansionCollector> collectors) {

		while (true) {

			for (ExpansionCollector c : collectors) {

				for (SomeRelation r : nextPassExpanders) {

					c.collectFromTargets(r);
				}
			}

			if (passExpansions.isEmpty()) {

				break;
			}

			nextPassExpanders = passExpansions;
			passExpansions = new ArrayList<SomeRelation>();
		}
	}

	private List<ExpansionCollector> getExpansionCollectors(SomeRelation relation) {

		List<ExpansionCollector> collectors = new ArrayList<ExpansionCollector>();

		for (PropertyChain chain : relation.getAllChains()) {

			collectors.add(new ExpansionCollector(chain));
		}

		return collectors;
	}

	private void addExpansion(SomeRelation relation) {

		allExpansions.add(relation);
		passExpansions.add(relation);
	}
}
