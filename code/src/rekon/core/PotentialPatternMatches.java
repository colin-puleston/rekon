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
abstract class PotentialPatternMatches<O> {

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

	private class RankMatches {

		private int rank = allRankMatches.size();

		private Map<Name, Set<O>> optionsByRefName = new HashMap<Name, Set<O>>();
		private Set<Name> refNamesCommonToAllOptions = new HashSet<Name>();

		void registerOption(O option, Names rankNames) {

			for (Name n : resolveNamesForRegistration(rankNames, rank).getNames()) {

				registerOptionName(option, n);
			}
		}

		private void registerOptionName(O option, Name n) {

			if (refNamesCommonToAllOptions.contains(n)) {

				return;
			}

			Set<O> options = optionsByRefName.get(n);

			if (options == null) {

				options = new HashSet<O>();

				optionsByRefName.put(n, options);
			}
			else {

				if (options.size() == totalOptions() - 1) {

					optionsByRefName.remove(n);
					refNamesCommonToAllOptions.add(n);

					return;
				}
			}

			options.add(option);
		}

		OptionCollector collectOptionsFor(Names rankNames) {

			OptionCollector rankOptions = createOptionCollector();

			for (Name n : rankNames.getNames()) {

				OptionUnion options = getOptionsFor(n);

				if (options != null && !rankOptions.absorb(options.getUnion())) {

					return null;
				}
			}

			return rankOptions;
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

		private OptionCollector createOptionCollector() {

			return unionRankOptionsForRetrieval()
						? new OptionUnion()
						: new OptionIntersection();
		}
	}

	private class MultiOptionRegistrar extends MultiThreadProcessor<Names> {

		static private final int DEFAULT_RANKS = 3;

		private int startRank;
		private int stopRank;

		private List<OptionReg> optionRegs = new ArrayList<OptionReg>();
		private boolean completedReg = true;

		private class OptionReg {

			private O option;
			private List<Names> rankedNames;

			OptionReg(O option) {

				this.option = option;

				rankedNames = getOptionMatchNames(option);
			}

			void checkRegister(RankMatches rankMatches, int rank) {

				int regRanks = rankedNames.size();

				if (regRanks > rank) {

					rankMatches.registerOption(option, rankedNames.get(rank));

					completedReg &= (regRanks <= stopRank);
				}
			}
		}

		MultiOptionRegistrar() {

			this(0, DEFAULT_RANKS);
		}

		MultiOptionRegistrar(int startRank, int stopRank) {

			this.startRank = startRank;
			this.stopRank = stopRank;

			ensureRankMatches(stopRank);
			setMaxProcesses(stopRank - startRank);

			for (O option : getAllOptions()) {

				optionRegs.add(new OptionReg(option));
			}

			execProcesses();
		}

		boolean completedReg() {

			return completedReg;
		}

		void execThreadProcess(int totalThreads, int threadIndex) {

			registerRanks(totalThreads, threadIndex);
		}

		void execAllInSingleThread() {

			registerRanks(1, 0);
		}

		private void registerRanks(int totalThreads, int threadIndex) {

			for (int rank = startRank + threadIndex ; rank < stopRank ; rank += totalThreads) {

				registerRank(rank);
			}
		}

		private void registerRank(int rank) {

			RankMatches rankMatches = allRankMatches.get(rank);

			for (OptionReg optReg : optionRegs) {

				optReg.checkRegister(rankMatches, rank);
			}
		}
	}

	void registerAllOptionRanks() {

		new MultiOptionRegistrar();
	}

	boolean registerOptionRanks(int startRank, int stopRank) {

		return new MultiOptionRegistrar(startRank, stopRank).completedReg();
	}

	Collection<O> getPotentialsOrNull(NodePattern request, List<Names> namesByRank) {

		if (namesByRank.size() > allRankMatches.size()) {

			return Collections.emptySet();
		}

		OptionIntersection optionsInsect = new OptionIntersection();
		int rank = 0;

		for (Names rankNames : namesByRank) {

			if (!rankNames.isEmpty()) {

				RankMatches matches = allRankMatches.get(rank);
				OptionCollector rankOptions = matches.collectOptionsFor(rankNames);

				if (rankOptions == null) {

					return Collections.emptySet();
				}

				rankOptions.absorbInto(optionsInsect);
			}

			rank++;
		}

		return optionsInsect.anyComponents() ? optionsInsect.getIntersection() : null;
	}

	abstract List<O> getAllOptions();

	abstract List<Names> getOptionMatchNames(O option);

	abstract Names resolveNamesForRegistration(Names names, int rank);

	abstract Names resolveNamesForRetrieval(Names names, int rank);

	abstract boolean unionRankOptionsForRetrieval();

	private void ensureRankMatches(int count) {

		for (int i = allRankMatches.size() ; i < count ; i++) {

			allRankMatches.add(new RankMatches());
		}
	}

	private int totalOptions() {

		return getAllOptions().size();
	}
}