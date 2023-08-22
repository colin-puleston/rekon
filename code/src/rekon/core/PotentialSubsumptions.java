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

/**
 * @author Colin Puleston
 */
abstract class PotentialSubsumptions<O> {

	private List<RankMatches> allRankMatches = new ArrayList<RankMatches>();

	private enum UpdateType {REGISTER_CORE, REGISTER_TRANSIENT, DEREGISTER_TRANSIENT}

	private class RankMatches {

		private int rank = allRankMatches.size();

		private Map<Name, IntHashSet> optionIdxsByRefName = new HashMap<Name, IntHashSet>();
		private Set<Name> refNamesCommonToAllOptions = new HashSet<Name>();

		void update(int index, O option, Names rankNames, UpdateType updateType) {

			for (Name n : resolveRankNamesForRegistration(rankNames)) {

				if (updateType == UpdateType.DEREGISTER_TRANSIENT) {

					deregisterOptionName(index, option, n);
				}
				else {

					registerOptionName(index, option, n, updateType);
				}
			}
		}

		IntegerCollector collectOptionsFor(Names rankNames) {

			IntegerCollector rankOptions = createRankOptionsCollector();

			for (Name n : rankNames.getNames()) {

				IntegerUnion options = getOptionsFor(n);

				if (options != null && !rankOptions.absorb(options.getUnion())) {

					return null;
				}
			}

			return rankOptions;
		}

		private Collection<Name> resolveRankNamesForRegistration(Names rankNames) {

			return resolveNamesForRegistration(rankNames, rank).getNames();
		}

		private void registerOptionName(int index, O option, Name n, UpdateType updateType) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			IntHashSet optIdxs = optionIdxsByRefName.get(n);

			if (optIdxs == null) {

				optIdxs = new IntHashSet();

				optionIdxsByRefName.put(n, optIdxs);
			}
			else {

				if (updateType == UpdateType.REGISTER_CORE
					&& optIdxs.size() == lastOptionIdx()) {

					optionIdxsByRefName.remove(n);
					refNamesCommonToAllOptions.add(n);

					return;
				}
			}

			optIdxs.add(index);
		}

		private void deregisterOptionName(int index, O option, Name n) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			IntHashSet optIdxs = optionIdxsByRefName.get(n);

			if (optIdxs == null) {

				throw new Error("Referenced name not registered for option!");
			}

			optIdxs.remove(index);
		}

		private IntegerUnion getOptionsFor(Name n) {

			IntegerUnion optionIdxs = new IntegerUnion();

			for (Name rn : resolveNamesForRetrieval(new NameSet(n), rank).getNames()) {

				if (!collectDirectOptionsFor(rn, optionIdxs)) {

					return null;
				}
			}

			return optionIdxs;
		}

		private boolean collectDirectOptionsFor(Name n, IntegerUnion optionIdxs) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return false;
			}

			IntHashSet optIdxs = optionIdxsByRefName.get(n);

			if (optIdxs != null) {

				optionIdxs.absorb(optIdxs);
			}

			return true;
		}

		private IntegerCollector createRankOptionsCollector() {

			return unionRankOptionsForRetrieval()
						? new IntegerUnion()
						: new IntegerIntersection();
		}
	}

	private class UpdateOp {

		private int index;
		private O option;

		private List<Names> rankedNames;

		UpdateOp(int index, O option, int startRank, int stopRank) {

			this.index = index;
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

				if (!rootCollected(rankNames)) {

					rankMatches.update(index, option, rankNames, getOpType());
				}

				return true;
			}

			return false;
		}
	}

	private class CoreRegisterOp extends UpdateOp {

		CoreRegisterOp(int index, O option, int startRank, int stopRank) {

			super(index, option, startRank, stopRank);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_CORE;
		}
	}

	private abstract class TransientUpdateOp extends UpdateOp {

		TransientUpdateOp(int index, O option) {

			super(index, option, 0, allRankMatches.size());

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

		TransientRegisterOp(O option) {

			super(lastOptionIdx(), option);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_TRANSIENT;
		}
	}

	private class TransientDeregisterOp extends TransientUpdateOp {

		TransientDeregisterOp(O option) {

			super(getAllOptions().indexOf(option), option);
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

		MultiOptionRegistrar(int startRank, int stopRank) {

			this.startRank = startRank;
			this.stopRank = stopRank;

			ensureRankMatches(stopRank);
			setMaxProcesses(stopRank - startRank);

			List<O> opts = getAllOptions();

			for (int i = 0 ; i < opts.size() ; i++) {

				registerOps.add(new CoreRegisterOp(i, opts.get(i), startRank, stopRank));
			}

			execProcesses();
		}

		boolean completedMultiReg() {

			return completedMultiReg;
		}

		void execThreadProcess(int totalThreads, int threadIndex) {

			registerRanks(totalThreads, threadIndex);
		}

		void execAllInSingleThread() {

			registerRanks(1, 0);
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

	void registerSingleOptionRank() {

		new MultiOptionRegistrar(0, 1);
	}

	void registerDefaultNestedOptionRanks() {

		new MultiOptionRegistrar(0, 3);
	}

	boolean registerOptionRanks(int startRank, int stopRank) {

		return new MultiOptionRegistrar(startRank, stopRank).completedMultiReg();
	}

	void registerTransientOption(O option) {

		new TransientRegisterOp(option);
	}

	void deregisterTransientOption(O option) {

		new TransientDeregisterOp(option);
	}

	abstract List<O> getAllOptions();

	abstract List<Names> getOptionMatchNames(O option, int startRank, int stopRank);

	abstract Names resolveNamesForRegistration(Names names, int rank);

	abstract Names resolveNamesForRetrieval(Names names, int rank);

	abstract boolean unionRankOptionsForRetrieval();

	Collection<O> getPotentialsFor(List<Names> namesByRank) {

		if (namesByRank.size() > allRankMatches.size()) {

			return Collections.emptySet();
		}

		IntegerIntersection optionsInsect = new IntegerIntersection();
		int rank = 0;

		for (Names rankNames : namesByRank) {

			if (!rootCollected(rankNames)) {

				RankMatches matches = allRankMatches.get(rank);
				IntegerCollector rankOptions = matches.collectOptionsFor(rankNames);

				if (rankOptions == null) {

					return Collections.emptySet();
				}

				rankOptions.absorbInto(optionsInsect);
			}

			rank++;
		}

		if (optionsInsect.anyComponents()) {

			return optionIdxsToOptions(optionsInsect.getIntersection());
		}

		return getAllOptions();
	}

	private Collection<O> optionIdxsToOptions(IntHashSet idxs) {

		List<O> opts = new ArrayList<O>();
		List<O> allOpts = getAllOptions();

		Iterator<IntCursor> i = idxs.iterator();

		while (i.hasNext()) {

			opts.add(allOpts.get(i.next().value));
		}

		return opts;
	}

	private void ensureRankMatches(int count) {

		for (int i = allRankMatches.size() ; i < count ; i++) {

			allRankMatches.add(new RankMatches());
		}
	}

	private int lastOptionIdx() {

		return getAllOptions().size() - 1;
	}

	private boolean rootCollected(Names rankNames) {

		return FilteringNameCollector.rootCollected(rankNames);
	}
}
