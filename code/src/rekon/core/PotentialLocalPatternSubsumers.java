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

	private Collection<PatternMatcher> allDefinitionPatterns;

	private SimplePotentials simplePotentials = new SimplePotentials();
	private NestedPotentials nestedPotentials = new NestedPotentials();

	private abstract class CategoryPotentials
								extends
									PotentialSubsumptions<PatternMatcher> {

		private List<PatternMatcher> categoryOptions = null;

		void collectPotentialsFor(Pattern request, List<PatternMatcher> collector) {

			checkInitialised();

			collector.addAll(getPotentialsFor(getRankedProfileNames(request)));
		}

		List<PatternMatcher> getAllOptions() {

			return categoryOptions;
		}

		List<Names> getOptionMatchNames(PatternMatcher option, int startRank, int stopRank) {

			return getRankedDefinitionNames(option.getPattern());
		}

		Names resolveNamesForRegistration(Names names, int rank) {

			return names;
		}

		Names resolveNamesForRetrieval(Names names, int rank) {

			return MatchNamesResolver.expand(names, MatchRole.rankToPatternRole(rank));
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

				categoryOptions = new ArrayList<PatternMatcher>();

				addCategoryOptions();
				initialiseOptionRanks();
			}
		}

		private void addCategoryOptions() {

			for (PatternMatcher d : allDefinitionPatterns) {

				if (d.getPattern().nestedPattern(false) == nestedPatterns()) {

					categoryOptions.add(d);
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

		List<Names> getRankedDefinitionNames(Pattern p) {

			return new FilteringNameCollector(true).collect(p);
		}

		List<Names> getRankedProfileNames(Pattern p) {

			return new FilteringNameCollector(false).collect(p);
		}
	}

	PotentialLocalPatternSubsumers(Collection<PatternMatcher> allDefinitionPatterns) {

		this.allDefinitionPatterns = allDefinitionPatterns;
	}

	Collection<PatternMatcher> getPotentialsFor(PatternMatcher request) {

		List<PatternMatcher> all = new ArrayList<PatternMatcher>();
		Pattern pattern = request.getPattern();

		simplePotentials.collectPotentialsFor(pattern, all);

		if (pattern.nestedPattern(true)) {

			nestedPotentials.collectPotentialsFor(pattern, all);
		}

		return all;
	}

	Collection<PatternMatcher> getPotentialsFor(DisjunctionMatcher request) {

		List<Collection<PatternMatcher>> djPotentials = new ArrayList<Collection<PatternMatcher>>();

		for (Name dj : request.getDirectDisjuncts()) {

			djPotentials.add(getPotentialsForDisjunct((NodeX)dj));
		}

		return new SetIntersector<PatternMatcher>().intersectSets(djPotentials);
	}

	private Collection<PatternMatcher> getPotentialsForDisjunct(NodeX disjunct) {

		Set<PatternMatcher> potentials = new HashSet<PatternMatcher>();

		for (PatternMatcher pm : disjunct.getProfilePatternMatcherAsList()) {

			potentials.addAll(getPotentialsFor(pm));
		}

		return potentials;
	}
}
