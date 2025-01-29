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
class DynamicExpression extends LocalExpression {

	private MultiPatternSource disjunctsSource;

	private NodeMatcher expressionMatcher = null;
	private NodeX asSingleNode = null;

	private class DynamicClasses extends FreeClasses {

		private class DynamicPatternClassNode extends PatternClassNode {
		}

		private class DynamicDefinitionClassNode extends DefinitionClassNode {
		}

		boolean localClasses() {

			return true;
		}

		PatternClassNode createPatternClass() {

			return new DynamicPatternClassNode();
		}

		DefinitionClassNode createDefinitionClass() {

			return new DynamicDefinitionClassNode();
		}
	}

	private class ExpressionCreator {

		private MatchStructures matchStructures;

		ExpressionCreator(MatchStructures matchStructures) {

			this.matchStructures = matchStructures;
		}

		NodeX create() {

			Collection<Pattern> djs = disjunctsSource.createAll(matchStructures);

			if (djs == null) {

				return null;
			}

			if (djs.size() == 1) {

				return createSinglePattern(djs.iterator().next());
			}

			return createDisjunction(djs);
		}

		private NodeX createSinglePattern(Pattern pattern) {

			asSingleNode = pattern.toSingleNode();

			if (asSingleNode != null) {

				expressionMatcher = new PatternMatcher(asSingleNode);

				return asSingleNode;
			}

			ClassNode defnCls = matchStructures.createDefinitionClass();

			expressionMatcher = matchStructures.addDefinitionPattern(defnCls, pattern);

			return defnCls;
		}

		private NodeX createDisjunction(Collection<Pattern> djs) {

			List<NodeX> djNodes = new ArrayList<NodeX>();

			for (Pattern dj : djs) {

				NodeX djNode = dj.toSingleNode();

				if (djNode == null) {

					djNode = matchStructures.createDefinitionClass();

					matchStructures.addDefinitionPattern(djNode, dj);
				}

				djNodes.add(djNode);
			}

			ClassNode defnCls = matchStructures.createDefinitionClass();

			expressionMatcher = matchStructures.addDisjunction(defnCls, djNodes, true);

			return defnCls;
		}
	}

	DynamicExpression(MultiPatternSource disjunctsSource) {

		this.disjunctsSource = disjunctsSource;

		initialise(new DynamicClasses());
	}

	DynamicExpression(SinglePatternSource patternSource) {

		this(new SingleAsMultiPatternSource(patternSource));
	}

	NodeX createExpression(MatchStructures matchStructures) {

		return new ExpressionCreator(matchStructures).create();
	}

	NodeMatcher getExpressionMatcher() {

		return expressionMatcher;
	}

	NodeX toSingleNode() {

		return asSingleNode;
	}
}
