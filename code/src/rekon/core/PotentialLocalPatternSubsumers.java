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
class PotentialLocalPatternSubsumers {

	private Collection<PatternNode> allPatternNodes;

	private SimplePotentials simplePotentials = new SimplePotentials();
	private NestedPotentials nestedPotentials = new NestedPotentials();

	private abstract class CategoryPotentials
								extends
									PotentialSubsumptions<DefinitionPattern> {

		private List<DefinitionPattern> categoryOptions = null;

		Collection<DefinitionPattern> getPotentialsFor(Pattern request) {

			checkInitialised();

			return getPotentialsFor(getRankedProfileNames(request));
		}

		List<DefinitionPattern> getAllOptions() {

			return categoryOptions;
		}

		List<Names> getOptionMatchNames(DefinitionPattern option, int startRank, int stopRank) {

			return getRankedDefinitionNames(option.getDefinition());
		}

		Names resolveNamesForRegistration(Names names, int rank) {

			return names;
		}

		Names resolveNamesForRetrieval(Names names, int rank) {

			return MatchNamesExpander.expand(names, MatchRole.rankToPatternRole(rank));
		}

		boolean unionRankOptionsForRetrieval() {

			return true;
		}

		abstract void initialiseOptionRanks();

		abstract boolean nestedPatterns();

		abstract List<Names> getRankedDefinitionNames(Pattern p);

		abstract List<Names> getRankedProfileNames(Pattern p);

		private synchronized void checkInitialised() {

			if (categoryOptions == null) {

				categoryOptions = new ArrayList<DefinitionPattern>();

				addCategoryOptions();
				initialiseOptionRanks();
			}
		}

		private void addCategoryOptions() {

			for (PatternNode n : allPatternNodes) {

				for (Pattern d : n.getDefinitions()) {

					if (d.nestedPattern(false) == nestedPatterns()) {

						categoryOptions.add(new DefinitionPattern(n.getName(), d));
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

		List<Names> getRankedDefinitionNames(Pattern p) {

			return getRankedNames(p);
		}

		List<Names> getRankedProfileNames(Pattern p) {

			return getRankedNames(p);
		}

		private List<Names> getRankedNames(Pattern p) {

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

		List<Names> getRankedDefinitionNames(Pattern p) {

			return new FilteringNameCollector(true).collect(p);
		}

		List<Names> getRankedProfileNames(Pattern p) {

			return new FilteringNameCollector(false).collect(p);
		}
	}

	PotentialLocalPatternSubsumers(Collection<PatternNode> allPatternNodes) {

		this.allPatternNodes = allPatternNodes;
	}

	Collection<DefinitionPattern> getPotentialsFor(Pattern request) {

		List<DefinitionPattern> all = new ArrayList<DefinitionPattern>();

		all.addAll(simplePotentials.getPotentialsFor(request));

		if (request.nestedPattern(true)) {

			all.addAll(nestedPotentials.getPotentialsFor(request));
		}

		return all;
	}
}
