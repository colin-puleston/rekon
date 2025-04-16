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

/**
 * @author Colin Puleston
 */
class InstancePattern extends LocalExpression {

	private SinglePatternSource patternSource;

	private InstanceNode instanceNode;
	private PatternMatcher patternMatcher;

	private class InstanceClasses extends FreeClasses {

		boolean localClasses() {

			return true;
		}

		String getLabelPrefix() {

			return "INSTANCE";
		}

		String getLabelSuffix() {

			return '(' + instanceNode.getLabel() + ')';
		}
	}

	InstancePattern(InstanceNode instanceNode, SinglePatternSource patternSource) {

		this.instanceNode = instanceNode;
		this.patternSource = patternSource;

		initialise(new InstanceClasses());
	}

	NodeX createExpression(MatchStructures matchStructures) {

		patternMatcher = createPatternMatcher(matchStructures);

		return instanceNode;
	}

	NodeMatcher getExpressionMatcher() {

		return patternMatcher;
	}

	private PatternMatcher createPatternMatcher(MatchStructures matchStructures) {

		Pattern p = createPattern(matchStructures);
		NodeX n = p.toSingleNode();

		if (n != null) {

			return new PatternMatcher(n);
		}

		return matchStructures.addDefinitionPattern(instanceNode, p);
	}

	private Pattern createPattern(MatchStructures matchStructures) {

		Pattern p = patternSource.create(matchStructures);

		if (p == null) {

			throw new RuntimeException(
						"Invalid description for instance-node: "
						+ instanceNode.getLabel());
		}

		return p;
	}
}
