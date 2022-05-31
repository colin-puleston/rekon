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

				if (options.size() == optionsSize() - 1) {

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

	void registerOption(O option, List<Names> matchNamesByRank) {

		registerOptionRanks(option, matchNamesByRank, 0, -1);
	}

	void registerOptionRanks(O option, List<Names> matchNamesByRank, int start, int end) {

		for (int rank = start ; rank < matchNamesByRank.size() ; rank++) {

			Names rankNames = matchNamesByRank.get(rank);

			resolveRankMatches(rank).registerOption(option, rankNames);

			if (rank == end) {

				break;
			}
		}
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

	abstract int optionsSize();

	abstract Names resolveNamesForRegistration(Names names);

	abstract Names resolveNamesForRetrieval(Names names);

	abstract boolean unionRankOptionsForRetrieval();

	private RankMatches resolveRankMatches(int rank) {

		if (allRankMatches.size() == rank) {

			RankMatches rankMatches = new RankMatches();

			allRankMatches.add(rankMatches);

			return rankMatches;
		}

		return allRankMatches.get(rank);
	}
}