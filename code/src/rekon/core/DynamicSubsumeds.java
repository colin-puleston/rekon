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

	private PotentialLocalPatternSubsumeds patternPotentials;
	private PotentialDisjunctionSubsumeds disjunctionPotentials;

	private abstract class NodeSubsumptions {

		final NameSet subsumeds = new NameSet();

		private NodeMatcher defn;

		private class MatchFinder extends NodeMatcherVisitor {

			MatchFinder(NodeMatcher defn) {

				defn.acceptVisitor(this);
			}

			void visit(PatternMatcher defn) {

				findViaMatchers(patternPotentials.getPotentialsFor(defn));
				findViaMatchers(disjunctionPotentials.getPotentialsFor(defn));
			}

			void visit(DisjunctionMatcher defn) {

				findViaMatchers(patternPotentials.getPotentialsFor(defn));
				findViaMatchers(disjunctionPotentials.getPotentialsFor(defn));
			}
		}

		NodeSubsumptions(LocalExpression expr) {

			defn = expr.getExpressionMatcher();
		}

		void findAll() {

			findDirectlyImplieds();

			new MatchFinder(defn);
		}

		abstract boolean requiredCandidate(Name n);

		private void findDirectlyImplieds() {

			for (Name n : defn.getDirectlyImpliedSubNodes()) {

				if (requiredCandidate(n)) {

					subsumeds.add(n);
				}
			}
		}

		private void findViaMatchers(Collection<? extends NodeMatcher> potentials) {

			for (NodeMatcher cand : potentials) {

				Name n = cand.getNode();

				if (requiredCandidate(n) && defn.subsumes(cand)) {

					subsumeds.add(n);
				}
			}
		}
	}

	private class AllNodeSubsumptions extends NodeSubsumptions {

		AllNodeSubsumptions(LocalExpression expr) {

			super(expr);

			findAll();
		}

		boolean requiredCandidate(Name n) {

			return true;
		}
	}

	private class ClassSubsumptions extends NodeSubsumptions {

		private NameSet filterNames;

		ClassSubsumptions(LocalExpression expr) {

			this(expr, null);
		}

		ClassSubsumptions(LocalExpression expr, NameSet filterNames) {

			super(expr);

			this.filterNames = filterNames;

			findAll();
		}

		boolean requiredCandidate(Name n) {

			if (n instanceof ClassNode) {

				return filterNames == null || filterNames.contains(n);
			}

			return false;
		}
	}

	DynamicSubsumeds(Ontology ontology) {

		patternPotentials = createPatternPotentials(ontology);
		disjunctionPotentials = createDisjunctionPotentials(ontology);
	}

	void checkAddInstanceOption(InstanceNode node) {

		patternPotentials.checkAddInstanceOption(node);
	}

	void checkRemoveInstanceOption(InstanceNode node) {

		patternPotentials.checkRemoveInstanceOption(node);
	}

	NameSet inferSubsumedClasses(LocalExpression expr) {

		return new ClassSubsumptions(expr).subsumeds;
	}

	NameSet inferSubsumedClasses(LocalExpression expr, NameSet filterNames) {

		return new ClassSubsumptions(expr, filterNames).subsumeds;
	}

	NameSet inferAllSubsumedNodes(LocalExpression expr) {

		return new AllNodeSubsumptions(expr).subsumeds;
	}

	private PotentialLocalPatternSubsumeds createPatternPotentials(Ontology ontology) {

		return new PotentialLocalPatternSubsumeds(
						filterMatchersForMappedNodes(
							ontology.getProfilePatterns()));
	}

	private PotentialDisjunctionSubsumeds createDisjunctionPotentials(Ontology ontology) {

		return new PotentialDisjunctionSubsumeds(
						filterMatchersForMappedNodes(
							ontology.getAllDisjunctions()));
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
