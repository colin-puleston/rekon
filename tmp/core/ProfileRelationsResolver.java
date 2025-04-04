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
class ProfileRelationsResolver {

	private class ChainBasedExpander extends ChainBasedProfileRelationsExpander {

		ChainBasedExpander(SomeRelation relation) {

			super(relation);
		}

		Collection<Relation> resolveProfileRelations(NodeX node) {

			return ProfileRelationsResolver.this.resolveProfileRelations(node);
		}
	}

	private class NodeRelationsResolver {

		private NodeX node;
		private RelationCollector collector;

		NodeRelationsResolver(NodeX node, RelationCollector collector) {

			this.node = node;
			this.collector = collector;
		}

		void collectFromSubsumers() {

			for (NodeX s : node.getSubsumers().asNodes()) {

				for (PatternMatcher p : s.getProfilePatternMatcherAsList()) {

					for (Relation r : getProfileRelations(p)) {

						collector.absorb(r);
					}
				}
			}
		}

		void collectChainExpansions() {

			for (Relation r : collector.copyCollected()) {

				for (Relation sr : getAllChainBasedExpansions(r)) {

					collector.absorb(sr);
				}
			}
		}

		private Collection<Relation> getAllChainBasedExpansions(Relation relation) {

			if (relation instanceof SomeRelation) {

				SomeRelation sr = (SomeRelation)relation;

				if (sr.chainExpandable()) {

					return new ChainBasedExpander(sr).getAllExpansions();
				}
			}

			return Collections.emptySet();
		}
	}

	void collectForNode(NodeX node, RelationCollector collector) {

		NodeRelationsResolver resolver = new NodeRelationsResolver(node, collector);

		resolver.collectFromSubsumers();
		resolver.collectChainExpansions();
	}

	private Collection<Relation> resolveProfileRelations(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		return p != null
				? getProfileRelations(p)
				: getInertNodeProfileRelations(node);
	}

	private Collection<Relation> getInertNodeProfileRelations(NodeX node) {

		RelationCollector collector = new RelationCollector();
		NodeRelationsResolver resolver = new NodeRelationsResolver(node, collector);

		resolver.collectFromSubsumers();

		return collector.getCollected();
	}

	private Collection<Relation> getProfileRelations(PatternMatcher p) {

		return p.getPattern().getProfileRelations().getAll();
	}
}
