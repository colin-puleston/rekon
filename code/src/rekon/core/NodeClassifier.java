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
class NodeClassifier extends NameClassifier {

	static private class NewInferredSubsumerExpander extends MultiThreadListProcessor<NodeX> {

		private boolean forMatchables;

		NewInferredSubsumerExpander(List<NodeX> all, boolean forMatchables) {

			this.forMatchables = forMatchables;

			invokeListProcesses(all);
		}

		void processElement(NodeX n) {

			getInferredSubsumers(n).expandLatestInferences(forMatchables);
		}
	}

	static void expandAllNewInferredSubsumers(List<NodeX> all) {

		do {

			new NewInferredSubsumerExpander(all, true);
			new NewInferredSubsumerExpander(all, false);
		}
		while(configureForNextInferenceExpansion(all));
	}

	static void absorbAllNewInferredSubsumers(List<NodeX> all) {

		for (NodeX n : all) {

			getInferredSubsumers(n).absorbNewInferences();
		}
	}

	static private boolean configureForNextInferenceExpansion(List<NodeX> all) {

		boolean expansions = false;

		for (NodeX n : all) {

			expansions |= getInferredSubsumers(n).configureForNextExpansion();
		}

		return expansions;
	}

	static private InferredSubsumers getInferredSubsumers(Name n) {

		return ((NodeX)n).getNodeClassifier().inferredSubsumers;
	}

	private InferredSubsumers inferredSubsumers = new NonMatcherInferredSubsumers();

	private boolean anyPhaseInferredSubsumers = false;
	private boolean anyLastPhaseInferredSubsumers = true;

	private abstract class InferredSubsumers {

		final NameSet allNewInferreds = new NameSet();

		abstract void addDirectlyInferred(Name subsumer);

		void expandLatestInferences(boolean forMatchable) {

			if (forMatchable) {

				expandLatestInferencesForMatchable();
			}
			else {

				expandLatestInferencesForNonMatchable();
			}
		}

		abstract void expandLatestInferencesForMatchable();

		abstract void expandLatestInferencesForNonMatchable();

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

		Names getMatchableSubsumerLatestInferreds(Name s) {

			return getInferredSubsumers(s).getMatchableLatestInferreds();
		}

		abstract Names getMatchableLatestInferreds();
	}

	private class MatcherInferredSubsumers extends InferredSubsumers {

		private NameSet latestInferreds = new NameSet();
		private NameSet currentExpansions = new NameSet();

		void addDirectlyInferred(Name subsumer) {

			allNewInferreds.add(subsumer);
			latestInferreds.add(subsumer);
		}

		void expandLatestInferencesForMatchable() {

			expandDirectLatestInferences();
			expandCurrentSubsumerLatestInferences();
		}

		void expandLatestInferencesForNonMatchable() {
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

		Names getMatchableLatestInferreds() {

			return latestInferreds;
		}

		private void expandDirectLatestInferences() {

			for (Name s : latestInferreds.getNames()) {

				addSubsumerExpansions(s.getSubsumers());
				addSubsumerExpansions(getInferredSubsumers(s).allNewInferreds);
			}
		}

		private void expandCurrentSubsumerLatestInferences() {

			for (Name s : getSubsumers().getNames()) {

				if (matchableNode(s)) {

					addSubsumerExpansions(getMatchableSubsumerLatestInferreds(s));
				}
			}
		}

		private void addSubsumerExpansions(Names subsumerSet) {

			for (Name s : subsumerSet.getNames()) {

				if (newSubsumer(s) && !allNewInferreds.contains(s)) {

					currentExpansions.add(s);
				}
			}
		}
	}

	private class NonMatcherInferredSubsumers extends InferredSubsumers {

		void addDirectlyInferred(Name subsumer) {

			throw new UnexpectedMethodInvocationError();
		}

		void expandLatestInferencesForMatchable() {
		}

		void expandLatestInferencesForNonMatchable() {

			for (Name s : getSubsumers().getNames()) {

				if (matchableNode(s)) {

					expandForNewMatchableSubsumers(s);
				}
			}
		}

		boolean configureForNextExpansion() {

			return false;
		}

		Names getMatchableLatestInferreds() {

			throw new UnexpectedMethodInvocationError();
		}

		private void expandForNewMatchableSubsumers(Name s) {

			for (Name ss : getMatchableSubsumerLatestInferreds(s).getNames()) {

				if (newSubsumer(ss)) {

					allNewInferreds.add(ss);
				}
			}
		}
	}

	NodeClassifier(NodeX node) {

		super(node);
	}

	void setMatchableNode() {

		inferredSubsumers = new MatcherInferredSubsumers();
	}

	boolean resetPhaseInferredSubsumers() {

		anyLastPhaseInferredSubsumers = anyPhaseInferredSubsumers;

		anyPhaseInferredSubsumers = false;

		return anyLastPhaseInferredSubsumers;
	}

	boolean anyLastPhaseInferredSubsumers() {

		return anyLastPhaseInferredSubsumers;
	}

	void checkAddInferredSubsumers(Names subsumers) {

		for (Name s : subsumers.getNames()) {

			if (newSubsumer(s)) {

				addNewInferredSubsumer(s);
			}
		}
	}

	synchronized void addNewInferredSubsumer(Name subsumer) {

		inferredSubsumers.addDirectlyInferred(subsumer);
	}

	boolean absorbNewInferredSubsumers() {

		return inferredSubsumers.absorbNewInferences();
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		return inferredSubsumers.anyNewInferences(selector);
	}

	private NodeX getNode() {

		return (NodeX)getName();
	}

	private boolean newSubsumer(Name s) {

		return s != getName() && !isSubsumer(s);
	}

	private boolean matchableNode(Name node) {

		return ((NodeX)node).matchable();
	}
}
