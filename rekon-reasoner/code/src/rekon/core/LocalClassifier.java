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
class LocalClassifier extends NodeMatcherClassifier {

	private PotentialLocalPatternSubsumers defnPatternsFilter;
	private PotentialDisjunctionSubsumers defnDisjunctionsFilter;

	private DefaultClassifier defaultClassifier = new DefaultClassifier();

	private abstract class ClassifierOption {

		NameSet classify(LocalExpression expr) {

			classifyProfiles(expr);

			return expr.getExpressionNode().getClassifier().getSubsumers();
		}

		void checkSubsumption(NodeMatcher defn, NodeMatcher candidate) {

			checkMatcherSubsumption(defn, candidate);
		}

		void checkSubsumption(PatternMatcher defn, PatternMatcher candidate) {

			checkMatcherSubsumption(defn, candidate);
		}

		private void classifyProfiles(LocalExpression expr) {

			for (NodeMatcher candidate : expr.getOrderedProfileMatchers()) {

				exhaustivelyClassify(candidate);
			}
		}

		private void exhaustivelyClassify(NodeMatcher candidate) {

			do {

				candidate.checkExpandLocalProfile();

				classify(candidate);
			}
			while (absorbNewSubsumerExpansions(candidate));
		}

		abstract void classify(NodeMatcher candidate);

		private boolean absorbNewSubsumerExpansions(NodeMatcher candidate) {

			NodeClassifier c = candidate.getNode().getNodeClassifier();

			return c.absorbNewLocallyInferredSubsumerExpansions();
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

			for (PatternMatcher defn : defnPatternsFilter.getPotentialsFor(candidate)) {

				checkSubsumption(defn, candidate);
			}
		}

		private void checkSubsumptions(DisjunctionMatcher candidate) {

			for (PatternMatcher defn : defnPatternsFilter.getPotentialsFor(candidate)) {

				checkSubsumption(defn, candidate);
			}

			for (DisjunctionMatcher defn : defnDisjunctionsFilter.getPotentialsFor(candidate)) {

				checkSubsumption(defn, candidate);
			}
		}
	}

	private class PreFilteredDefnsClassifier extends ClassifierOption {

		private Collection<NodeMatcher> preFilteredDefns;

		private abstract class TypeClassifier
								<C extends NodeMatcher>
								extends NodeMatcherVisitor {

			final C candidate;

			TypeClassifier(C candidate) {

				this.candidate = candidate;
			}
		}

		private class PatternClassifier extends TypeClassifier<PatternMatcher> {

			PatternClassifier(PatternMatcher candidate) {

				super(candidate);
			}

			void visit(PatternMatcher defn) {

				checkSubsumption(defn, candidate);
			}

			void visit(DisjunctionMatcher defn) {

				checkSubsumption(defn, candidate);
			}
		}

		private class DisjunctionClassifier extends TypeClassifier<DisjunctionMatcher> {

			DisjunctionClassifier(DisjunctionMatcher candidate) {

				super(candidate);

				candidate.inferNewCommonDisjunctSubsumers();
			}

			void visit(PatternMatcher defn) {
			}

			void visit(DisjunctionMatcher defn) {

				checkSubsumption(defn, candidate);
			}
		}

		private class TypeClassifierCreator extends NodeMatcherVisitor {

			private TypeClassifier<?> created;

			TypeClassifier<?> create(NodeMatcher candidate) {

				candidate.acceptVisitor(this);

				return created;
			}

			void visit(PatternMatcher defn) {

				created = new PatternClassifier(defn);
			}

			void visit(DisjunctionMatcher defn) {

				created = new DisjunctionClassifier(defn);
			}
		}

		PreFilteredDefnsClassifier(Collection<NodeMatcher> preFilteredDefns) {

			this.preFilteredDefns = preFilteredDefns;
		}

		void classify(NodeMatcher candidate) {

			TypeClassifier<?> tc = new TypeClassifierCreator().create(candidate);

			for (NodeMatcher defn : preFilteredDefns) {

				defn.acceptVisitor(tc);
			}
		}
	}

	LocalClassifier(Ontology ontology) {

		defnPatternsFilter = createDefnPatternsFilter(ontology);
		defnDisjunctionsFilter = createDefnDisjunctionsFilter(ontology);
	}

	NameSet classify(LocalExpression expr) {

		return defaultClassifier.classify(expr);
	}

	NameSet classify(LocalExpression expr, Collection<NodeMatcher> preFilteredDefns) {

		return new PreFilteredDefnsClassifier(preFilteredDefns).classify(expr);
	}

	private PotentialLocalPatternSubsumers createDefnPatternsFilter(Ontology ontology) {

		return new PotentialLocalPatternSubsumers(ontology.getDefinitionPatterns());
	}

	private PotentialDisjunctionSubsumers createDefnDisjunctionsFilter(Ontology ontology) {

		return new PotentialDisjunctionSubsumers(ontology.getDefinitionDisjunctions());
	}
}
