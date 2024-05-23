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

	private List<DisjunctionMatcher> allDisjunctions = new ArrayList<DisjunctionMatcher>();
	private List<DisjunctionMatcher> definitionDisjunctions = new ArrayList<DisjunctionMatcher>();

	private List<PatternMatcher> potentialExpansionProfilePatterns = new ArrayList<PatternMatcher>();

	NodeMatchers(Iterable<NodeX> nodes) {

		for (NodeX n : nodes) {

			profilePatterns.addAll(n.getProfilePatternMatcherAsList());
			definitionPatterns.addAll(n.getDefinitionPatternMatchers());
			allDisjunctions.addAll(n.getAllDisjunctionMatchers());
		}

		addDefinitionDisjunctions();
	}

	void expandProfilePatterns() {

		initPotentialExpansionProfilePatterns();
		initCurrentProfilePatternExpansions();

		processProfilePatternExpansions(profilePatterns);
		processProfilePatternExpansions(potentialExpansionProfilePatterns);
	}

	void clearProfilePatternExpansions() {

		clearCurrentProfilePatternExpansions();
		clearPotentialExpansionProfilePatterns();
	}

	List<PatternMatcher> getProfilePatterns() {

		return profilePatterns;
	}

	List<PatternMatcher> getDefinitionPatterns() {

		return definitionPatterns;
	}

	List<DisjunctionMatcher> getAllDisjunctions() {

		return allDisjunctions;
	}

	List<DisjunctionMatcher> getDefinitionDisjunctions() {

		return definitionDisjunctions;
	}

	private void initCurrentProfilePatternExpansions() {

		for (PatternMatcher p : profilePatterns) {

			getProfileRelations(p).initExpansion();
		}
	}

	private void initPotentialExpansionProfilePatterns() {

		for (DisjunctionMatcher d : allDisjunctions) {

			NodeX n = d.getNode();

			if (n.getProfilePatternMatcher() == null) {

				PatternMatcher p = n.addProfilePatternMatcher();

				potentialExpansionProfilePatterns.add(p);
				getProfileRelations(p).initExpansion();
			}
		}
	}

	private void processProfilePatternExpansions(List<PatternMatcher> patterns) {

		for (PatternMatcher p : patterns) {

			getProfileRelations(p).processExpansion();
		}
	}

	private void clearCurrentProfilePatternExpansions() {

		for (PatternMatcher p : profilePatterns) {

			getProfileRelations(p).clearExpansion();
		}
	}

	private void clearPotentialExpansionProfilePatterns() {

		for (PatternMatcher p : potentialExpansionProfilePatterns) {

			ProfileRelations prs = getProfileRelations(p);

			if (prs.anyRelations()) {

				prs.clearExpansion();
				profilePatterns.add(p);
			}
			else {

				p.getNode().removeProfilePatternMatcher();
			}
		}

		potentialExpansionProfilePatterns.clear();
	}

	private void addDefinitionDisjunctions() {

		for (DisjunctionMatcher d : allDisjunctions) {

			if (d.definition()) {

				definitionDisjunctions.add(d);
			}
		}
	}

	private ProfileRelations getProfileRelations(PatternMatcher p) {

		return p.getPattern().getProfileRelations();
	}
}
