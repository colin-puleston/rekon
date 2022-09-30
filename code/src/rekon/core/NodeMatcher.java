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
abstract class NodeMatcher {

	static final NodeMatcher ANY = new Any();
	static final NodeMatcher STRUCTURED = new Structured();
	static final NodeMatcher CLASSIFY_TARGET_ROOT = new ClassifyTarget(PatternNameRole.ROOT);
	static final NodeMatcher CLASSIFY_TARGET_VALUE = new ClassifyTarget(PatternNameRole.VALUE);

	static NodeMatcher structureFor(PropertyName property) {

		return new StructureFor(property);
	}

	static private class Any extends NodeMatcher {

		boolean match(NodeName node) {

			return true;
		}

		boolean anyMatches(Names nodes) {

			return !nodes.isEmpty();
		}
	}

	static abstract private class SelectiveNodeMatcher extends NodeMatcher {

		boolean anyMatches(Names nodes) {

			for (Name n : nodes.getNames()) {

				if (match((NodeName)n)) {

					return true;
				}
			}

			return false;
		}
	}

	static abstract private class StructuredNodeMatcher extends SelectiveNodeMatcher {

		boolean match(NodeName node) {

			MatchableNode m = node.getMatchable();

			return m != null && match(m.getProfile().getSignatureRelations());
		}

		abstract boolean match(Collection<Relation> rels);
	}

	static private class Structured extends StructuredNodeMatcher {

		boolean match(Collection<Relation> rels) {

			return !rels.isEmpty();
		}
	}

	static private class StructureFor extends StructuredNodeMatcher {

		private PropertyName property;

		StructureFor(PropertyName property) {

			this.property = property;
		}

		boolean match(Collection<Relation> rels) {

			for (Relation r : rels) {

				if (r.getProperty() == property) {

					return true;
				}
			}

			return false;
		}
	}

	static private class ClassifyTarget extends Structured {

		private PatternNameRole role;

		ClassifyTarget(PatternNameRole role) {

			this.role = role;
		}

		boolean match(NodeName node) {

			return node.definitionRefed(role) || super.match(node);
		}

		boolean match(Collection<Relation> rels) {

			for (Relation r : rels) {

				if (r instanceof ObjectRelation && targetFor((ObjectRelation)r)) {

					return true;
				}
			}

			return false;
		}

		private boolean targetFor(ObjectRelation rel) {

			return rel.getObjectProperty().definitionRefed();
		}
	}

	abstract boolean match(NodeName node);

	abstract boolean anyMatches(Names nodes);
}
