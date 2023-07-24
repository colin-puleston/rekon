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
abstract class LocalPattern {

	private GNode patternNode;
	private Pattern pattern;
	private OrderedProfileMatchers profileMatchers = new OrderedProfileMatchers();

	abstract class LocalClasses extends FreeClasses {

		abstract class LocalPatternClassNode extends PatternClassNode {

			boolean local() {

				return true;
			}
		}

		ClassNode createGCIImpliedClass() {

			throw new Error("Cannot create local GCI-implied classes!");
		}
	}

	void initialise(PatternCreator patternCreator) {

		MatchStructures matchStructures = createMatchStructures();

		patternNode = ensurePatternNode(matchStructures);
		pattern = patternCreator.createNestedPatterns(matchStructures);

		profileMatchers.addDefinitionPattern(patternNode, pattern);

		processAllLocalNamesPostAdditions();
	}

	GNode getPatternNode() {

		return patternNode;
	}

	Pattern getPattern() {

		return pattern;
	}

	OrderedProfileMatchers getProfileMatchers() {

		return profileMatchers;
	}

	abstract LocalClasses createLocalClasses();

	abstract GNode ensurePatternNode(MatchStructures matchStructures);

	private MatchStructures createMatchStructures() {

		return new MatchStructures(profileMatchers, createLocalClasses());
	}

	private void processAllLocalNamesPostAdditions() {

		for (PatternMatcher p : profileMatchers.getProfilePatterns()) {

			p.getNode().getClassifier().onPostAssertionAdditions();
		}
	}
}
