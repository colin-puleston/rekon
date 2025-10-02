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
abstract class NodeSelector {

	static final NodeSelector ANY = new AnyNode();
	static final NodeSelector SIMPLE_PATTERN_NODE = new SimplePatternNode();
	static final NodeSelector NESTED_PATTERN_ROOT = new NestedPatternRoot();
	static final NodeSelector NESTED_PATTERN_VALUE = new NestedPatternValue();

	static private class AnyNode extends NodeSelector {

		boolean select(NodeX node) {

			return true;
		}

		boolean anyMatches(Names nodes) {

			return !nodes.isEmpty();
		}
	}

	static private abstract class PatternNode extends NodeSelector {

		boolean select(NodeX node) {

			return node.definitionRefed(getMatchRole());
		}

		boolean anyMatches(Names nodes) {

			for (NodeX n : nodes.asNodes()) {

				if (select(n)) {

					return true;
				}
			}

			return false;
		}

		abstract MatchRole getMatchRole();
	}

	static private class SimplePatternNode extends PatternNode {

		MatchRole getMatchRole() {

			return MatchRole.SIMPLE_PATTERN_NODE;
		}
	}

	static private class NestedPatternRoot extends PatternNode {

		MatchRole getMatchRole() {

			return MatchRole.NESTED_PATTERN_ROOT;
		}
	}

	static private class NestedPatternValue extends PatternNode {

		MatchRole getMatchRole() {

			return MatchRole.NESTED_PATTERN_VALUE;
		}
	}

	abstract boolean select(NodeX node);

	abstract boolean anyMatches(Names nodes);
}
