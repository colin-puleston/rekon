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
class DynamicNodeOpsHandler extends DynamicOpsHandler {

	private NodeX node;

	 private abstract class PatternCollector {

		Collection<Pattern> getAll() {

			if (node == null) {

				return Collections.emptyList();
			}

			List<Pattern> patterns = new ArrayList<Pattern>();

			checkAddPattern(patterns, node);

			for (Name en : node.getEquivalents()) {

				checkAddPattern(patterns, (NodeX)en);
			}

			return patterns;
		}

		abstract void checkAddPattern(Collection<Pattern> patterns, NodeX n);
	}

	 private class ProfileCollector extends PatternCollector {

		void checkAddPattern(Collection<Pattern> patterns, NodeX n) {

			PatternMatcher p = n.getProfilePatternMatcher();

			if (p != null) {

				patterns.add(p.getPattern());
			}
		}
	}

	 private class DefinitionCollector extends PatternCollector {

		void checkAddPattern(Collection<Pattern> patterns, NodeX n) {

			for (PatternMatcher p : n.getDefinitionPatternMatchers()) {

				patterns.add(p.getPattern());
			}
		}
	}

	public Names getEquivalents() {

		if (node == null) {

			return Names.NO_NAMES;
		}

		NameList equivs = new NameList(node);

		equivs.addAll(node.getEquivalents());

		return equivs;
	}

	public Names getSupers(boolean direct) {

		return node != null ? node.getSupers(direct) : Names.NO_NAMES;
	}

	public Names getSubs(boolean direct) {

		return getSubs(ClassNode.class, direct);
	}

	public Names getIndividuals(boolean direct) {

		return getSubs(IndividualNode.class, direct);
	}

	public boolean subsumes(DynamicOpsHandler other) {

		if (other instanceof DynamicNodeOpsHandler) {

			return node.subsumes(((DynamicNodeOpsHandler)other).node);
		}

		return super.subsumes(other);
	}

	DynamicNodeOpsHandler(NodeX node) {

		this.node = node;
	}

	Collection<Pattern> getProfiles() {

		return new ProfileCollector().getAll();
	}

	Collection<Pattern> getDefinitions() {

		return new DefinitionCollector().getAll();
	}

	private Names getSubs(Class<? extends NodeX> type, boolean direct) {

		return node != null ? node.getSubs(type, direct) : Names.NO_NAMES;
	}
}
