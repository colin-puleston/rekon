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
class NodeNameClassifier extends NameClassifier {

	static private class NewInferredSubsumerExpander extends MultiThreadListProcessor<NodeName> {

		private boolean forMatchableNode;

		NewInferredSubsumerExpander(List<NodeName> all, boolean forMatchableNode) {

			this.forMatchableNode = forMatchableNode;

			invokeListProcesses(all);
		}

		void processElement(NodeName n) {

			getInferredSubsumers(n).expandLatestInferences(forMatchableNode);
		}
	}

	static void expandAllNewInferredSubsumers(List<NodeName> all) {

		do {

			new NewInferredSubsumerExpander(all, true);
			new NewInferredSubsumerExpander(all, false);
		}
		while(configureForNextInferenceExpansion(all));
	}

	static void absorbAllNewInferredSubsumers(List<NodeName> all) {

		for (NodeName n : all) {

			getInferredSubsumers(n).absorbNewInferences();
		}
	}

	static private boolean configureForNextInferenceExpansion(List<NodeName> all) {

		boolean expansions = false;

		for (NodeName n : all) {

			expansions |= getInferredSubsumers(n).configureForNextExpansion();
		}

		return expansions;
	}

	static private InferredSubsumers getInferredSubsumers(Name n) {

		return ((NodeName)n).getNodeClassifier().inferredSubsumers;
	}

	private InferredSubsumers inferredSubsumers = new NonMatchableNodeInferredSubsumers();

	private abstract class InferredSubsumers {

		final NameSet allNewInferreds = new NameSet();

		abstract void addDirectlyInferred(Name subsumer);

		void expandLatestInferences(boolean forMatchableNode) {

			if (forMatchableNode) {

				expandLatestInferencesForMatchableNode();
			}
			else {

				expandLatestInferencesForNonMatchableNode();
			}
		}

		abstract void expandLatestInferencesForMatchableNode();

		abstract void expandLatestInferencesForNonMatchableNode();

		abstract boolean configureForNextExpansion();

		boolean anyNewInferences(NodeMatcher matcher) {

			return matcher.anyMatches(allNewInferreds);
		}

		boolean absorbNewInferences() {

			if (allNewInferreds.isEmpty()) {

				return false;
			}

			getSubsumers().addAll(allNewInferreds);

			allNewInferreds.clear();

			return true;
		}

		Names getMatchableSubsumerLatestInferreds(Name s) {

			return getInferredSubsumers(s).getMatchableNodeLatestInferreds();
		}

		abstract Names getMatchableNodeLatestInferreds();
	}

	private class MatchableNodeInferredSubsumers extends InferredSubsumers {

		private NameSet latestInferreds = new NameSet();
		private NameSet currentExpansions = new NameSet();

		void addDirectlyInferred(Name subsumer) {

			allNewInferreds.add(subsumer);
			latestInferreds.add(subsumer);
		}

		void expandLatestInferencesForMatchableNode() {

			expandDirectLatestInferences();
			expandCurrentSubsumerLatestInferences();
		}

		void expandLatestInferencesForNonMatchableNode() {
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

		Names getMatchableNodeLatestInferreds() {

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

	private class NonMatchableNodeInferredSubsumers extends InferredSubsumers {

		void addDirectlyInferred(Name subsumer) {

			throw new Error("Unexpected method invocation!");
		}

		void expandLatestInferencesForMatchableNode() {
		}

		void expandLatestInferencesForNonMatchableNode() {

			for (Name s : getSubsumers().getNames()) {

				if (matchableNode(s)) {

					expandForNewMatchableSubsumers(s);
				}
			}
		}

		boolean configureForNextExpansion() {

			return false;
		}

		Names getMatchableNodeLatestInferreds() {

			throw new Error("Unexpected method invocation!");
		}

		private void expandForNewMatchableSubsumers(Name s) {

			for (Name ss : getMatchableSubsumerLatestInferreds(s).getNames()) {

				if (newSubsumer(ss)) {

					allNewInferreds.add(ss);
				}
			}
		}
	}

	NodeNameClassifier(NodeName name) {

		super(name);
	}

	void setMatchableNode() {

		inferredSubsumers = new MatchableNodeInferredSubsumers();
	}

	synchronized void addNewInferredSubsumer(Name subsumer) {

		inferredSubsumers.addDirectlyInferred(subsumer);
	}

	boolean absorbNewInferredSubsumers() {

		return inferredSubsumers.absorbNewInferences();
	}

	boolean anyNewSubsumers(NodeMatcher matcher) {

		return inferredSubsumers.anyNewInferences(matcher);
	}

	private boolean newSubsumer(Name s) {

		return s != getName() && !isSubsumer(s);
	}

	private boolean matchableNode(Name name) {

		return ((NodeName)name).matchable();
	}
}
