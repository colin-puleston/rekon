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
class PotentialSubsumeds extends PotentialPatternMatches<MatchableNode> {

	private Config config;
	private List<MatchableNode> allOptions;

	private abstract class Config {

		Config() {

			config = this;
		}

		abstract Names resolveNamesForRegistration(Names names, int rank);

		abstract List<Names> getOptionMatchNames(NodePattern profile, int startRank, int stopRank);

		abstract Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn);
	}

	private class OntologyConfig extends Config {

		OntologyConfig() {

			registerAllOptionRanks();
		}

		Names resolveNamesForRegistration(Names names, int rank) {

			return names.expandWithNonRootDefnSubsumers(PatternNameRole.rankToRole(rank));
		}

		List<Names> getOptionMatchNames(NodePattern profile, int startRank, int stopRank) {

			return collectRankedNames(profile, false);
		}

		Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn) {

			return getPotentialsOrNull(defn, collectRankedNames(defn, true));
		}

		private List<Names> collectRankedNames(NodePattern p, boolean definition) {

			return new NameCollector(definition, false).collectRanked(p);
		}
	}

	private class DynamicConfig extends Config {

		private int nextOptionRegRank = 0;
		private boolean optionRegComplete = false;

		Names resolveNamesForRegistration(Names names, int rank) {

			return names.expandWithNonRootSubsumers();
		}

		Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn) {

			List<Names> defnNames = new NameCollector(true, true).collectRanked(defn);

			int stopRank = defnNames.size();

			if (!optionRegComplete && stopRank > nextOptionRegRank) {

				registerOptionRanks(nextOptionRegRank, stopRank);

				nextOptionRegRank = stopRank;
			}

			return getPotentialsOrNull(defn, defnNames);
		}

		List<Names> getOptionMatchNames(NodePattern profile, int startRank, int stopRank) {

			return new NameCollector(false, true).collectRanked(profile, startRank, stopRank);
		}
	}

	PotentialSubsumeds(List<MatchableNode> allOptions, boolean dynamic) {

		this.allOptions = allOptions;

		if (dynamic) {

			new DynamicConfig();
		}
		else {

			new OntologyConfig();
		}
	}

	Collection<MatchableNode> getPotentialsFor(NodePattern defn) {

		Collection<MatchableNode> potentials = config.getPotentialsForOrNull(defn);

		return potentials != null ? potentials : allOptions;
	}

	List<MatchableNode> getAllOptions() {

		return allOptions;
	}

	List<Names> getOptionMatchNames(MatchableNode option, int startRank, int stopRank) {

		return config.getOptionMatchNames(option.getProfile(), startRank, stopRank);
	}

	Names resolveNamesForRegistration(Names names, int rank) {

		return config.resolveNamesForRegistration(names, rank);
	}

	Names resolveNamesForRetrieval(Names names, int rank) {

		return names;
	}

	boolean unionRankOptionsForRetrieval() {

		return false;
	}
}
