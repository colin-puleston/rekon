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
class PotentialLocalSubsumers {

	private Collection<PatternMatcher> allDefinitions;

	private SimplePotentials simplePotentials = new SimplePotentials();
	private NestedPotentials nestedPotentials = new NestedPotentials();

	private abstract class CategoryPotentials extends PotentialSubsumptions {

		private boolean initialised = false;

		void collectPotentialsFor(PatternMatcher request, List<PatternMatcher> potentials) {

			checkInitialised();

			potentials.addAll(getPotentialsFor(request));
		}

		List<Names> getOptionMatchNames(Pattern option, int startRank, int stopRank) {

			return getRankedDefinitionNames(option);
		}

		List<Names> getRequestMatchNames(Pattern request) {

			return getRankedProfileNames(request);
		}

		Names resolveNamesForRegistration(Names names, int rank) {

			return names;
		}

		Names resolveNamesForRetrieval(Names names, int rank) {

			return MatchNamesResolver.resolve(names, rank);
		}

		boolean unionRankOptionsForRetrieval() {

			return true;
		}

		abstract void initialiseOptionRanks();

		abstract boolean nestedPatterns();

		abstract List<Names> getRankedDefinitionNames(Pattern defn);

		abstract List<Names> getRankedProfileNames(Pattern profile);

		private synchronized void checkInitialised() {

			if (!initialised) {

				setFixedOptions(getCategoryOptions());
				initialiseOptionRanks();

				initialised = true;
			}
		}

		private List<PatternMatcher> getCategoryOptions() {

			List<PatternMatcher> categoryOpts = new ArrayList<PatternMatcher>();

			for (PatternMatcher d : allDefinitions) {

				if (d.getPattern().nestedPattern(false) == nestedPatterns()) {

					categoryOpts.add(d);
				}
			}

			return categoryOpts;
		}
	}

	private class SimplePotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return false;
		}

		void initialiseOptionRanks() {

			registerSingleOptionRank();
		}

		List<Names> getRankedDefinitionNames(Pattern defn) {

			return getRankedNames(defn);
		}

		List<Names> getRankedProfileNames(Pattern profile) {

			return getRankedNames(profile);
		}

		private List<Names> getRankedNames(Pattern p) {

			return Collections.singletonList(p.getNodes());
		}
	}

	private class NestedPotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return true;
		}

		void initialiseOptionRanks() {

			registerDefaultNestedOptionRanks();
		}

		List<Names> getRankedDefinitionNames(Pattern defn) {

			return new DefaultDefinitionFilteringNameCollector().collect(defn);
		}

		List<Names> getRankedProfileNames(Pattern profile) {

			return new DefaultProfileFilteringNameCollector().collect(profile);
		}
	}

	PotentialLocalSubsumers(Collection<PatternMatcher> allDefinitions) {

		this.allDefinitions = allDefinitions;
	}

	Collection<PatternMatcher> getPotentialsFor(PatternMatcher request) {

		List<PatternMatcher> potentials = new ArrayList<PatternMatcher>();

		simplePotentials.collectPotentialsFor(request, potentials);

		if (request.getPattern().nestedPattern(true)) {

			nestedPotentials.collectPotentialsFor(request, potentials);
		}

		return potentials;
	}
}
