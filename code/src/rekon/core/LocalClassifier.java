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

	private PotentialLocalPatternSubsumers definedPatternsFilter;
	private PotentialDisjunctionSubsumers disjunctionDefnsFilter;

	private DefaultClassifier defaultClassifier = new DefaultClassifier();
	private SubsumptionChecker subsumptionChecker = new SubsumptionChecker();

	private abstract class ClassifierOption {

		private boolean newSubsumptions = false;

		NameSet classify(LocalPattern pattern) {

			classifyMatchables(pattern.getPatternMatchables());

			return pattern.getPatternNode().getClassifier().getSubsumers();
		}

		void updateNewSubsumptions(boolean newSubsumption) {

			if (newSubsumption && !newSubsumptions) {

				newSubsumptions = true;
			}
		}

		private void classifyMatchables(OrderedMatchableNodes matchables) {

			for (MatchableNode<?> candidate : matchables.getOrderedNodes()) {

				exhaustivelyClassify(candidate);

				if (!newSubsumptions) {

					break;
				}

				newSubsumptions = false;
			}
		}

		private void exhaustivelyClassify(MatchableNode<?> candidate) {

			do {

				classify(candidate);
			}
			while (checkReclassifiable(candidate));
		}

		abstract void classify(MatchableNode<?> candidate);

		private boolean checkReclassifiable(MatchableNode<?> candidate) {

			NodeName n = candidate.getName();

			if (n.getNodeClassifier().absorbNewInferredSubsumers()) {

				Pattern p = n.getProfilePattern();

				if (p != null && p.potentialSignatureUpdates()) {

					p.resetSignatureRefs();
				}

				return true;
			}

			return false;
		}
	}

	private class DefaultClassifier extends ClassifierOption {

		private TypeClassifier typeClassifier = new TypeClassifier();

		private class TypeClassifier extends MatchableNodeVisitor {

			void visit(PatternNode candidate) {

				classify(candidate);
			}

			void visit(DisjunctionNode candidate) {

				classify(candidate);
			}
		}

		void classify(MatchableNode<?> candidate) {

			candidate.acceptVisitor(typeClassifier);
		}

		private void classify(PatternNode candidate) {

			Pattern profile = candidate.getProfile();

			for (DefinitionPattern defn : definedPatternsFilter.getPotentialsFor(profile)) {

				updateNewSubsumptions(subsumptionChecker.check(defn, candidate));
			}
		}

		private void classify(DisjunctionNode candidate) {

			for (DisjunctionNode defn : disjunctionDefnsFilter.getPotentialsFor(candidate)) {

				updateNewSubsumptions(subsumptionChecker.check(defn, candidate));
			}
		}
	}

	private class PreFilteredDefinedsClassifier extends ClassifierOption {

		private Collection<MatchableNode<?>> preFilteredDefineds;

		private class TypeClassifier extends MatchableNodeVisitor {

			private MatchableNode<?> candidate;

			TypeClassifier(MatchableNode<?> defined, MatchableNode<?> candidate) {

				this.candidate = candidate;

				defined.acceptVisitor(this);
			}

			void visit(PatternNode defined) {

				classify(defined, (PatternNode)candidate);
			}

			void visit(DisjunctionNode defined) {

				classify(defined, (DisjunctionNode)candidate);
			}
		}

		PreFilteredDefinedsClassifier(Collection<MatchableNode<?>> preFilteredDefineds) {

			this.preFilteredDefineds = preFilteredDefineds;
		}

		void classify(MatchableNode<?> candidate) {

			for (MatchableNode<?> defined : preFilteredDefineds) {

				if (defined.getClass() == candidate.getClass()) {

					new TypeClassifier(defined, candidate);
				}
			}
		}

		private void classify(PatternNode defined, PatternNode candidate) {

			for (Pattern defn : defined.getDefinitions()) {

				updateNewSubsumptions(subsumptionChecker.check(defined, defn, candidate));
			}
		}

		private void classify(DisjunctionNode defined, DisjunctionNode candidate) {

			updateNewSubsumptions(subsumptionChecker.check(defined, candidate));
		}
	}

	LocalClassifier(MatchableNodes matchables) {

		definedPatternsFilter = createDefinedPatternsFilter(matchables);
		disjunctionDefnsFilter = createDisjunctionDefnsFilter(matchables);
	}

	NameSet classify(LocalPattern pattern) {

		return defaultClassifier.classify(pattern);
	}

	NameSet classify(LocalPattern pattern, Collection<MatchableNode<?>> preFilteredDefineds) {

		return new PreFilteredDefinedsClassifier(preFilteredDefineds).classify(pattern);
	}

	private PotentialLocalPatternSubsumers createDefinedPatternsFilter(MatchableNodes matchables) {

		return new PotentialLocalPatternSubsumers(matchables.getAllPatternNodes());
	}

	private PotentialDisjunctionSubsumers createDisjunctionDefnsFilter(MatchableNodes matchables) {

		return new PotentialDisjunctionSubsumers(matchables.getDisjunctionNodes());
	}
}
