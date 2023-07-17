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
abstract class PotentialPatternSubsumeds extends PotentialSubsumptions<PatternNode> {

	private List<PatternNode> allOptions;

	PotentialPatternSubsumeds(List<PatternNode> allOptions) {

		this.allOptions = allOptions;
	}

	void checkAddInstanceOption(InstanceName name) {

		PatternNode pn = name.getPatternNode();

		if (pn != null) {

			allOptions.add(pn);
			registerTransientOption(pn);
		}
	}

	void checkRemoveInstanceOption(InstanceName name) {

		PatternNode pn = name.getPatternNode();

		if (pn != null) {

			allOptions.remove(pn);
			deregisterTransientOption(pn);
		}
	}

	Collection<PatternNode> getPotentialsFor(Pattern request) {

		return getPotentialsFor(getRankedDefinitionNames(request));
	}

	List<PatternNode> getAllOptions() {

		return allOptions;
	}

	List<Names> getOptionMatchNames(PatternNode option, int startRank, int stopRank) {

		return getRankedProfileNames(option.getProfile(), startRank, stopRank);
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
