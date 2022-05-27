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
class PotentialSubsumers {

	private Collection<MatchableNode> matchables;

	private SimpleDefPotentials simpleDefPotentials;
	private NestedDefPotentials nestedDefPotentials;

	private abstract class CategoryPotentials extends PotentialPatternMatches<NodeDefinition> {

		private Collection<NodeDefinition> categoryDefs;

		CategoryPotentials() {

			categoryDefs = getCategoryDefs();

			for (NodeDefinition d : categoryDefs) {

				registerOption(d, getRankedDefinitionNames(d.getDefinition()));
			}
		}

		Collection<NodeDefinition> getPotentialsFor(NodePattern profile) {

			List<Names> profNames = getRankedProfileNames(profile);
			Collection<NodeDefinition> p = getPotentialsOrNull(profile, profNames);

			return p != null ? p : categoryDefs;
		}

		int allOptionsSize() {

			return categoryDefs.size();
		}

		Names resolveNamesForRegistration(Names names) {

			return names;
		}

		boolean expandNamesForRetrieval() {

			return true;
		}

		boolean unionRankOptionsForRetrieval() {

			return true;
		}

		abstract boolean nestedPatterns();

		abstract List<Names> getRankedDefinitionNames(NodePattern defn);

		abstract List<Names> getRankedProfileNames(NodePattern profile);

		private Collection<NodeDefinition> getCategoryDefs() {

			List<NodeDefinition> defs = new ArrayList<NodeDefinition>();

			for (MatchableNode m : matchables) {

				for (NodePattern d : m.getDefinitions()) {

					if (d.nestedPattern() == nestedPatterns()) {

						defs.add(new NodeDefinition(m.getName(), d));
					}
				}
			}

			return defs;
		}
	}

	private class SimpleDefPotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return false;
		}

		List<Names> getRankedDefinitionNames(NodePattern defn) {

			return Collections.singletonList(defn.getNames());
		}

		List<Names> getRankedProfileNames(NodePattern profile) {

			return Collections.singletonList(profile.getNames());
		}
	}

	private class NestedDefPotentials extends CategoryPotentials {

		boolean nestedPatterns() {

			return true;
		}

		List<Names> getRankedDefinitionNames(NodePattern defn) {

			return NameCollector.definitionOptions.collectRanked(defn);
		}

		List<Names> getRankedProfileNames(NodePattern profile) {

			return NameCollector.signatureRequests.collectRanked(profile);
		}
	}

	PotentialSubsumers(Collection<MatchableNode> matchables) {

		this.matchables = matchables;

		simpleDefPotentials = new SimpleDefPotentials();
		nestedDefPotentials = new NestedDefPotentials();
	}

	Collection<NodeDefinition> getPotentialsFor(NodePattern profile) {

		List<NodeDefinition> all = new ArrayList<NodeDefinition>();

		all.addAll(simpleDefPotentials.getPotentialsFor(profile));
		all.addAll(nestedDefPotentials.getPotentialsFor(profile));

		return all;
	}
}
