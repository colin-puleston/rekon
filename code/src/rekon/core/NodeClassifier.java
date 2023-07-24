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

	static private class NewInferredSubsumerExpander extends MultiThreadListProcessor<GNode> {

		private boolean forNodeMatcher;

		NewInferredSubsumerExpander(List<GNode> all, boolean forNodeMatcher) {

			this.forNodeMatcher = forNodeMatcher;

			invokeListProcesses(all);
		}

		void processElement(GNode n) {

			getInferredSubsumers(n).expandLatestInferences(forNodeMatcher);
		}
	}

	static void expandAllNewInferredSubsumers(List<GNode> all) {

		do {

			new NewInferredSubsumerExpander(all, true);
			new NewInferredSubsumerExpander(all, false);
		}
		while(configureForNextInferenceExpansion(all));
	}

	static void absorbAllNewInferredSubsumers(List<GNode> all) {

		for (GNode n : all) {

			getInferredSubsumers(n).absorbNewInferences();
		}
	}

	static private boolean configureForNextInferenceExpansion(List<GNode> all) {

		boolean expansions = false;

		for (GNode n : all) {

			expansions |= getInferredSubsumers(n).configureForNextExpansion();
		}

		return expansions;
	}

	static private InferredSubsumers getInferredSubsumers(Name n) {

		return ((GNode)n).getNodeClassifier().inferredSubsumers;
	}

	private InferredSubsumers inferredSubsumers = new NonMatcherInferredSubsumers();

	private abstract class InferredSubsumers {

		final NameSet allNewInferreds = new NameSet();

		abstract void addDirectlyInferred(Name subsumer);

		void expandLatestInferences(boolean forNodeMatcher) {

			if (forNodeMatcher) {

				expandLatestInferencesForNodeMatcher();
			}
			else {

				expandLatestInferencesForNonNodeMatcher();
			}
		}

		abstract void expandLatestInferencesForNodeMatcher();

		abstract void expandLatestInferencesForNonNodeMatcher();

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

			return true;
		}

		Names getNodeMatcherSubsumerLatestInferreds(Name s) {

			return getInferredSubsumers(s).getNodeMatcherLatestInferreds();
		}

		abstract Names getNodeMatcherLatestInferreds();
	}

	private class MatcherInferredSubsumers extends InferredSubsumers {

		private NameSet latestInferreds = new NameSet();
		private NameSet currentExpansions = new NameSet();

		void addDirectlyInferred(Name subsumer) {

			allNewInferreds.add(subsumer);
			latestInferreds.add(subsumer);
		}

		void expandLatestInferencesForNodeMatcher() {

			expandDirectLatestInferences();
			expandCurrentSubsumerLatestInferences();
		}

		void expandLatestInferencesForNonNodeMatcher() {
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

		Names getNodeMatcherLatestInferreds() {

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

					addSubsumerExpansions(getNodeMatcherSubsumerLatestInferreds(s));
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

		void expandLatestInferencesForNodeMatcher() {
		}

		void expandLatestInferencesForNonNodeMatcher() {

			for (Name s : getSubsumers().getNames()) {

				if (matchableNode(s)) {

					expandForNewNodeMatcherSubsumers(s);
				}
			}
		}

		boolean configureForNextExpansion() {

			return false;
		}

		Names getNodeMatcherLatestInferreds() {

			throw new UnexpectedMethodInvocationError();
		}

		private void expandForNewNodeMatcherSubsumers(Name s) {

			for (Name ss : getNodeMatcherSubsumerLatestInferreds(s).getNames()) {

				if (newSubsumer(ss)) {

					allNewInferreds.add(ss);
				}
			}
		}
	}

	NodeClassifier(GNode node) {

		super(node);
	}

	void setMatchableNode() {

		inferredSubsumers = new MatcherInferredSubsumers();
	}

	void onPostAssertionAdditions() {

		super.onPostAssertionAdditions();

		for (DisjunctionMatcher d : getNode().getDisjunctionMatchers()) {

			d.setPreInferredCommonDisjunctSubsumers();
		}
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

	private GNode getNode() {

		return (GNode)getName();
	}

	private boolean newSubsumer(Name s) {

		return s != getName() && !isSubsumer(s);
	}

	private boolean matchableNode(Name node) {

		return ((GNode)node).matchable();
	}
}
