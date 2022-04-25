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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
abstract class PotentialNodeMatches {

	private Collection<MatchableNode> allOptions;
	private List<RankMatches> allRankMatches = new ArrayList<RankMatches>();

	private class OptionSet {

		private Set<MatchableNode> options = Collections.emptySet();
		private int components = 0;

		void addOptions(Set<MatchableNode> newOptions) {

			if (components == 0) {

				options = newOptions;
			}
			else {

				if (components == 1) {

					options = new HashSet<MatchableNode>(options);
				}

				options.addAll(newOptions);
			}

			components++;
		}

		Set<MatchableNode> getSet() {

			return options;
		}
	}

	private abstract class OptionCollector {

		abstract boolean addSet(Set<MatchableNode> options);

		abstract void addToCollected(List<Set<MatchableNode>> optionSets);
	}

	private class IntersectingOptionCollector extends OptionCollector {

		private List<Set<MatchableNode>> intersectionSets = new ArrayList<Set<MatchableNode>>();

		boolean addSet(Set<MatchableNode> set) {

			if (set.isEmpty()) {

				return false;
			}

			intersectionSets.add(set);

			return true;
		}

		void addToCollected(List<Set<MatchableNode>> collectedSets) {

			collectedSets.addAll(intersectionSets);
		}
	}

	private class UnioningOptionCollector extends OptionCollector {

		private OptionSet unionSet = new OptionSet();

		boolean addSet(Set<MatchableNode> set) {

			if (!set.isEmpty()) {

				unionSet.addOptions(set);
			}

			return true;
		}

		void addToCollected(List<Set<MatchableNode>> collectedSets) {

			collectedSets.add(unionSet.getSet());
		}
	}

	private class RankMatches {

		private Map<Name, Set<MatchableNode>> optionsByName = new HashMap<Name, Set<MatchableNode>>();
		private Set<Name> namesCommonToAllOptions = new HashSet<Name>();

		void registerOption(MatchableNode option, Names rankNames) {

			for (Name n : resolveRegistrationNames(rankNames).getNames()) {

				if (!ignoreRootNamesForRegistration() || !n.rootName()) {

					registerOptionName(option, n);
				}
			}
		}

		boolean collectOptionSetsFor(Names rankNames, List<Set<MatchableNode>> optionSets) {

			OptionCollector collector = createOptionCollector();

			for (Name n : rankNames.getNames()) {

				OptionSet options = getOptionSetFor(n);

				if (options != null && !collector.addSet(options.getSet())) {

					return false;
				}
			}

			collector.addToCollected(optionSets);

			return true;
		}

		private OptionCollector createOptionCollector() {

			return unionRankOptionsForRetrieval()
						? new UnioningOptionCollector()
						: new IntersectingOptionCollector();
		}

		private OptionSet getOptionSetFor(Name n) {

			OptionSet options = new OptionSet();

			for (Name nr : resolveRetrievalNames(n).getNames()) {

				if (namesCommonToAllOptions.contains(nr)) {

					return null;
				}

				Set<MatchableNode> opts = optionsByName.get(nr);

				if (opts != null) {

					options.addOptions(opts);
				}
			}

			return options;
		}

		private void registerOptionName(MatchableNode option, Name n) {

			Set<MatchableNode> options = optionsByName.get(n);

			if (options == null) {

				options = new HashSet<MatchableNode>();

				optionsByName.put(n, options);
			}
			else {

				if (options.size() == allOptions.size() - 1) {

					optionsByName.remove(n);
					namesCommonToAllOptions.add(n);
				}
			}

			options.add(option);
		}
	}

	PotentialNodeMatches(Collection<MatchableNode> allOptions) {

		this.allOptions = allOptions;

		for (MatchableNode o : allOptions) {

			registerOption(o);
		}
	}

	Collection<MatchableNode> getPotentialsFor(NodePattern request) {

		List<Names> namesByRank = getRequestMatchNames(request);

		if (namesByRank.size() > allRankMatches.size()) {

			return Collections.emptySet();
		}

		List<Set<MatchableNode>> optionSets = new ArrayList<Set<MatchableNode>>();
		int rank = 0;

		for (Names rankNames : namesByRank) {

			if (!rankNames.isEmpty()) {

				RankMatches matches = allRankMatches.get(rank);

				if (!matches.collectOptionSetsFor(rankNames, optionSets)) {

					return Collections.emptySet();
				}
			}

			rank++;
		}

		return optionSets.isEmpty() ? allOptions : SetIntersector.intersect(optionSets);
	}

	abstract boolean ignoreRootNamesForRegistration();

	abstract boolean expandNamesForRegistration();

	abstract boolean expandNamesForRetrieval();

	abstract boolean unionRankOptionsForRetrieval();

	abstract Collection<NodePattern> getOptionPatterns(MatchableNode node);

	abstract List<Names> getOptionMatchNames(NodePattern pattern);

	abstract List<Names> getRequestMatchNames(NodePattern pattern);

	private void registerOption(MatchableNode option) {

		for (NodePattern p : getOptionPatterns(option)) {

			int rank = 0;

			for (Names rankNames : getOptionMatchNames(p)) {

				resolveRankMatches(rank++).registerOption(option, rankNames);
			}
		}
	}

	private RankMatches resolveRankMatches(int rank) {

		if (allRankMatches.size() == rank) {

			RankMatches rankMatches = new RankMatches();

			allRankMatches.add(rankMatches);

			return rankMatches;
		}

		return allRankMatches.get(rank);
	}

	private Names resolveRegistrationNames(Names names) {

		return expandNamesForRegistration() ? names.expandWithSubsumers() : names;
	}

	private Names resolveRetrievalNames(Name name) {

		Names names = new NameSet(name);

		return expandNamesForRetrieval() ? names.expandWithSubsumers() : names;
	}
}