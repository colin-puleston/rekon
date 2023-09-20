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
class NodeMatchers {

	private List<PatternMatcher> profilePatterns = new ArrayList<PatternMatcher>();
	private List<PatternMatcher> definitionPatterns = new ArrayList<PatternMatcher>();

	private List<DisjunctionMatcher> disjunctions = new ArrayList<DisjunctionMatcher>();

	private class DefinitionPropagator extends PatternCrawler {

		DefinitionPropagator(Pattern defnPattern) {

			crawlFrom(defnPattern);
		}

		boolean visit(NodeX n) {

			return true;
		}

		boolean visit(PatternMatcher m) {

			return true;
		}

		boolean visit(DisjunctionMatcher m) {

			m.setAsDefinition();

			return true;
		}
	}

	void addProfilePattern(PatternMatcher profile) {

		profilePatterns.add(profile);
	}

	void addProfilePattern(NodeX node, Pattern profile) {

		addProfilePattern(node.addProfilePatternMatcher(profile));
	}

	void addDefinitionPattern(NodeX node, Pattern defn) {

		addNameSubsumers(node, defn.getNodes());

		resolveProfilePattern(node).absorbDefinitionIntoProfile(defn);
		definitionPatterns.add(node.addDefinitionPatternMatcher(defn));

		defn.registerDefinitionRefedNames();
		new DefinitionPropagator(defn);
	}

	void addDisjunction(DisjunctionMatcher disjunction) {

		disjunctions.add(disjunction);
	}

	void addDisjunction(
			ClassNode node,
			Collection<? extends NodeX> disjuncts,
			boolean definition) {

		DisjunctionMatcher m = node.addDisjunctionMatcher(disjuncts);

		if (definition) {

			m.setAsDefinition();
		}

		addDisjunction(m);
	}

	List<PatternMatcher> getProfilePatterns() {

		return profilePatterns;
	}

	List<PatternMatcher> getDefinitionPatternMatchers() {

		return definitionPatterns;
	}

	List<DisjunctionMatcher> getAllDisjunctionMatchers() {

		return disjunctions;
	}

	List<DisjunctionMatcher> getDefinitionDisjunctionMatchers() {

		List<DisjunctionMatcher> defns = new ArrayList<DisjunctionMatcher>();

		for (DisjunctionMatcher d : disjunctions) {

			if (d.definition()) {

				defns.add(d);
			}
		}

		return defns;
	}

	private void addNameSubsumers(NodeX node, Names subsumers) {

		NameClassifier classifier = node.getClassifier();

		for (Name s : subsumers) {

			if (!s.rootName()) {

				classifier.addAssertedSubsumer(s);
			}
		}
	}

	private PatternMatcher resolveProfilePattern(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p == null) {

			p = node.addProfilePatternMatcher();

			addProfilePattern(p);
		}

		return p;
	}
}
