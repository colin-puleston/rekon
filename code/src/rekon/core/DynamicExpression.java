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

	private MultiPatternCreator disjunctsCreator;

	private NodeMatcher expressionMatcher = null;
	private NodeX asSingleNode = null;

	private class DynamicClasses extends LocalClasses {

		private class DynamicPatternClassNode extends LocalPatternClassNode {
		}

		private class DynamicDefinitionClassNode extends LocalDefinitionClassNode {
		}

		LocalPatternClassNode createLocalPatternClass() {

			return new DynamicPatternClassNode();
		}

		LocalDefinitionClassNode createLocalDefinitionClass() {

			return new DynamicDefinitionClassNode();
		}
	}

	DynamicExpression(MultiPatternCreator disjunctsCreator) {

		this.disjunctsCreator = disjunctsCreator;

		initialise(new DynamicClasses());
	}

	DynamicExpression(SinglePatternCreator patternCreator) {

		this(new SingleToMultiPatternCreator(patternCreator));
	}

	NodeX createStructures(MatchStructures matchStructures) {

		Collection<Pattern> djs = disjunctsCreator.createAll(matchStructures);

		if (djs == null) {

			return null;
		}

		NodeX exprNode = null;
		List<NodeX> djNodes = new ArrayList<NodeX>();

		for (Pattern dj : djs) {

			exprNode = dj.toSingleNode();

			if (exprNode == null) {

				exprNode = matchStructures.addDefinitionClass();
				expressionMatcher = matchStructures.addDefinitionPattern(exprNode, dj);
			}

			djNodes.add(exprNode);
		}

		if (djs.size() == 1) {

			asSingleNode = djs.iterator().next().toSingleNode();
		}
		else {

			ClassNode defnCls = matchStructures.addDefinitionClass();

			exprNode = defnCls;
			expressionMatcher = matchStructures.addDisjunction(defnCls, djNodes, true);
		}

		return exprNode;
	}

	NodeMatcher getExpressionMatcher() {

		return expressionMatcher;
	}

	NodeX toSingleNode() {

		return asSingleNode;
	}
}
