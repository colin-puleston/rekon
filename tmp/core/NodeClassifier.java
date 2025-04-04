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

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class NodeClassifier extends NameClassifier {

	static private class NewInferredSubsumerExpander extends MultiThreadListProcessor<NodeX> {

		private boolean forMatchables;

		protected void processElement(NodeX n) {

			if (n.matchable() == forMatchables) {

				getInferredSubsumers(n).expandLatestInferences();
			}
		}

		NewInferredSubsumerExpander(Iterable<NodeX> all, boolean forMatchables) {

			this.forMatchables = forMatchables;

			invokeListProcesses(all);
		}
	}

	static void expandAllNewInferredSubsumers(Iterable<NodeX> all) {

		do {

			new NewInferredSubsumerExpander(all, true);
			new NewInferredSubsumerExpander(all, false);
		}
		while(configureForNextInferenceExpansion(all));
	}

	static void absorbAllNewInferredSubsumers(Iterable<NodeX> all) {

		for (NodeX n : all) {

			getInferredSubsumers(n).absorbNewInferences();
		}
	}

	static boolean resetAllPhaseInferredSubsumers(Iterable<NodeX> all) {

		boolean anyInfs = false;

		for (NodeX n : all) {

			if (n.getNodeClassifier().resetPhaseInferredSubsumers()) {

				anyInfs |= true;
			}
		}

		return anyInfs;
	}

	static private boolean configureForNextInferenceExpansion(Iterable<NodeX> all) {

		boolean expansions = false;

		for (NodeX n : all) {

			expansions |= getInferredSubsumers(n).configureForNextExpansion();
		}

		return expansions;
	}

	static private InferredSubsumers getInferredSubsumers(NodeX n) {

		return n.getNodeClassifier().inferredSubsumers;
	}

	static private ActiveInferredSubsumers getActiveInferredSubsumers(NodeX n) {

		return (ActiveInferredSubsumers)getInferredSubsumers(n);
	}

	private InferredSubsumers inferredSubsumers = new InactiveInferredSubsumers();

	private boolean anyPhaseInferredSubsumers = false;

	private abstract class InferredSubsumers {

		final NameSet allNewInferreds = new NameSet();

		abstract void addDirectlyInferred(Name subsumer);

		abstract void expandLatestInferences();

		abstract boolean configureForNextExpansion();

		boolean anyNewInferences(NodeSelector selector) {

			return selector.anyMatches(allNewInferreds);
		}

		boolean absorbNewInferences() {

			if (allNewInferreds.isEmpty()) {

				return false;
			}

			getSubsumers().addAll(allNewInferreds);

			allNewInferreds.clear();
			anyPhaseInferredSubsumers = true;

			return true;
		}

		void expandNewLocallyInferredSubsumers() {

			for (Name n : allNewInferreds.copyNames()) {

				allNewInferreds.addAll(n.getSubsumers());
			}
		}

		Names getActiveSubsumerLatestInferreds(NodeX s) {

			return getActiveInferredSubsumers(s).latestInferreds;
		}
	}

	private class ActiveInferredSubsumers extends InferredSubsumers {

		private NameSet latestInferreds = new NameSet();
		private NameSet currentExpansions = new NameSet();

		void addDirectlyInferred(Name subsumer) {

			allNewInferreds.add(subsumer);
			latestInferreds.add(subsumer);
		}

		void expandLatestInferences() {

			expandDirectLatestInferences();
			expandCurrentSubsumerLatestInferences();
		}

		boolean configureForNextExpansion() {

			allNewInferreds.addAll(currentExpansions);
			latestInferreds.clear();

			if (currentExpansions.isEmpty()) {

				return false;
			}

			latestInferreds = currentExpansions;
			currentExpansions = new NameSet();

			return true;
		}

		private void expandDirectLatestInferences() {

			for (NodeX s : latestInferreds.asNodes()) {

				addSubsumerExpansions(s.getSubsumers());
				addSubsumerExpansions(getInferredSubsumers(s).allNewInferreds);
			}
		}

		private void expandCurrentSubsumerLatestInferences() {

			for (NodeX s : getSubsumers().asNodes()) {

				if (s.matchable()) {

					addSubsumerExpansions(getActiveSubsumerLatestInferreds(s));
				}
			}
		}

		private void addSubsumerExpansions(Names subsumerSet) {

			for (NodeX s : subsumerSet.asNodes()) {

				if (newSubsumer(s) && !allNewInferreds.contains(s)) {

					currentExpansions.add(s);
				}
			}
		}
	}

	private class InactiveInferredSubsumers extends InferredSubsumers {

		void addDirectlyInferred(Name subsumer) {

			throw new UnexpectedMethodInvocationError();
		}

		void expandLatestInferences() {

			for (NodeX s : getSubsumers().asNodes()) {

				if (s.matchable()) {

					expandForNewMatchableSubsumer(s);
				}
			}
		}

		boolean configureForNextExpansion() {

			return false;
		}

		private void expandForNewMatchableSubsumer(NodeX s) {

			for (NodeX ss : getActiveSubsumerLatestInferreds(s).asNodes()) {

				if (newSubsumer(ss)) {

					allNewInferreds.add(ss);
				}
			}
		}
	}

	NodeClassifier(NodeX node) {

		super(node);
	}

	void setClassifiableNode() {

		if (inferredSubsumers instanceof InactiveInferredSubsumers) {

			inferredSubsumers = new ActiveInferredSubsumers();
		}
	}

	void checkAddInferredSubsumers(Names subsumers) {

		for (NodeX s : subsumers.asNodes()) {

			checkAddInferredSubsumer(s);
		}
	}

	void checkAddInferredSubsumer(NodeX subsumer) {

		if (newSubsumer(subsumer)) {

			addNewInferredSubsumer(subsumer);
		}
	}

	synchronized void addNewInferredSubsumer(Name subsumer) {

		inferredSubsumers.addDirectlyInferred(subsumer);
	}

	boolean absorbNewLocallyInferredSubsumerExpansions() {

		inferredSubsumers.expandNewLocallyInferredSubsumers();

		return inferredSubsumers.absorbNewInferences();
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		return inferredSubsumers.anyNewInferences(selector);
	}

	private boolean resetPhaseInferredSubsumers() {

		boolean any = anyPhaseInferredSubsumers;

		anyPhaseInferredSubsumers = false;

		return any;
	}

	private NodeX getNode() {

		return (NodeX)getName();
	}

	private boolean newSubsumer(NodeX s) {

		return s != getName() && !hasSubsumer(s);
	}
}
