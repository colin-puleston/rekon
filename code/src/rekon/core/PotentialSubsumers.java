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

	private SimpleDefPotentials simpleDefPotentials = new SimpleDefPotentials();
	private NestedDefPotentials nestedDefPotentials = new NestedDefPotentials();

	private abstract class CategoryPotentials extends PotentialPatternMatches<NodeDefinition> {

		private Collection<NodeDefinition> categoryDefs = null;

		Collection<NodeDefinition> getPotentialsFor(NodePattern profile) {

			checkInitialised();

			List<Names> profNames = getRankedProfileNames(profile);
			Collection<NodeDefinition> potentials = getPotentialsOrNull(profile, profNames);

			return potentials != null ? potentials : categoryDefs;
		}

		int totalOptions() {

			return categoryDefs.size();
		}

		List<Names> getOptionMatchNames(NodeDefinition option) {

			return getRankedDefinitionNames(option.getDefinition());
		}

		Names resolveNamesForRegistration(Names names) {

			return names;
		}

		Names resolveNamesForRetrieval(Names names) {

			return names.expandWithNonRootDefinitionSubsumers();
		}

		boolean unionRankOptionsForRetrieval() {

			return true;
		}

		abstract boolean nestedPatterns();

		abstract List<Names> getRankedDefinitionNames(NodePattern defn);

		abstract List<Names> getRankedProfileNames(NodePattern profile);

		private void checkInitialised() {

			if (categoryDefs == null) {

				categoryDefs = new ArrayList<NodeDefinition>();

				collectCategoryDefs();
				registerCategoryOptions();
			}
		}

		private void collectCategoryDefs() {

			for (MatchableNode m : matchables) {

				for (NodePattern d : m.getDefinitions()) {

					if (d.nestedPattern() == nestedPatterns()) {

						categoryDefs.add(new NodeDefinition(m.getName(), d));
					}
				}
			}
		}

		private void registerCategoryOptions() {

			registerOptions(categoryDefs);
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

			return new NameCollector(true).collectRanked(defn);
		}

		List<Names> getRankedProfileNames(NodePattern profile) {

			return new NameCollector(false).collectRanked(profile);
		}
	}

	PotentialSubsumers(Collection<MatchableNode> matchables) {

		this.matchables = matchables;
	}

	Collection<NodeDefinition> getPotentialsFor(NodePattern profile) {

		List<NodeDefinition> all = new ArrayList<NodeDefinition>();

		all.addAll(simpleDefPotentials.getPotentialsFor(profile));
		all.addAll(nestedDefPotentials.getPotentialsFor(profile));

		return all;
	}
}
