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
abstract class FreeClasses {

	private FreeClassGenerator patternClasses = new FreeClassGenerator(ClassRole.PATTERN);
	private FreeClassGenerator disjunctionClasses = new FreeClassGenerator(ClassRole.DISJUNCTION);
	private FreeClassGenerator multiDefnClasses = new FreeClassGenerator(ClassRole.MULTI_DEFINITION);

	private enum ClassRole {PATTERN, DISJUNCTION, MULTI_DEFINITION}

	private class FreeClassNode extends ClassNode {

		private String label;

		public String getLabel() {

			return label;
		}

		public boolean mapped() {

			return false;
		}

		FreeClassNode(String label) {

			this.label = label;
		}

		boolean local() {

			return localClasses();
		}

		String getClassNameString() {

			return FreeClassNode.class.getSimpleName();
		}
	}

	private class FreeClassGenerator {

		private ClassRole classRole;

		private int nextIndex = 0;

		FreeClassGenerator(ClassRole classRole) {

			this.classRole = classRole;
		}

		FreeClassNode next() {

			FreeClassNode c = new FreeClassNode(createLabel());

			initialise(c);

			return c;
		}

		private String createLabel() {

			String suffix = getLabelSuffix();

			if (!suffix.isEmpty()) {

				suffix = "-" + suffix;
			}

			return getLabelPrefix() + "-" + classRole + "-" + nextIndex++ + suffix;
		}
	}

	abstract boolean localClasses();

	ClassNode createPatternClass() {

		return patternClasses.next();
	}

	ClassNode createDisjunctionClass() {

		return disjunctionClasses.next();
	}

	ClassNode createMultiDefinitionClass() {

		return multiDefnClasses.next();
	}

	abstract String getLabelPrefix();

	String getLabelSuffix() {

		return "";
	}

	void initialise(ClassNode c) {
	}
}
