/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Colin Puleston
 *
 * Permission is hereby granted, free of charge, to new person obtaining a copy
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
class LocalClassifier {

	private PotentialLocalPatternSubsumers defnPatternsFilter;
	private PotentialDisjunctionSubsumers defnDisjunctionsFilter;

	private DefaultClassifier defaultClassifier = new DefaultClassifier();
	private SubsumptionChecker subsumptionChecker = new SubsumptionChecker();

	private abstract class ClassifierOption {

		private boolean newSubsumptions = false;

		NameSet classify(LocalPattern pattern) {

			classifyProfiles(pattern);

			return pattern.getDefinitionNode().getClassifier().getSubsumers();
		}

		void checkSubsumption(PatternMatcher defn, PatternMatcher candidate) {

			updateNewSubsumptions(subsumptionChecker.check(defn, candidate));
		}

		void checkSubsumption(DisjunctionMatcher defn, DisjunctionMatcher candidate) {

			updateNewSubsumptions(subsumptionChecker.check(defn, candidate));
		}

		private void classifyProfiles(LocalPattern pattern) {

			for (NodeMatcher candidate : pattern.getOrderedProfileMatchers()) {

				exhaustivelyClassify(candidate);

				if (!newSubsumptions) {

					break;
				}

				newSubsumptions = false;
			}
		}

		private void exhaustivelyClassify(NodeMatcher candidate) {

			do {

				checkExpandProfile(candidate);
				classify(candidate);
			}
			while (absorbNewSubsumerExpansions(candidate));
		}

		abstract void classify(NodeMatcher candidate);

		private void checkExpandProfile(NodeMatcher candidate) {

			if (candidate instanceof PatternMatcher) {

				Pattern p = ((PatternMatcher)candidate).getPattern();

				p.setProfileExpansionCheckRequired();
				p.updateForProfileExpansion();
			}
		}

		private boolean absorbNewSubsumerExpansions(NodeMatcher candidate) {

			NodeClassifier c = candidate.getNode().getNodeClassifier();

			return c.absorbNewLocallyInferredSubsumerExpansions();
		}

		private void updateNewSubsumptions(boolean newSubsumption) {

			if (newSubsumption && !newSubsumptions) {

				newSubsumptions = true;
			}
		}
	}

	private class DefaultClassifier extends ClassifierOption {

		private TypeClassifier typeClassifier = new TypeClassifier();

		private class TypeClassifier extends NodeMatcherVisitor {

			void visit(PatternMatcher candidate) {

				checkSubsumptions(candidate);
			}

			void visit(DisjunctionMatcher candidate) {

				checkSubsumptions(candidate);
			}
		}

		void classify(NodeMatcher candidate) {

			candidate.acceptVisitor(typeClassifier);
		}

		private void checkSubsumptions(PatternMatcher candidate) {

			Pattern profile = candidate.getPattern();

			for (PatternMatcher defn : defnPatternsFilter.getPotentialsFor(profile)) {

				checkSubsumption(defn, candidate);
			}
		}

		private void checkSubsumptions(DisjunctionMatcher candidate) {

			for (DisjunctionMatcher defn : defnDisjunctionsFilter.getPotentialsFor(candidate)) {

				checkSubsumption(defn, candidate);
			}
		}
	}

	private class PreFilteredDefnsClassifier extends ClassifierOption {

		private Collection<NodeMatcher> preFilteredDefns;

		private class TypeClassifier extends NodeMatcherVisitor {

			private NodeMatcher candidate;

			TypeClassifier(NodeMatcher candidate) {

				this.candidate = candidate;
			}

			void visit(PatternMatcher defn) {

				if (candidate instanceof PatternMatcher) {

					checkSubsumption(defn, (PatternMatcher)candidate);
				}
			}

			void visit(DisjunctionMatcher defn) {

				if (candidate instanceof DisjunctionMatcher) {

					checkSubsumption(defn, (DisjunctionMatcher)candidate);
				}
			}
		}

		PreFilteredDefnsClassifier(Collection<NodeMatcher> preFilteredDefns) {

			this.preFilteredDefns = preFilteredDefns;
		}

		void classify(NodeMatcher candidate) {

			TypeClassifier tc = new TypeClassifier(candidate);

			for (NodeMatcher defn : preFilteredDefns) {

				defn.acceptVisitor(tc);
			}
		}
	}

	LocalClassifier(NodeMatchers nodeMatchers) {

		defnPatternsFilter = createDefnPatternsFilter(nodeMatchers);
		defnDisjunctionsFilter = createDefnDisjunctionsFilter(nodeMatchers);
	}

	NameSet classify(LocalPattern pattern) {

		return defaultClassifier.classify(pattern);
	}

	NameSet classify(LocalPattern pattern, Collection<NodeMatcher> preFilteredDefns) {

		return new PreFilteredDefnsClassifier(preFilteredDefns).classify(pattern);
	}

	private PotentialLocalPatternSubsumers createDefnPatternsFilter(NodeMatchers nodeMatchers) {

		return new PotentialLocalPatternSubsumers(nodeMatchers.getDefinitionPatterns());
	}

	private PotentialDisjunctionSubsumers createDefnDisjunctionsFilter(NodeMatchers nodeMatchers) {

		return new PotentialDisjunctionSubsumers(nodeMatchers.getDefinitionDisjunctions());
	}
}
