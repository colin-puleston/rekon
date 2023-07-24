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
abstract class PotentialPatternSubsumeds extends PotentialSubsumptions<PatternMatcher> {

	private List<PatternMatcher> allOptions;

	PotentialPatternSubsumeds(List<PatternMatcher> allOptions) {

		this.allOptions = allOptions;
	}

	void checkAddInstanceOption(InstanceNode node) {

		PatternMatcher pp = node.getProfilePatternMatcher();

		if (pp != null) {

			allOptions.add(pp);
			registerTransientOption(pp);
		}
	}

	void checkRemoveInstanceOption(InstanceNode node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p != null) {

			allOptions.remove(p);
			deregisterTransientOption(p);
		}
	}

	Collection<PatternMatcher> getPotentialsFor(Pattern request) {

		return getPotentialsFor(getRankedDefinitionNames(request));
	}

	List<PatternMatcher> getAllOptions() {

		return allOptions;
	}

	List<Names> getOptionMatchNames(PatternMatcher option, int startRank, int stopRank) {

		return getRankedProfileNames(option.getPattern(), startRank, stopRank);
	}

	Names resolveNamesForRetrieval(Names names, int rank) {

		return names;
	}

	boolean unionRankOptionsForRetrieval() {

		return false;
	}

	abstract List<Names> getRankedDefinitionNames(Pattern defn);

	abstract List<Names> getRankedProfileNames(Pattern profile, int startRank, int stopRank);
}
