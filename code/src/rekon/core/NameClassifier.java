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

	private boolean multipleAsserteds = false;

	private InferredSubsumers inferredSubsumers = new InferredSubsumers();

	private class InferredSubsumers {

		private NameSet collected = new NameSet();
		private NameSet latest = new NameSet();
		private NameSet expansions = new NameSet();

		void checkAddDirectlyInferred(Name subsumer) {

			if (newSubsumer(subsumer) && collected.add(subsumer)) {

				latest.add(subsumer);
			}
		}

		void expandLatest() {

			expandLatest(subsumers);
			expandLatest(latest);
		}

		boolean configureForNextExpansion() {

			collected.addAll(latest);
			latest.clear();

			if (expansions.isEmpty()) {

				return false;
			}

			latest = expansions;
			expansions = new NameSet();

			return true;
		}

		void absorbCollected() {

			subsumers.addAll(collected);

			collected.clear();
		}

		boolean anyInferences() {

			return !collected.isEmpty();
		}

		private void expandLatest(NameSet sourceSubsumers) {

			for (Name s : sourceSubsumers.getNames()) {

				if (sourceSubsumers == latest) {

					addExpansions(s.getSubsumers());
				}

				InferredSubsumers si = s.getClassifier().inferredSubsumers;

				addExpansions(si.collected);
				addExpansions(si.latest);
			}
		}

		private void addExpansions(Names subsumerExps) {

			for (Name s : subsumerExps.getNames()) {

				if (newSubsumer(s) && !collected.contains(s) && !latest.contains(s)) {

					expansions.add(s);
				}
			}
		}
	}

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

		inferredSubsumers.checkAddDirectlyInferred(subsumer);
	}

	void expandLatestInferences() {

		inferredSubsumers.expandLatest();
	}

	boolean configureForNextInferenceExpansion() {

		return inferredSubsumers.configureForNextExpansion();
	}

	void absorbNewInferences() {

		inferredSubsumers.absorbCollected();
	}

	boolean anyNewInferences() {

		return inferredSubsumers.anyInferences();
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

	private boolean newSubsumer(Name subsumer) {

		return subsumer != name && !subsumers.contains(subsumer);
	}
}
