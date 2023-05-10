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
class PatternSubsumers {

	static private EquivCheckDefineds equivCheckDefineds = new EquivCheckDefineds();

	static private class EquivCheckDefineds {

		Collection<MatchableNode<?>> deriveFromSubsumeds(Names subsumeds) {

			Set<MatchableNode<?>> potentials = new HashSet<MatchableNode<?>>();

			for (Name s : subsumeds.getNames()) {

				findAllFrom((NodeName)s, potentials);
			}

			return potentials;
		}

		private void findAllFrom(NodeName n, Set<MatchableNode<?>> potentials) {

			findFrom(n, potentials);

			for (Name ss : n.getSubs(ClassName.class, false).getNames()) {

				findFrom((NodeName)ss, potentials);
			}
		}

		private void findFrom(NodeName n, Set<MatchableNode<?>> potentials) {

			PatternNode pn = n.getPatternNode();

			if (pn != null && pn.hasDefinitions() && potentials.add(pn)) {

				findFrom(pn, potentials);
			}

			potentials.addAll(n.getDisjunctionNodes());
		}

		private void findFrom(PatternNode pn, Set<MatchableNode<?>> potentials) {

			for (NodePattern d : pn.getDefinitions()) {

				for (Name n : getDefinitionMatchNames(d).getNames()) {

					if (n instanceof NodeName) {

						findFrom((NodeName)n, potentials);
					}
				}
			}
		}

		private Names getDefinitionMatchNames(NodePattern defn) {

			return new SimpleNameCollector(true).collect(defn);
		}
	}

	private LocalClassifier localClassifier;

	PatternSubsumers(MatchableNodes matchables) {

		localClassifier = new LocalClassifier(matchables);
	}

	NameSet inferSubsumers(LocalPattern pattern) {

		return localClassifier.classify(pattern);
	}

	NameSet inferSubsumersForSubsumeds(LocalPattern pattern, Names subsumeds) {

		return localClassifier.classify(
					pattern,
					equivCheckDefineds.deriveFromSubsumeds(subsumeds));
	}
}
