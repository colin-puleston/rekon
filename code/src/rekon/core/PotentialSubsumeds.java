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
	private Collection<MatchableNode> allOptions;

	private abstract class Config {

		abstract void initialise();

		abstract Names resolveNamesForRegistration(Names names);

		abstract Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn);

		abstract void configureNameCollector(NameCollector collector);
	}

	private class OntologyConfig extends Config {

		void initialise() {

			for (MatchableNode o : allOptions) {

				registerOption(o, getRankedProfileNames(o.getProfile()));
			}
		}

		Names resolveNamesForRegistration(Names names) {

			return names.expandWithNonRootDefinitionSubsumers();
		}

		Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn) {

			return getPotentialsOrNull(defn, getRankedDefinitionNames(defn));
		}

		void configureNameCollector(NameCollector collector) {
		}
	}

	private class DynamicConfig extends Config {

		private int nextOptionRegRank = 0;
		private boolean optionRegComplete = false;

		void initialise() {
		}

		Names resolveNamesForRegistration(Names names) {

			return names.expandWithNonRootSubsumers();
		}

		Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn) {

			List<Names> defnNames = getRankedDefinitionNames(defn);
			int endRank = defnNames.size() - 1;

			if (!optionRegComplete && endRank >= nextOptionRegRank) {

				registerOptionsRanks(nextOptionRegRank, endRank);

				nextOptionRegRank = endRank + 1;
			}

			return getPotentialsOrNull(defn, defnNames);
		}

		void configureNameCollector(NameCollector collector) {

			collector.setLinkedCollection();
		}

		private void registerOptionsRanks(int start, int end) {

			boolean incompleteReg = false;

			for (MatchableNode o : allOptions) {

				List<Names> rankedNames = getRankedProfileNames(o.getProfile());

				if (rankedNames.size() > start) {

					registerOptionRanks(o, rankedNames, start, end);

					if (!incompleteReg && rankedNames.size() > end + 1) {

						incompleteReg = true;
					}
				}
			}

			if (!incompleteReg) {

				optionRegComplete = true;
			}
		}
	}

	PotentialSubsumeds(Collection<MatchableNode> allOptions, boolean dynamic) {

		this.allOptions = allOptions;

		config = dynamic ? new DynamicConfig() : new OntologyConfig();

		config.initialise();
	}

	Collection<MatchableNode> getPotentialsFor(NodePattern defn) {

		Collection<MatchableNode> potentials = config.getPotentialsForOrNull(defn);

		return potentials != null ? potentials : allOptions;
	}

	int optionsSize() {

		return allOptions.size();
	}

	Names resolveNamesForRegistration(Names names) {

		return config.resolveNamesForRegistration(names);
	}

	Names resolveNamesForRetrieval(Names names) {

		return names;
	}

	boolean unionRankOptionsForRetrieval() {

		return false;
	}

	private List<Names> getRankedProfileNames(NodePattern profile) {

		return collectNames(NameCollector.signatureOptions, profile);
	}

	private List<Names> getRankedDefinitionNames(NodePattern defn) {

		return collectNames(NameCollector.definitionRequests, defn);
	}

	private List<Names> collectNames(NameCollector collector, NodePattern p) {

		config.configureNameCollector(collector);

		return collector.collectRanked(p);
	}
}
