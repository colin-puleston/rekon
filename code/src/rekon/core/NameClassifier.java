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
class NameClassifier {

	private Name name;
	private NameSet subsumers = new NameSet();

	private NameSet allNewInferredSubsumers = new NameSet();
	private NameSet latestInferredSubsumers = new NameSet();
	private NameSet inferredSubsumerExpansions = new NameSet();

	private boolean multipleAsserteds = false;

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

		if (subsumers.size() > 1) {

			multipleAsserteds = true;
		}

		for (Name s : subsumers.copyNames()) {

			expandAssertedsFrom(s);
		}
	}

	void checkNewInferredSubsumer(Name subsumer) {

		checkAddInferredSubsumer(latestInferredSubsumers, subsumer);
	}

	void expandLatestNewInferredSubsumers() {

		for (Name s : latestInferredSubsumers.getNames()) {

			addInferredSubsumerExpansions(s.getSubsumers());
			addInferredSubsumerExpansions(s.getClassifier().latestInferredSubsumers);
		}
	}

	boolean resetInferredSubsumerExpansions() {

		latestInferredSubsumers.clear();

		if (inferredSubsumerExpansions.isEmpty()) {

			return false;
		}

		latestInferredSubsumers = inferredSubsumerExpansions;
		inferredSubsumerExpansions = new NameSet();

		return true;
	}

	boolean anyNewInferredSubsumers() {

		return !allNewInferredSubsumers.isEmpty();
	}

	void absorbNewInferredSubsumers() {

		subsumers.addAll(allNewInferredSubsumers);

		allNewInferredSubsumers.clear();
	}

	boolean rootName() {

		return subsumers.isEmpty();
	}

	boolean multipleAsserteds() {

		return multipleAsserteds;
	}

	boolean isSubsumer(Name test) {

		return subsumers.contains(test);
	}

	NameSet getSubsumers() {

		return subsumers;
	}

	private void expandAssertedsFrom(Name current) {

		for (Name next : current.getSubsumers().getNames()) {

			if (checkAddAssertedSubsumer(next)) {

				expandAssertedsFrom(next);
			}
		}
	}

	private boolean checkAddAssertedSubsumer(Name subsumer) {

		return subsumer != name && subsumers.add(subsumer);
	}

	private void addInferredSubsumerExpansions(Names subsumerExps) {

		for (Name s : subsumerExps.getNames()) {

			checkAddInferredSubsumer(inferredSubsumerExpansions, s);
		}
	}

	private void checkAddInferredSubsumer(Names targetInferreds, Name subsumer) {

		if (subsumer != name && !subsumers.contains(subsumer)) {

			if (allNewInferredSubsumers.add(subsumer)) {

				targetInferreds.add(subsumer);
			}
		}
	}
}
