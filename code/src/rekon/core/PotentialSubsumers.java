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

	private SimpleDefPotentials simpleDefPotentials;
	private NestedDefPotentials nestedDefPotentials;

	private abstract class CategoryPotentials extends PotentialPatternMatches {

		CategoryPotentials(Collection<MatchableNode> categoryOptions) {

			super(categoryOptions);
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

		Collection<NodePattern> getOptionPatterns(MatchableNode node) {

			List<NodePattern> categoryDefns = new ArrayList<NodePattern>();

			for (NodePattern d : node.getDefinitions()) {

				if (d.nestedPattern() == nestedPatterns()) {

					categoryDefns.add(d);
				}
			}

			return categoryDefns;
		}

		abstract boolean nestedPatterns();
	}

	private class SimpleDefPotentials extends CategoryPotentials {

		SimpleDefPotentials(Collection<MatchableNode> categoryOptions) {

			super(categoryOptions);
		}

		List<Names> getOptionMatchNames(NodePattern pattern) {

			return Collections.singletonList(pattern.getNames());
		}

		List<Names> getRequestMatchNames(NodePattern pattern) {

			return Collections.singletonList(pattern.getNames());
		}

		boolean nestedPatterns() {

			return false;
		}
	}

	private class NestedDefPotentials extends CategoryPotentials {

		NestedDefPotentials(Collection<MatchableNode> categoryOptions) {

			super(categoryOptions);
		}

		List<Names> getOptionMatchNames(NodePattern pattern) {

			return NameCollector.definitionOptions.collectRanked(pattern);
		}

		List<Names> getRequestMatchNames(NodePattern pattern) {

			return NameCollector.signatureRequests.collectRanked(pattern);
		}

		boolean nestedPatterns() {

			return true;
		}
	}

	private class Initialiser {

		private List<MatchableNode> simpleDefOpts = new ArrayList<MatchableNode>();
		private List<MatchableNode> nestedDefOpts = new ArrayList<MatchableNode>();

		Initialiser(Collection<MatchableNode> allOptions) {

			processOptions(allOptions);

			simpleDefPotentials = new SimpleDefPotentials(simpleDefOpts);
			nestedDefPotentials = new NestedDefPotentials(nestedDefOpts);
		}

		private void processOptions(Collection<MatchableNode> allOptions) {

			for (MatchableNode o : allOptions) {

				processOption(o);
			}
		}

		private void processOption(MatchableNode o) {

			boolean simpleDef = false;
			boolean nestedDef = false;

			for (NodePattern d : o.getDefinitions()) {

				if (d.nestedPattern()) {

					if (!nestedDef) {

						nestedDefOpts.add(o);

						if (simpleDef) {

							break;
						}

						nestedDef = true;
					}
				}
				else {

					if (!simpleDef) {

						simpleDefOpts.add(o);

						if (nestedDef) {

							break;
						}

						simpleDef = true;
					}
				}
			}
		}
	}

	PotentialSubsumers(Collection<MatchableNode> allOptions) {

		new Initialiser(allOptions);
	}

	Collection<MatchableNode> getPotentialsFor(NodePattern request) {

		List<MatchableNode> allPotentials = new ArrayList<MatchableNode>();

		allPotentials.addAll(simpleDefPotentials.getPotentialsFor(request));
		allPotentials.addAll(nestedDefPotentials.getPotentialsFor(request));

		return allPotentials;
	}
}
