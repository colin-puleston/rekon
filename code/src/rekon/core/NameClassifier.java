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

	private InferredSubsumersImpl inferredSubsumers = null;

	private class InferredSubsumersImpl extends InferredSubsumers {

		private NameSet allNewInfs = new NameSet();
		private NameSet latestInfs = new NameSet();
		private NameSet latestExpansions = new NameSet();

		void checkAddDirectlyInferred(Name subsumer) {

			if (newSubsumer(subsumer)) {

				addDirectlyInferred(subsumer);
			}
		}

		void expandLatestInferences() {

			expandLatestInferences(subsumers);
			expandLatestInferences(latestInfs);
		}

		boolean configureForNextExpansion() {

			allNewInfs.addAll(latestInfs);
			latestInfs.clear();

			if (latestExpansions.isEmpty()) {

				return false;
			}

			latestInfs = latestExpansions;
			latestExpansions = new NameSet();

			return true;
		}

		boolean anyMatches(NodeMatcher matcher) {

			return matcher.anyMatches(allNewInfs);
		}

		void absorbIntoClassifier() {

			subsumers.addAll(allNewInfs);

			allNewInfs.clear();
		}

		private synchronized void addDirectlyInferred(Name subsumer) {

			allNewInfs.add(subsumer);
			latestInfs.add(subsumer);
		}

		private void expandLatestInferences(NameSet sourceSubsumers) {

			for (Name s : sourceSubsumers.getNames()) {

				if (sourceSubsumers == latestInfs) {

					addExpansions(s.getSubsumers());
				}

				InferredSubsumersImpl sa = s.getClassifier().getInferredSubsumersImpl();

				addExpansions(sa.allNewInfs);
				addExpansions(sa.latestInfs);
			}
		}

		private void addExpansions(Names subsumerExps) {

			for (Name s : subsumerExps.getNames()) {

				if (newSubsumer(s) && !allNewInfs.contains(s) && !latestInfs.contains(s)) {

					latestExpansions.add(s);
				}
			}
		}

		private boolean newSubsumer(Name s) {

			return s != name && !isSubsumer(s);
		}
	}

	NameClassifier(Name name) {

		this.name = name;
	}

	InferredSubsumers getInferredSubsumers() {

		return getInferredSubsumersImpl();
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

	boolean rootName() {

		return subsumers.isEmpty();
	}

	boolean multipleAsserteds() {

		return multipleAsserteds;
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

	private InferredSubsumersImpl getInferredSubsumersImpl() {

		if (inferredSubsumers == null) {

			inferredSubsumers = new InferredSubsumersImpl();
		}

		return inferredSubsumers;
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
}
