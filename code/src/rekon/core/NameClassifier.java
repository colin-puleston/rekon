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
class NameClassifier extends NameClassificationHandler {

	static private class NewInferredSubsumerExpander extends MultiThreadListProcessor<NodeName> {

		NewInferredSubsumerExpander(List<NodeName> all) {

			invokeListProcesses(all);
		}

		void processElement(NodeName n) {

			n.getClassifier().inferredSubsumers.expandLatestInferences();
		}
	}

	static void expandAllNewInferredSubsumers(List<NodeName> all) {

		do {

			new NewInferredSubsumerExpander(all);
		}
		while(configureForNextInferenceExpansion(all));
	}

	static void absorbAllNewInferredSubsumers(List<NodeName> all) {

		for (NodeName n : all) {

			n.getClassifier().inferredSubsumers.absorbNewInferences();
		}
	}

	static private boolean configureForNextInferenceExpansion(List<NodeName> all) {

		boolean expansions = false;

		for (NodeName n : all) {

			expansions |= n.getClassifier().inferredSubsumers.configureForNextExpansion();
		}

		return expansions;
	}

	private Name name;
	private NameSet subsumers = new NameSet();

	private boolean multipleAsserteds = false;

	private InferredSubsumers inferredSubsumers = new InferredSubsumers();

	private class InferredSubsumers {

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

		boolean anyNewInferences(NodeMatcher matcher) {

			return matcher.anyMatches(allNewInfs);
		}

		boolean absorbNewInferences() {

			if (allNewInfs.isEmpty()) {

				return false;
			}

			subsumers.addAll(allNewInfs);

			allNewInfs.clear();

			return true;
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

				InferredSubsumers sis = s.getClassifier().inferredSubsumers;

				addExpansions(sis.allNewInfs);
				addExpansions(sis.latestInfs);
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

	void registerDirectlyInferredSubsumer(Name subsumer) {

		inferredSubsumers.checkAddDirectlyInferred(subsumer);
	}

	boolean absorbNewInferredSubsumers() {

		return inferredSubsumers.absorbNewInferences();
	}

	boolean classified() {

		return false;
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

	boolean multipleAsserteds() {

		return multipleAsserteds;
	}

	boolean anyNewInferredSubsumers(NodeMatcher matcher) {

		return inferredSubsumers.anyNewInferences(matcher);
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
