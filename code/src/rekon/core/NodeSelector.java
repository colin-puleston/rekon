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

	static final NodeSelector ANY = new Any();
	static final NodeSelector STRUCTURED = new Structured();
	static final NodeSelector CLASSIFY_TARGET_PATTERN_ROOT = new ClassifyTargetPatternRoot();
	static final NodeSelector CLASSIFY_TARGET_PATTERN_VALUE = new ClassifyTargetPatternValue();
	static final NodeSelector CLASSIFY_TARGET_DISJUNCT = new ClassifyTargetDisjunct();

	static NodeSelector structureFor(PropertyName property) {

		return new StructureFor(property);
	}

	static private class Any extends NodeSelector {

		boolean select(NodeName node) {

			return true;
		}

		boolean anyMatches(Names nodes) {

			return !nodes.isEmpty();
		}
	}

	static abstract private class SelectiveNodeSelector extends NodeSelector {

		boolean anyMatches(Names nodes) {

			for (Name n : nodes.getNames()) {

				if (select((NodeName)n)) {

					return true;
				}
			}

			return false;
		}
	}

	static abstract private class StructuredNodeSelector extends SelectiveNodeSelector {

		boolean select(NodeName node) {

			PatternMatcher p = node.getProfilePatternMatcher();

			return p != null && select(p.getPattern().getSignatureRelations());
		}

		abstract boolean select(Collection<Relation> rels);
	}

	static private class Structured extends StructuredNodeSelector {

		boolean select(Collection<Relation> rels) {

			return !rels.isEmpty();
		}
	}

	static private class StructureFor extends StructuredNodeSelector {

		private PropertyName property;

		StructureFor(PropertyName property) {

			this.property = property;
		}

		boolean select(Collection<Relation> rels) {

			for (Relation r : rels) {

				if (r.getProperty() == property) {

					return true;
				}
			}

			return false;
		}
	}

	static private abstract class ClassifyTargetPattern extends Structured {

		boolean select(NodeName node) {

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

			NodePropertyName p = rel.getNodeProperty();

			return p.definitionRefed() && p.anyChains();
		}
	}

	static private class ClassifyTargetPatternRoot extends ClassifyTargetPattern {

		MatchRole getMatchRole() {

			return MatchRole.PATTERN_ROOT;
		}
	}

	static private class ClassifyTargetPatternValue extends ClassifyTargetPattern {

		MatchRole getMatchRole() {

			return MatchRole.PATTERN_VALUE;
		}
	}

	static private class ClassifyTargetDisjunct extends SelectiveNodeSelector {

		boolean select(NodeName node) {

			return node.definitionRefed(MatchRole.DISJUNCT);
		}
	}

	abstract boolean select(NodeName node);

	abstract boolean anyMatches(Names nodes);
}
