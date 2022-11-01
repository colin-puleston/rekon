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
class PotentialDynamicSubsumers {

	private Collection<MatchableNode> allMatchables;

	private SimplePotentials simplePotentials = new SimplePotentials();
	private NestedPotentials nestedPotentials = new NestedPotentials();

	private abstract class CategoryPotentials extends PotentialPatternMatches<NodeDefinition> {

		private List<NodeDefinition> categoryOptions = null;

		Collection<NodeDefinition> getPotentialsFor(NodePattern request) {

			checkInitialised();

			return getPotentialsFor(request, getRankedNames(request, false));
		}

		List<NodeDefinition> getAllOptions() {

			return categoryOptions;
		}

		List<Names> getOptionMatchNames(NodeDefinition option, int startRank, int stopRank) {

			return getRankedNames(option.getDefinition(), true);
		}

		Names resolveNamesForRegistration(Names names, int rank) {

			return names;
		}

		Names resolveNamesForRetrieval(Names names, int rank) {

			return ProfileMatchNames.resolve(names, PatternNameRole.rankToRole(rank));
		}

		boolean unionRankOptionsForRetrieval() {

			return true;
		}

		abstract void initialiseOptionRanks();

		abstract boolean nestedPatterns();

		abstract List<Names> getRankedNames(NodePattern p, boolean definition);

		private synchronized void checkInitialised() {

			if (categoryOptions == null) {

				categoryOptions = new ArrayList<NodeDefinition>();

				addCategoryOptions();
				initialiseOptionRanks();
			}
		}

		private void addCategoryOptions() {

			for (MatchableNode m : allMatchables) {

				for (NodePattern d : m.getDefinitions()) {

					if (d.nestedPattern(false) == nestedPatterns()) {

						categoryOptions.add(new NodeDefinition(m.getName(), d));
					}
				}
			}
		}
	}

	private class SimplePotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return false;
		}

		void initialiseOptionRanks() {

			registerSingleOptionRank();
		}

		List<Names> getRankedNames(NodePattern p, boolean definition) {

			return Collections.singletonList(p.getNames());
		}
	}

	private class NestedPotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return true;
		}

		void initialiseOptionRanks() {

			registerDefaultNestedOptionRanks();
		}

		List<Names> getRankedNames(NodePattern p, boolean definition) {

			return new NameCollector(definition, false).collectRanked(p);
		}
	}

	PotentialDynamicSubsumers(Collection<MatchableNode> allMatchables) {

		this.allMatchables = allMatchables;
	}

	Collection<NodeDefinition> getPotentialsFor(NodePattern request) {

		List<NodeDefinition> all = new ArrayList<NodeDefinition>();

		all.addAll(simplePotentials.getPotentialsFor(request));

		if (request.nestedPattern(true)) {

			all.addAll(nestedPotentials.getPotentialsFor(request));
		}

		return all;
	}
}
