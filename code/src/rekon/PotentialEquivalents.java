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

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
class PotentialEquivalents {

	Collection<MatchableNode> getPotentials(NameSet subsumeds) {

		List<MatchableNode> potentials = new ArrayList<MatchableNode>();

		for (Name s : subsumeds.getNames()) {

			findAllFrom((NodeName)s, potentials);
		}

		return potentials;
	}

	private void findAllFrom(NodeName n, List<MatchableNode> potentials) {

		findFrom(n, potentials);

		for (Name ss : n.getSubs(ClassName.class, false).getNames()) {

			if (ss instanceof NodeName) {

				findFrom((NodeName)ss, potentials);
			}
		}
	}

	private void findFrom(NodeName n, List<MatchableNode> potentials) {

		MatchableNode m = n.getMatchable();

		if (m != null && m.hasDefinitions() && potentials.add(m)) {

			findFrom(m, potentials);
		}
	}

	private void findFrom(MatchableNode m, List<MatchableNode> potentials) {

		for (NodePattern d : m.getDefinitions()) {

			for (Name n : getDefinitionMatchNames(d).getNames()) {

				if (n instanceof NodeName) {

					findFrom((NodeName)n, potentials);
				}
			}
		}
	}

	private Names getDefinitionMatchNames(NodePattern defn) {

		return NameCollector.definitionRequests.collectUnranked(defn);
	}
}
