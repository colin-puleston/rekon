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

	static private final String LOCAL_CLASS_NAMES_PREFIX_FORMAT = "%s(%s)";

	private SinglePatternBuilder patternBuilder;

	private InstanceNode instanceNode;
	private PatternMatcher patternMatcher;

	private class InstanceClasses extends LocalClasses {

		private class InstancePatternClassNode extends LocalPatternClassNode {

			String getLabelPrefix() {

				String gen = super.getLabelPrefix();
				String spec = instanceNode.getLabel();

				return String.format(LOCAL_CLASS_NAMES_PREFIX_FORMAT, gen, spec);
			}
		}

		LocalPatternClassNode createLocalPatternClass() {

			return new InstancePatternClassNode();
		}

		LocalDefinitionClassNode createLocalDefinitionClass() {

			throw new Error("Cannot create instanceNode-definition class!");
		}
	}

	InstancePattern(InstanceNode instanceNode, SinglePatternBuilder patternBuilder) {

		this.instanceNode = instanceNode;
		this.patternBuilder = patternBuilder;

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

		Pattern p = patternBuilder.create(matchStructures);

		if (p == null) {

			throw new RuntimeException(
						"Invalid description for instanceNode: "
						+ instanceNode.getLabel());
		}

		return p;
	}
}
