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
abstract class NodeSelector {

	static final NodeSelector ANY = new AnyNode();
	static final NodeSelector STRUCTURED = new StructuredNode();
	static final NodeSelector MATCHABLE_PATTERN_ROOT = new MatchablePatternRoot();
	static final NodeSelector MATCHABLE_PATTERN_VALUE = new MatchablePatternValue();
	static final NodeSelector MATCHABLE_DISJUNCT = new MatchableDisjunct();

	static private class AnyNode extends NodeSelector {

		boolean select(NodeX node) {

			return true;
		}

		boolean anyMatches(Names nodes) {

			return !nodes.isEmpty();
		}
	}

	static private abstract class SelectiveSelector extends NodeSelector {

		boolean anyMatches(Names nodes) {

			for (Name n : nodes) {

				if (select((NodeX)n)) {

					return true;
				}
			}

			return false;
		}
	}

	static private class StructuredNode extends SelectiveSelector {

		boolean select(NodeX node) {

			PatternMatcher p = node.getProfilePatternMatcher();

			return p != null && select(p.getPattern().getProfileRelations().getAll());
		}

		boolean select(Collection<Relation> rels) {

			return !rels.isEmpty();
		}
	}

	static private abstract class MatchablePatternNode extends StructuredNode {

		boolean select(NodeX node) {

			return node.definitionRefed(getMatchRole()) || super.select(node);
		}

		boolean select(Collection<Relation> rels) {

			for (Relation r : rels) {

				if (r instanceof NodeRelation && targetFor((NodeRelation)r)) {

					return true;
				}
			}

			return false;
		}

		abstract MatchRole getMatchRole();

		private boolean targetFor(NodeRelation rel) {

			NodeProperty p = rel.getNodeProperty();

			return p.definitionRefed() && p.anyChains();
		}
	}

	static private class MatchablePatternRoot extends MatchablePatternNode {

		MatchRole getMatchRole() {

			return MatchRole.PATTERN_ROOT;
		}
	}

	static private class MatchablePatternValue extends MatchablePatternNode {

		MatchRole getMatchRole() {

			return MatchRole.PATTERN_VALUE;
		}
	}

	static private class MatchableDisjunct extends SelectiveSelector {

		boolean select(NodeX node) {

			return node.definitionRefed(MatchRole.DISJUNCT);
		}
	}

	abstract boolean select(NodeX node);

	abstract boolean anyMatches(Names nodes);
}
