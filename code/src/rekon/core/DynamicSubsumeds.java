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
class DynamicSubsumeds {

	private PotentialPatternSubsumeds patternPotentials;
	private PotentialDisjunctionSubsumeds disjunctionPotentials;

	private ClassSubsumptions classSubsumptions;
	private NodeSubsumptions allNodeSubsumptions;

	private class PotentialsSelector extends NodeMatcherVisitor {

		private Collection<? extends NodeMatcher> potentials = null;

		PotentialsSelector(NodeMatcher defn) {

			defn.acceptVisitor(this);
		}

		void visit(PatternMatcher defn) {

			potentials = patternPotentials.getPotentialsFor(defn.getPattern());
		}

		void visit(DisjunctionMatcher defn) {

			potentials = disjunctionPotentials.getPotentialsFor(defn);
		}

		Collection<? extends NodeMatcher> getSelecteds() {

			return potentials;
		}
	}

	private class NodeSubsumptions {

		NameSet find(LocalExpression expr) {

			NameSet matches = new NameSet();
			NodeMatcher defn = expr.getExpressionMatcher();

			for (Name n : defn.getDirectlyImpliedSubNodes()) {

				if (requiredCandidate(n)) {

					matches.add(n);
				}
			}

			for (NodeMatcher cand : new PotentialsSelector(defn).getSelecteds()) {

				Name n = cand.getNode();

				if (requiredCandidate(n) && defn.subsumes(cand)) {

					matches.add(n);
				}
			}

			return matches;
		}

		boolean requiredCandidate(Name n) {

			return true;
		}
	}

	private class ClassSubsumptions extends NodeSubsumptions {

		private NameSet filterNames = null;

		NameSet find(LocalExpression expr, NameSet filterNames) {

			this.filterNames = filterNames;

			return find(expr);
		}

		boolean requiredCandidate(Name n) {

			if (n instanceof ClassNode) {

				return filterNames == null || filterNames.contains(n);
			}

			return false;
		}
	}

	DynamicSubsumeds(NodeMatchers nodeMatchers) {

		patternPotentials = createPatternPotentials(nodeMatchers);
		disjunctionPotentials = createDisjunctionPotentials(nodeMatchers);

		classSubsumptions = new ClassSubsumptions();
		allNodeSubsumptions = new NodeSubsumptions();
	}

	void checkAddInstanceOption(InstanceNode node) {

		patternPotentials.checkAddInstanceOption(node);
	}

	void checkRemoveInstanceOption(InstanceNode node) {

		patternPotentials.checkRemoveInstanceOption(node);
	}

	NameSet inferSubsumedClasses(LocalExpression expr) {

		return classSubsumptions.find(expr, null);
	}

	NameSet inferSubsumedClasses(LocalExpression expr, NameSet filterNames) {

		return classSubsumptions.find(expr, filterNames);
	}

	NameSet inferAllSubsumedNodes(LocalExpression expr) {

		return allNodeSubsumptions.find(expr);
	}

	private PotentialPatternSubsumeds createPatternPotentials(NodeMatchers nodeMatchers) {

		return new PotentialLocalPatternSubsumeds(
						filterMatchersForMappedNodes(
							nodeMatchers.getProfilePatterns()));
	}

	private PotentialDisjunctionSubsumeds createDisjunctionPotentials(NodeMatchers nodeMatchers) {

		return new PotentialDisjunctionSubsumeds(
						filterMatchersForMappedNodes(
							nodeMatchers.getDefinitionDisjunctions()));
	}

	private <M extends NodeMatcher>List<M> filterMatchersForMappedNodes(List<M> all) {

		List<M> candidates = new ArrayList<M>();

		for (M m : all) {

			if (m.getNode().mapped()) {

				candidates.add(m);
			}
		}

		return candidates;
	}
}
