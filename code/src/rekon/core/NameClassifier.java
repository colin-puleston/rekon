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
class NameClassifier extends NameClassificationHandler {

	private Name name;
	private NameSet subsumers = new NameSet();

	private boolean multipleAssertedSubsumers = false;

	NameClassifier(Name name) {

		this.name = name;
	}

	NameClassification createClassification() {

		return new NameClassification(name, subsumers);
	}

	void addAssertedSubsumer(Name subsumer) {

		checkAddAssertedSubsumer(subsumer);
	}

	void addAssertedSubsumers(Names subsumers) {

		for (Name s : subsumers.getNames()) {

			checkAddAssertedSubsumer(s);
		}
	}

	void addAssertedEquivalent(Name equiv) {

		addAssertedSubsumer(equiv);

		equiv.getClassifier().addAssertedSubsumer(name);
	}

	void onPostAssertionAdditions() {

		for (Name s : subsumers.copyNames()) {

			expandAssertedsFrom(s);
		}
	}

	boolean rootName() {

		return subsumers.isEmpty();
	}

	Name getName() {

		return name;
	}

	NameSet getSubsumers() {

		return subsumers;
	}

	boolean isSubsumer(Name test) {

		return subsumers.contains(test);
	}

	boolean multipleAssertedSubsumers() {

		return multipleAssertedSubsumers;
	}

	private void expandAssertedsFrom(Name current) {

		for (Name next : current.getSubsumers().getNames()) {

			if (checkAddSubsumer(next)) {

				expandAssertedsFrom(next);
			}
		}
	}

	private void checkAddAssertedSubsumer(Name subsumer) {

		if (checkAddSubsumer(subsumer)) {

			multipleAssertedSubsumers |= subsumers.size() > 1;
		}
	}

	private boolean checkAddSubsumer(Name subsumer) {

		return subsumer != name && subsumers.add(subsumer);
	}
}
