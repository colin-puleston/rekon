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

	static private class PhaseInferredSubsumerExpander extends MultiThreadListProcessor<NodeX> {

		protected void processElement(NodeX n, int threadIndex) {

			n.getNodeClassifier().expandPhaseInferences();
		}

		PhaseInferredSubsumerExpander(Iterable<NodeX> all) {

			invokeListProcesses(all);
		}
	}

	static void absorbPassInferences(Iterable<NodeX> all) {

		for (NodeX n : all) {

			n.getNodeClassifier().absorbPassInferences();
		}
	}

	static void expandPhaseInferences(Iterable<NodeX> all) {

		new PhaseInferredSubsumerExpander(all);
	}

	static void absorbPhaseInferenceExpansions(Iterable<NodeX> all) {

		for (NodeX n : all) {

			n.getNodeClassifier().absorbPhaseInferenceExpansions();
		}
	}

	private NameSet passInferredSubsumers = new NameSet();
	private NameSet phaseInferredSubsumers = new NameSet();
	private NameSet phaseInferredSubsumerExpansions = new NameSet();

	NodeClassifier(NodeX node) {

		super(node);
	}

	synchronized void addNewInferredSubsumer(Name subsumer) {

		passInferredSubsumers.add(subsumer);

		for (NodeX ss : subsumer.getSubsumers().asNodes()) {

			if (newSubsumer(ss)) {

				passInferredSubsumers.add(ss);
			}
		}
	}

	boolean absorbPassInferences() {

		phaseInferredSubsumers.addAll(passInferredSubsumers);

		return absorbInferreds(passInferredSubsumers);
	}

	void expandPhaseInferences() {

		for (NodeX s : phaseInferredSubsumers.asNodes()) {

			expandPhaseInferred(s);
		}

		for (NodeX s : getSubsumers().asNodes()) {

			expandPhaseInferreds(s);
		}
	}

	boolean absorbPhaseInferenceExpansions() {

		phaseInferredSubsumers.clear();

		return absorbInferreds(phaseInferredSubsumerExpansions);
	}

	boolean absorbExpandedLocalInferences() {

		for (NodeX s : passInferredSubsumers.copyNodes()) {

			passInferredSubsumers.addAll(s.getSubsumers());
		}

		return absorbInferreds(passInferredSubsumers);
	}

	boolean anyPassInferredSubsumers(NodeSelector selector) {

		return !passInferredSubsumers.isEmpty();
	}

	boolean anyPhaseInferredSubsumerExpansions(NodeSelector selector) {

		return !phaseInferredSubsumerExpansions.isEmpty();
	}

	private void expandPhaseInferences(Names infSubsumers) {

		for (NodeX s : infSubsumers.asNodes()) {

			if (newInferredSubsumer(s) && phaseInferredSubsumerExpansions.add(s)) {

				expandPhaseInferred(s);
			}
		}
	}

	private void expandPhaseInferreds(NodeX n) {

		expandPhaseInferences(n.getNodeClassifier().phaseInferredSubsumers);
	}

	private void expandPhaseInferred(NodeX s) {

		expandPhaseInferreds(s);
		expandPhaseInferences(s.getSubsumers());
	}

	private boolean absorbInferreds(NameSet inferreds) {

		if (inferreds.isEmpty()) {

			return false;
		}

		getSubsumers().addAll(inferreds);

		inferreds.clear();

		return true;
	}

	private boolean newInferredSubsumer(NodeX s) {

		return newSubsumer(s) && !phaseInferredSubsumers.contains(s);
	}

	private boolean newSubsumer(NodeX s) {

		return s != getName() && !hasSubsumer(s);
	}
}
