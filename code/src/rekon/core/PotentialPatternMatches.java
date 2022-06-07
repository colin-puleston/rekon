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

		abstract boolean absorb(Set<O> options);

		abstract void absorbInto(OptionIntersection insect);
	}

	private class OptionIntersection extends OptionCollector {

		private List<Set<O>> optionSets = new ArrayList<Set<O>>();

		boolean absorb(Set<O> options) {

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

		Set<O> getIntersection() {

			return SetIntersector.intersect(optionSets);
		}
	}

	private class OptionUnion extends OptionCollector {

		private Set<O> union = Collections.emptySet();
		private int components = 0;

		boolean absorb(Set<O> options) {

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

		Set<O> getUnion() {

			return union;
		}
	}

	private class RankMatches {

		private Map<Name, Set<O>> optionsByName = new HashMap<Name, Set<O>>();
		private Set<Name> namesCommonToAllOptions = new HashSet<Name>();

		void registerOption(O option, Names rankNames) {

			for (Name n : resolveNamesForRegistration(rankNames).getNames()) {

				registerOptionName(option, n);
			}
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

		private void registerOptionName(O option, Name n) {

			if (namesCommonToAllOptions.contains(n)) {

				return;
			}

			Set<O> options = optionsByName.get(n);

			if (options == null) {

				options = new HashSet<O>();

				optionsByName.put(n, options);
			}
			else {

				if (options.size() == totalOptions() - 1) {

					optionsByName.remove(n);
					namesCommonToAllOptions.add(n);

					return;
				}
			}

			options.add(option);
		}

		private OptionUnion getOptionsFor(Name n) {

			OptionUnion options = new OptionUnion();

			for (Name rn : resolveNamesForRetrieval(new NameSet(n)).getNames()) {

				if (!collectDirectOptionsFor(rn, options)) {

					return null;
				}
			}

			return options;
		}

		private boolean collectDirectOptionsFor(Name n, OptionUnion options) {

			if (namesCommonToAllOptions.contains(n)) {

				return false;
			}

			Set<O> opts = optionsByName.get(n);

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

		private Collection<O> options;
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

			void checkRegister(int rank) {

				int regRanks = rankedNames.size();

				if (regRanks > rank) {

					allRankMatches.get(rank).registerOption(option, rankedNames.get(rank));

					completedReg &= (regRanks <= stopRank);
				}
			}
		}

		MultiOptionRegistrar(Collection<O> options) {

			this(options, 0, DEFAULT_RANKS);
		}

		MultiOptionRegistrar(Collection<O> options, int startRank, int stopRank) {

			this.options = options;
			this.startRank = startRank;
			this.stopRank = stopRank;

			ensureRankMatches(stopRank);
			setMaxProcesses(stopRank - startRank);

			for (O option : options) {

				optionRegs.add(new OptionReg(option));
			}

			execProcesses();
		}

		boolean completedReg() {

			return completedReg;
		}

		void execThreadProcess(int totalThreads, int threadIndex) {

			for (OptionReg optReg : optionRegs) {

				optReg.checkRegister(startRank + threadIndex);
			}
		}

		void execAllInSingleThread() {

			for (OptionReg optReg : optionRegs) {

				for (int rank = startRank ; rank < stopRank ; rank++) {

					optReg.checkRegister(rank);
				}
			}
		}
	}

	void registerOptions(Collection<O> options) {

		new MultiOptionRegistrar(options);
	}

	boolean registerOptionRanks(Collection<O> options, int startRank, int stopRank) {

		return new MultiOptionRegistrar(options, startRank, stopRank).completedReg();
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

	abstract int totalOptions();

	abstract List<Names> getOptionMatchNames(O option);

	abstract Names resolveNamesForRegistration(Names names);

	abstract Names resolveNamesForRetrieval(Names names);

	abstract boolean unionRankOptionsForRetrieval();

	private void ensureRankMatches(int count) {

		for (int i = allRankMatches.size() ; i < count ; i++) {

			allRankMatches.add(new RankMatches());
		}
	}
}