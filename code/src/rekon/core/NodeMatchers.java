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

	private DefinitionPropagator definitionPropagator = new DefinitionPropagator();

	private class DefinitionPropagator extends PatternCrawler {

		boolean visit(NodeX n) {

			return n instanceof FreeClassNode;
		}

		boolean visit(PatternMatcher m) {

			if (m.profileMatcher()) {

				ensureDefinitionForNode(m);
			}

			return true;
		}

		boolean visit(DisjunctionMatcher m) {

			m.ensureDefinition();

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

		addDefinitionPattern(createTopLevelDefinitionPattern(node, defn));

		definitionPropagator.crawlFrom(defn);
	}

	void addDisjunction(DisjunctionMatcher disjunction) {

		disjunctions.add(disjunction);

		if (disjunction.definition()) {

			definitionPropagator.crawlFrom(disjunction);
		}
	}

	void addDisjunction(
			ClassNode node,
			Collection<? extends NodeX> disjuncts,
			boolean definition) {

		DisjunctionMatcher m = node.addDisjunctionMatcher(disjuncts);

		if (definition) {

			m.ensureDefinition();
		}

		addDisjunction(m);
	}

	List<PatternMatcher> getProfilePatterns() {

		return profilePatterns;
	}

	List<PatternMatcher> getDefinitionPatterns() {

		return definitionPatterns;
	}

	List<DisjunctionMatcher> getAllDisjunctions() {

		return disjunctions;
	}

	List<DisjunctionMatcher> getDefinitionDisjunctions() {

		List<DisjunctionMatcher> defns = new ArrayList<DisjunctionMatcher>();

		for (DisjunctionMatcher d : disjunctions) {

			if (d.definition()) {

				defns.add(d);
			}
		}

		return defns;
	}

	private PatternMatcher createTopLevelDefinitionPattern(NodeX node, Pattern defn) {

		addNameSubsumers(node, defn.getNodes());
		resolveProfilePattern(node).absorbDefinitionIntoProfile(defn);

		return node.addDefinitionPatternMatcher(defn);
	}

	private void ensureDefinitionForNode(PatternMatcher defn) {

		if (defn.getNode().ensureDefined()) {

			addDefinitionPattern(defn);
		}
	}

	private void addDefinitionPattern(PatternMatcher defn) {

		definitionPatterns.add(defn);

		defn.getPattern().registerDefinitionRefedNames();
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
