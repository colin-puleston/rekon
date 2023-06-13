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
abstract class PotentialSubsumptions<O> {

	private List<RankMatches> allRankMatches = new ArrayList<RankMatches>();

	private abstract class OptionCollector {

		abstract boolean absorb(Collection<O> options);

		abstract void absorbInto(OptionIntersection insect);
	}

	private class OptionIntersection extends OptionCollector {

		private List<Collection<O>> optionSets = new ArrayList<Collection<O>>();

		boolean absorb(Collection<O> options) {

			if (options.isEmpty()) {

				return false;
			}

			optionSets.add(options);

			return true;
		}

		void absorbInto(OptionIntersection insect) {

			insect.optionSets.addAll(optionSets);
		}

		boolean anyComponents() {

			return !optionSets.isEmpty();
		}

		Collection<O> getIntersection() {

			return SetIntersector.intersect(optionSets);
		}
	}

	private class OptionUnion extends OptionCollector {

		private Collection<O> union = Collections.emptySet();
		private int components = 0;

		boolean absorb(Collection<O> options) {

			if (!options.isEmpty()) {

				if (components == 0) {

					union = options;
				}
				else {

					if (components == 1) {

						union = new HashSet<O>(union);
					}

					union.addAll(options);
				}

				components++;
			}

			return true;
		}

		void absorbInto(OptionIntersection insect) {

			insect.absorb(union);
		}

		Collection<O> getUnion() {

			return union;
		}
	}

	private enum UpdateType {REGISTER_CORE, REGISTER_TRANSIENT, DEREGISTER_TRANSIENT}

	private class RankMatches {

		private int rank = allRankMatches.size();

		private Map<Name, Set<O>> optionsByRefName = new HashMap<Name, Set<O>>();
		private Set<Name> refNamesCommonToAllOptions = new HashSet<Name>();

		void update(O option, Names rankNames, UpdateType updateType) {

			for (Name n : resolveRankNamesForRegistration(rankNames)) {

				if (updateType == UpdateType.DEREGISTER_TRANSIENT) {

					deregisterOptionName(option, n);
				}
				else {

					registerOptionName(option, n, updateType);
				}
			}
		}

		OptionCollector collectOptionsFor(Names rankNames) {

			OptionCollector rankOptions = createRankOptionsCollector();

			for (Name n : rankNames.getNames()) {

				OptionUnion options = getOptionsFor(n);

				if (options != null && !rankOptions.absorb(options.getUnion())) {

					return null;
				}
			}

			return rankOptions;
		}

		private Collection<Name> resolveRankNamesForRegistration(Names rankNames) {

			return resolveNamesForRegistration(rankNames, rank).getNames();
		}

		private void registerOptionName(O option, Name n, UpdateType updateType) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			Set<O> options = optionsByRefName.get(n);

			if (options == null) {

				options = new HashSet<O>();

				optionsByRefName.put(n, options);
			}
			else {

				if (updateType == UpdateType.REGISTER_CORE
					&& options.size() == totalOptions() - 1) {

					optionsByRefName.remove(n);
					refNamesCommonToAllOptions.add(n);

					return;
				}
			}

			options.add(option);
		}

		private void deregisterOptionName(O option, Name n) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			Set<O> options = optionsByRefName.get(n);

			if (options == null) {

				throw new Error("Referenced name not registered for option!");
			}

			options.remove(option);
		}

		private OptionUnion getOptionsFor(Name n) {

			OptionUnion options = new OptionUnion();

			for (Name rn : resolveNamesForRetrieval(new NameSet(n), rank).getNames()) {

				if (!collectDirectOptionsFor(rn, options)) {

					return null;
				}
			}

			return options;
		}

		private boolean collectDirectOptionsFor(Name n, OptionUnion options) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return false;
			}

			Set<O> opts = optionsByRefName.get(n);

			if (opts != null) {

				options.absorb(opts);
			}

			return true;
		}

		private OptionCollector createRankOptionsCollector() {

			return unionRankOptionsForRetrieval()
						? new OptionUnion()
						: new OptionIntersection();
		}
	}

	private class UpdateOp {

		private O option;
		private List<Names> rankedNames;

		UpdateOp(O option, int startRank, int stopRank) {

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

				rankMatches.update(option, rankNames, getOpType());

				return true;
			}

			return false;
		}
	}

	private class CoreRegisterOp extends UpdateOp {

		CoreRegisterOp(O option, int startRank, int stopRank) {

			super(option, startRank, stopRank);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_CORE;
		}
	}

	private abstract class TransientUpdateOp extends UpdateOp {

		TransientUpdateOp(O option) {

			super(option, 0, allRankMatches.size());

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

			super(option);
		}

		UpdateType getOpType() {

			return UpdateType.REGISTER_TRANSIENT;
		}
	}

	private class TransientDeregisterOp extends TransientUpdateOp {

		TransientDeregisterOp(O option) {

			super(option);
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

			for (O option : getAllOptions()) {

				registerOps.add(new CoreRegisterOp(option, startRank, stopRank));
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

		OptionIntersection optionsInsect = new OptionIntersection();
		int rank = 0;

		for (Names rankNames : namesByRank) {

			if (!rootNameRank(rankNames)) {

				RankMatches matches = allRankMatches.get(rank);
				OptionCollector rankOptions = matches.collectOptionsFor(rankNames);

				if (rankOptions == null) {

					return Collections.emptySet();
				}

				rankOptions.absorbInto(optionsInsect);
			}

			rank++;
		}

		return optionsInsect.anyComponents()
				? optionsInsect.getIntersection()
				: getAllOptions();
	}

	private boolean rootNameRank(Names rankNames) {

		return rankNames.size() == 1 && rankNames.getFirstName().rootName();
	}

	private void ensureRankMatches(int count) {

		for (int i = allRankMatches.size() ; i < count ; i++) {

			allRankMatches.add(new RankMatches());
		}
	}

	private int totalOptions() {

		return getAllOptions().size();
	}
}