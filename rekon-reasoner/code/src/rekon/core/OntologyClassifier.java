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
class OntologyClassifier extends NodeMatcherClassifier {

	private Ontology ontology;
	private OntologyClassifyListener classifyListener;

	private class PatternSubsumedsChecker extends MultiThreadListProcessor<PatternMatcher> {

		private PotentialPatternSubsumeds candidatesFilter;

		protected void processElement(PatternMatcher defn) {

			for (PatternMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				checkSubsumption(defn, c);
			}
		}

		PatternSubsumedsChecker(List<PatternMatcher> candidates) {

			candidatesFilter = new PotentialCorePatternSubsumeds(candidates);

			invokeListProcesses(ontology.getDefinitionPatterns());
		}
	}

	private class DisjunctionSubsumedsChecker extends MultiThreadListProcessor<DisjunctionMatcher> {

		private PotentialDisjunctionSubsumeds candidatesFilter;

		protected void processElement(DisjunctionMatcher defn) {

			for (DisjunctionMatcher c : candidatesFilter.getPotentialsFor(defn)) {

				checkSubsumption(defn, c);
			}
		}

		DisjunctionSubsumedsChecker(List<DisjunctionMatcher> candidates) {

			candidatesFilter = new PotentialDisjunctionSubsumeds(candidates);

			invokeListProcesses(ontology.getDefinitionDisjunctions());
		}
	}

	private class Pass {

		private ClassifyPassType passType;

		private List<PatternMatcher> patternMatchCandidates
										= new ArrayList<PatternMatcher>();

		private List<DisjunctionMatcher> disjunctionMatchCandidates
											= new ArrayList<DisjunctionMatcher>();
		private List<DisjunctionMatcher> disjunctionClassifyCandidates
											= new ArrayList<DisjunctionMatcher>();

		private RelationEndPointSubsumptions relationEndPointSubsumptions;

		Pass(ClassifyPassType passType) {

			this.passType = passType;

			relationEndPointSubsumptions = createRelationEndPointSubsumptions();

			if (!expansionPass()) {

				findAllCandidates();
			}
		}

		boolean initialisePass() {

			if (expansionPass()) {

				ProfilePatternsExpander.expandAll(ontology);

				findAllCandidates();
			}

			return candidateCount() != 0;
		}

		Pass perfomPass() {

			checkSubsumptions();

			Iterable<NodeX> allNodes = ontology.getAllNodes();

			expandAllNewInferences();

			Pass next = new Pass(ClassifyPassType.DEFAULT);

			absorbAllNewInferences();

			return next;
		}

		int candidateCount() {

			return patternMatchCandidates.size()
					+ disjunctionClassifyCandidates.size()
					+ relationEndPointSubsumptions.candidateCount();
		}

		boolean phaseInitialPass() {

			return passType != ClassifyPassType.DEFAULT;
		}

		boolean patternMatchCandidate(Pattern pattern) {

			return expansionPass()
					? pattern.expandedProfile()
					: pattern.matchable(phaseInitialPass());
		}

		private RelationEndPointSubsumptions createRelationEndPointSubsumptions() {

			return new RelationEndPointSubsumptions(ontology, passType);
		}

		private boolean expansionPass() {

			return passType == ClassifyPassType.EXPANSION;
		}

		private void findAllCandidates() {

			findPatternClassifyCandidates();
			findDisjunctionClassifyCandidates();
		}

		private void findPatternClassifyCandidates() {

			for (PatternMatcher m : ontology.getProfilePatterns()) {

				if (patternMatchCandidate(m.getPattern())) {

					patternMatchCandidates.add(m);
				}

				relationEndPointSubsumptions.checkAddInferenceSource(m);
			}
		}

		private void findDisjunctionClassifyCandidates() {

			boolean initPass = phaseInitialPass();

			for (DisjunctionMatcher d : ontology.getAllDisjunctions()) {

				if (d.unprocessedSubsumers(initPass)) {

					disjunctionClassifyCandidates.add(d);

					if (d.matchable(initPass)) {

						disjunctionMatchCandidates.add(d);
					}
				}
			}
		}

		private void checkSubsumptions() {

			new PatternSubsumedsChecker(patternMatchCandidates);
			new DisjunctionSubsumedsChecker(disjunctionMatchCandidates);

			inferNewCommonDisjunctSubsumers();
			relationEndPointSubsumptions.inferNewSubsumptions();
		}

		private void inferNewCommonDisjunctSubsumers() {

			for (DisjunctionMatcher d : disjunctionClassifyCandidates) {

				d.inferNewCommonDisjunctSubsumers();
			}
		}

		private void expandAllNewInferences() {

			NodeClassifier.expandAllNewInferredSubsumers(getAllNodes());
		}

		private void absorbAllNewInferences() {

			NodeClassifier.absorbAllNewInferredSubsumers(getAllNodes());
		}
	}

	private class Phase {

		private boolean initialPhase;
		private Pass initialPass;

		Phase() {

			this(true, ClassifyPassType.INITIAL);
		}

		boolean performPhase() {

			Pass pass = initialPass;

			while (pass.initialisePass()) {

				if (pass.phaseInitialPass()) {

					classifyListener.onPhaseStart();
				}

				classifyListener.onPassStart(pass.candidateCount());

				pass = pass.perfomPass();
			}

			if (initialPhase) {

				resetAllPhaseInferredSubsumers();

				return true;
			}

			if (pass.phaseInitialPass()) {

				return false;
			}

			return resetAllPhaseInferredSubsumers();
		}

		Phase createNextPhase() {

			return new Phase(false, ClassifyPassType.EXPANSION);
		}

		private Phase(boolean initialPhase, ClassifyPassType initialPassType) {

			this.initialPhase = initialPhase;

			initialPass = new Pass(initialPassType);
		}

		private boolean resetAllPhaseInferredSubsumers() {

			return NodeClassifier.resetAllPhaseInferredSubsumers(getAllNodes());
		}
	}

	OntologyClassifier(Ontology ontology, OntologyClassifyListener classifyListener) {

		this.ontology = ontology;
		this.classifyListener = classifyListener;

		classify();
	}

	boolean patternSubsumption(Pattern defn, Pattern candidate) {

		if (candidate.getProfileRelations().getAll().size() < 2) {

			return true;
		}

		return defn.subsumesRelations(candidate);
	}

	private void classify() {

		Phase phase = new Phase();

		while (phase.performPhase()) {

			phase = phase.createNextPhase();
		}

		completeClassification();
	}

	private void completeClassification() {

		classifyListener.onCompletionStart();

		NameClassification.completeAllClassifications(getAllNames());
	}

	private Iterable<Name> getAllNames() {

		return ontology.getAllNames();
	}

	private Iterable<NodeX> getAllNodes() {

		return ontology.getAllNodes();
	}
}
