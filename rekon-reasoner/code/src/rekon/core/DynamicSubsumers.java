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

/**
 * @author Colin Puleston
 */
class DynamicSubsumers extends NodeMatcherClassifier {

	private PotentialLocalPatternSubsumers defnPatternsFilter;
	private PotentialDisjunctionSubsumers defnDisjunctionsFilter;

	private TypeClassifier typeClassifier = new TypeClassifier();

	private class TypeClassifier extends NodeMatcherVisitor {

		void visit(PatternMatcher candidate) {

			checkSubsumptions(candidate);
		}

		void visit(DisjunctionMatcher candidate) {

			checkSubsumptions(candidate);
		}
	}

	DynamicSubsumers(Ontology ontology) {

		defnPatternsFilter = createDefnPatternsFilter(ontology);
		defnDisjunctionsFilter = createDefnDisjunctionsFilter(ontology);
	}

	NameSet inferSubsumers(LocalExpression expr) {

		for (NodeMatcher candidate : expr.getOrderedProfileMatchers()) {

			exhaustivelyClassify(candidate);
		}

		return expr.getExpressionNode().getClassifier().getSubsumers();
	}

	private void exhaustivelyClassify(NodeMatcher candidate) {

		do {

			candidate.checkExpandLocalProfile();
			candidate.acceptVisitor(typeClassifier);
		}
		while (absorbNewSubsumerExpansions(candidate));
	}

	private boolean absorbNewSubsumerExpansions(NodeMatcher candidate) {

		NodeClassifier c = candidate.getNode().getNodeClassifier();

		return c.absorbNewLocallyInferredSubsumerExpansions();
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

	private PotentialLocalPatternSubsumers createDefnPatternsFilter(Ontology ontology) {

		return new PotentialLocalPatternSubsumers(ontology.getDefinitionPatterns());
	}

	private PotentialDisjunctionSubsumers createDefnDisjunctionsFilter(Ontology ontology) {

		return new PotentialDisjunctionSubsumers(ontology.getDefinitionDisjunctions());
	}
}

