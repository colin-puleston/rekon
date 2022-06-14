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

		abstract Names resolveNamesForRegistration(Names names);

		abstract Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn);

		abstract void configureNameCollector(NameCollector collector);
	}

	private class OntologyConfig extends Config {

		OntologyConfig() {

			registerAllOptionRanks();
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

		Names resolveNamesForRegistration(Names names) {

			return names.expandWithNonRootSubsumers();
		}

		Collection<MatchableNode> getPotentialsForOrNull(NodePattern defn) {

			List<Names> defnNames = getRankedDefinitionNames(defn);
			int stopRank = defnNames.size();

			if (!optionRegComplete && stopRank > nextOptionRegRank) {

				registerOptionRanks(nextOptionRegRank, stopRank);

				nextOptionRegRank = stopRank;
			}

			return getPotentialsOrNull(defn, defnNames);
		}

		void configureNameCollector(NameCollector collector) {

			collector.setLinkedCollection();
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

	List<Names> getOptionMatchNames(MatchableNode option) {

		return collectNames(false, option.getProfile());
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

	private List<Names> getRankedDefinitionNames(NodePattern defn) {

		return collectNames(true, defn);
	}

	private List<Names> collectNames(boolean definition, NodePattern p) {

		NameCollector collector = new NameCollector(definition);

		config.configureNameCollector(collector);

		return collector.collectRanked(p);
	}
}
