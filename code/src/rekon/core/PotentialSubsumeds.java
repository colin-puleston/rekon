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

	private Collection<MatchableNode> allOptions;
	private boolean dynamic;

	PotentialSubsumeds(Collection<MatchableNode> allOptions, boolean dynamic) {

		this.allOptions = allOptions;
		this.dynamic = dynamic;

		for (MatchableNode o : allOptions) {

			registerOption(o, getRankedProfileNames(o.getProfile()));
		}
	}

	Collection<MatchableNode> getPotentialsFor(NodePattern defn) {

		List<Names> defnNames = getRankedDefinitionNames(defn);
		Collection<MatchableNode> p = getPotentialsOrNull(defn, defnNames);

		return p != null ? p : allOptions;
	}

	int allOptionsSize() {

		return allOptions.size();
	}

	Names resolveNamesForRegistration(Names names) {

		if (dynamic) {

			return names.expandWithNonRootSubsumers();
		}

		return names.expandWithNonRootDefinitionSubsumers();
	}

	boolean expandNamesForRetrieval() {

		return false;
	}

	boolean unionRankOptionsForRetrieval() {

		return false;
	}

	private List<Names> getRankedProfileNames(NodePattern profile) {

		if (dynamic) {

			return NameCollector.signatureOptionsExtendedMatch.collectRanked(profile);
		}

		return NameCollector.signatureOptions.collectRanked(profile);
	}

	private List<Names> getRankedDefinitionNames(NodePattern defn) {

		if (dynamic) {

			return NameCollector.definitionRequestsExtendedMatch.collectRanked(defn);
		}

		return NameCollector.definitionRequests.collectRanked(defn);
	}
}
