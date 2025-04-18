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

import com.carrotsearch.hppc.*;
import com.carrotsearch.hppc.cursors.*;

import rekon.util.*;

/**
 * @author Colin Puleston
 */
abstract class PotentialSubsumptions<M extends NodeMatcher> {

	private List<M> allOptions = null;
	private Map<M, Integer> optionIdxs = new HashMap<M, Integer>();

	private List<RankMatches> allRankMatches = new ArrayList<RankMatches>();

	private enum UpdateType {REGISTER_CORE, REGISTER_TRANSIENT, DEREGISTER_TRANSIENT}

	private class RankMatches {

		private int rank = allRankMatches.size();

		private Map<Name, IntHashSet> optionIdxsByRefName = new HashMap<Name, IntHashSet>();
		private Set<Name> refNamesCommonToAllOptions = new HashSet<Name>();

		void update(M option, int optionIdx, Names rankNames, UpdateType updateType) {

			switch (updateType) {

				case REGISTER_CORE:

					registerOption(option, optionIdx, rankNames, true);
					break;

				case REGISTER_TRANSIENT:

					registerOption(option, optionIdx, rankNames, false);
					break;

				case DEREGISTER_TRANSIENT:

					deregisterOption(option, optionIdx, rankNames);
					break;
			}
		}

		IntegerCollector collectOptionsFor(Integer requestIdx, Names rankNames) {

			IntegerCollector rankOptions = createRankOptionsCollector();

			for (Name n : rankNames) {

				rankOptions.absorbFrom(getOptionsFor(requestIdx, n));

				if (rankOptions.emptyResult()) {

					break;
				}
			}

			return rankOptions;
		}

		private void registerOption(M option, int optionIdx, Names rankNames, boolean coreOption) {

			for (Name n : resolveRankNamesForRegistration(rankNames)) {

				registerOptionName(option, optionIdx, n, coreOption);
			}
		}

		private void deregisterOption(M option, int optionIdx, Names rankNames) {

			for (Name n : resolveRankNamesForRegistration(rankNames)) {

				deregisterOptionName(option, optionIdx, n);
			}
		}

		private Collection<Name> resolveRankNamesForRegistration(Names rankNames) {

			return resolveNamesForRegistration(rankNames, rank).getNames();
		}

		private void registerOptionName(M option, int optionIdx, Name n, boolean coreOption) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			IntHashSet optIdxs = optionIdxsByRefName.get(n);

			if (optIdxs == null) {

				optIdxs = new IntHashSet();

				optionIdxsByRefName.put(n, optIdxs);
			}
			else {

				if (coreOption && optIdxs.size() == lastOptionIdx()) {

					optionIdxsByRefName.remove(n);
					refNamesCommonToAllOptions.add(n);

					return;
				}
			}

			optIdxs.add(optionIdx);
		}

		private void deregisterOptionName(M option, int optionIdx, Name n) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			IntHashSet optIdxs = optionIdxsByRefName.get(n);

			if (optIdxs == null) {

				throw new Error("Referenced name not registered for option!");
			}

			optIdxs.remove(optionIdx);
		}

		private IntegerUnion getOptionsFor(Integer requestIdx, Name n) {

			IntegerUnion optionIdxs = new IntegerUnion();

			for (Name rn : resolveNamesForRetrieval(new NameSet(n), rank)) {

				collectDirectOptionsFor(requestIdx, rn, optionIdxs);

				if (optionIdxs.allOptionsResult()) {

					break;
				}
			}

			return optionIdxs;
		}

		private void collectDirectOptionsFor(Integer requestIdx, Name n, IntegerUnion optionIdxs) {

			if (refNamesCommonToAllOptions.contains(n)) {

				optionIdxs.absorbAllOptions();
			}
			else {

				IntHashSet optIdxs = optionIdxsByRefName.get(n);

				if (optIdxs != null) {

					if (requestIdx != null) {

						optionIdxs.absorbExcept(optIdxs, requestIdx);
					}
					else {

						optionIdxs.absorb(optIdxs);
					}
				}
			}
		}

		private IntegerCollector createRankOptionsCollector() {

			return unionRankOptionsForRetrieval()
						? new IntegerUnion()
						: new IntegerIntersection();
		}
	}

	private class UpdateOp {

		private int optionIdx;
		private M option;

		private List<Names> rankedNames;

		UpdateOp(M option, int optionIdx, int startRank, int stopRank) {

			this.optionIdx = optionIdx;
			this.option = option;

			rankedNames = getOptionMatchNames(option, startRank, stopRank);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_CORE;
		}

		int totalOptionRanks() {

			return rankedNames.size();
		}

		boolean performOp(int rank) {

			if (rankedNames.size() > rank) {

				RankMatches rankMatches = allRankMatches.get(rank);
				Names rankNames = rankedNames.get(rank);

				rankMatches.update(option, optionIdx, rankNames, getOpType());

				return true;
			}

			return false;
		}
	}

	private class CoreRegisterOp extends UpdateOp {

		CoreRegisterOp(M option, int optionIdx, int startRank, int stopRank) {

			super(option, optionIdx, startRank, stopRank);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_CORE;
		}
	}

	private abstract class TransientUpdateOp extends UpdateOp {

		TransientUpdateOp(M option, int optionIdx) {

			super(option, optionIdx, 0, allRankMatches.size());

			processForCurrentRankMatches();
		}

		private void processForCurrentRankMatches() {

			for (int rank = 0 ; rank < allRankMatches.size() ; rank++) {

				if (!performOp(rank)) {

					break;
				}
			}
		}
	}

	private class TransientRegisterOp extends TransientUpdateOp {

		TransientRegisterOp(M option) {

			super(option, lastOptionIdx());
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_TRANSIENT;
		}
	}

	private class TransientDeregisterOp extends TransientUpdateOp {

		TransientDeregisterOp(M option) {

			super(option, optionIdxs.get(option));
		}

		UpdateType getOpType() {

			return UpdateType.DEREGISTER_TRANSIENT;
		}
	}

	private class MultiOptionRegistrar extends MultiThreadProcessor<Names> {

		private int startRank;
		private int stopRank;

		private List<UpdateOp> registerOps = new ArrayList<UpdateOp>();
		private boolean completedMultiReg = true;

		protected void execThreadProcess(int totalThreads, int threadIndex) {

			registerRanks(totalThreads, threadIndex);
		}

		protected void execAllInSingleThread() {

			registerRanks(1, 0);
		}

		MultiOptionRegistrar(int startRank, int stopRank) {

			this.startRank = startRank;
			this.stopRank = stopRank;

			ensureRankMatches(stopRank);
			setMaxProcesses(stopRank - startRank);

			List<M> opts = allOptions;

			for (int i = 0 ; i < opts.size() ; i++) {

				registerOps.add(new CoreRegisterOp(opts.get(i), i, startRank, stopRank));
			}

			execProcesses();
		}

		boolean completedMultiReg() {

			return completedMultiReg;
		}

		private void registerRanks(int totalThreads, int threadIndex) {

			for (int r = startRank + threadIndex ; r < stopRank ; r += totalThreads) {

				registerRank(r);
			}
		}

		private void registerRank(int rank) {

			for (UpdateOp op : registerOps) {

				if (op.performOp(rank)) {

					completedMultiReg &= (op.totalOptionRanks() <= stopRank);
				}
			}
		}
	}

	void initialise(List<M> allOptions) {

		this.allOptions = allOptions;

		for (int i = 0 ; i < allOptions.size() ; i++) {

			optionIdxs.put(allOptions.get(i), i);
		}
	}

	boolean initialised() {

		return allOptions != null;
	}

	void registerSingleOptionRank() {

		new MultiOptionRegistrar(0, 1);
	}

	void registerDefaultNestedOptionRanks() {

		new MultiOptionRegistrar(0, 3);
	}

	boolean registerOptionRanks(int startRank, int stopRank) {

		return new MultiOptionRegistrar(startRank, stopRank).completedMultiReg();
	}

	void registerTransientOption(M option) {

		new TransientRegisterOp(option);
	}

	void deregisterTransientOption(M option) {

		new TransientDeregisterOp(option);
	}

	Collection<M> getPotentialsFor(M request) {

		List<Names> requestNames = getRequestMatchNames(request);

		if (requestNames.size() > allRankMatches.size()) {

			return Collections.emptySet();
		}

		IntegerIntersection optionsInsect = new IntegerIntersection();

		Integer requestIdx = optionIdxs.get(request);
		int rank = 0;

		for (Names rankNames : requestNames) {

			if (!rankNames.isEmpty()) {

				RankMatches matches = allRankMatches.get(rank);
				IntegerCollector rankOpts = matches.collectOptionsFor(requestIdx, rankNames);

				if (rankOpts.emptyResult()) {

					return Collections.emptySet();
				}

				rankOpts.absorbInto(optionsInsect);
			}

			rank++;
		}

		if (optionsInsect.allOptionsResult() || optionsInsect.emptySubsetResult()) {

			return allOptions;
		}

		return optionIdxsToOptions(optionsInsect.getSubsetResult());
	}

	abstract List<Names> getOptionMatchNames(M option, int startRank, int stopRank);

	abstract List<Names> getRequestMatchNames(M request);

	abstract Names resolveNamesForRegistration(Names names, int rank);

	abstract Names resolveNamesForRetrieval(Names names, int rank);

	abstract boolean unionRankOptionsForRetrieval();

	private Collection<M> optionIdxsToOptions(IntHashSet idxs) {

		List<M> opts = new ArrayList<M>();

		Iterator<IntCursor> i = idxs.iterator();

		while (i.hasNext()) {

			opts.add(allOptions.get(i.next().value));
		}

		return opts;
	}

	private void ensureRankMatches(int count) {

		for (int i = allRankMatches.size() ; i < count ; i++) {

			allRankMatches.add(new RankMatches());
		}
	}

	private int lastOptionIdx() {

		return allOptions.size() - 1;
	}
}
