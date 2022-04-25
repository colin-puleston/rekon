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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
class NameClassifier {

	private Name name;
	private NameSet subsumers = new NameSet();

	private boolean multipleAsserteds = false;
	private boolean newInferreds = false;

	NameClassifier(Name name) {

		this.name = name;
	}

	NameClassification createClassification() {

		return new NameClassification(name, subsumers);
	}

	void addAssertedSubsumer(Name subsumer) {

		checkAddSubsumer(subsumer);
	}

	void addAssertedSubsumers(Names subsumers) {

		checkAddSubsumers(subsumers);
	}

	void addAssertedEquivalent(Name equiv) {

		addAssertedSubsumer(equiv);

		equiv.getClassifier().addAssertedSubsumer(name);
	}

	void onPostAssertionAdditions() {

		if (subsumers.size() > 1) {

			multipleAsserteds = true;
		}

		for (Name s : subsumers.copyNames()) {

			expandAssertedsFrom(s);
		}
	}

	void checkAddInferred(Name subsumer) {

		if (checkAddSubsumer(subsumer)) {

			checkAddSubsumers(subsumer.getSubsumers());

			newInferreds = true;
		}
	}

	boolean expandNewInferreds() {

		boolean expansions = false;

		for (Name subsumer : subsumers.copyNames()) {

			if (subsumer.getClassifier().newInferreds()) {

				for (Name subsSubsumer : subsumer.getSubsumers().getNames()) {

					if (checkAddSubsumer(subsSubsumer)) {

						expansions = true;
					}
				}
			}
		}

		return expansions;
	}

	void resetNewInferreds() {

		newInferreds = false;
	}

	boolean rootName() {

		return subsumers.isEmpty();
	}

	boolean multipleAsserteds() {

		return multipleAsserteds;
	}

	boolean newInferreds() {

		return newInferreds;
	}

	boolean noSubsumers() {

		return subsumers.isEmpty();
	}

	boolean isSubsumer(Name test) {

		return subsumers.contains(test);
	}

	NameSet getSubsumers() {

		return subsumers;
	}

	private void expandAssertedsFrom(Name current) {

		for (Name next : current.getSubsumers().getNames()) {

			if (checkAddSubsumer(next)) {

				expandAssertedsFrom(next);
			}
		}
	}

	private void checkAddSubsumers(Names subsumers) {

		for (Name s : subsumers.getNames()) {

			checkAddSubsumer(s);
		}
	}

	private boolean checkAddSubsumer(Name subsumer) {

		return subsumer != name && subsumers.add(subsumer);
	}
}
