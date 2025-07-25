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
class FilteringLinkedNameCollector extends FilteringNameCollector {

	private int startRank;
	private int stopRank;

	private Deque<NodeX> profileLinkingNodes = new ArrayDeque<NodeX>();

	private class LinkedRankCollector extends RankCollector {

		private NameSet profileValueNodesCollected = new NameSet();

		void collectForSingleValueNode(NodeX n) {

			if (definition()) {

				collectForDefinitionNode(n);
			}
			else {

				collectForProfileNode(n);
			}
		}

		boolean preCollectRank(int rank) {

			return rank < startRank;
		}

		boolean continueForNextRelationsRank(int rank) {

			return rank != stopRank - 1;
		}

		private void collectForDefinitionNode(NodeX n) {

			if (n.local()) {

				for (PatternMatcher d : n.getDefinitionMatchers()) {

					d.getPattern().collectNames(this);
				}
			}
			else {

				collectName(n);
			}
		}

		private void collectForProfileNode(NodeX n) {

			if (profileValueNodesCollected.add(n)) {

				collectName(n);

				if (continueForNextRelationsRank() && !profileLinkingNodes.contains(n)) {

					collectFromRelations(n);
				}
			}
		}

		private void collectFromRelations(NodeX n) {

			Collection<Relation> rels = getRelations(n);

			if (!rels.isEmpty()) {

				profileLinkingNodes.push(n);

				Relation.collectNamesFromNextRankRelations(this, rels);

				profileLinkingNodes.pop();
			}
		}

		private Collection<Relation> getRelations(NodeX n) {

			PatternMatcher p = n.getProfileMatcher();

			if (p != null) {

				return p.getPattern().getProfileRelations().getAll();
			}

			return new ProfileRelationsResolver().collectFromSubsumers(n);
		}

		private void collectNamesFromSubsumerRelations(NodeX n) {

		}
	}

	FilteringLinkedNameCollector(boolean definition) {

		this(definition, 0, -1);
	}

	FilteringLinkedNameCollector(boolean definition, int startRank, int stopRank) {

		super(definition);

		this.startRank = startRank;
		this.stopRank = stopRank;
	}

	RankCollector createRankCollector() {

		return new LinkedRankCollector();
	}
}

