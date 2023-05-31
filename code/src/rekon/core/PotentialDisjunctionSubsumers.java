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
class PotentialDisjunctionSubsumers extends PotentialSubsumptions<DisjunctionNode> {

	private List<DisjunctionNode> definitions;

	PotentialDisjunctionSubsumers(List<DisjunctionNode> definitions) {

		this.definitions = definitions;

		registerSingleOptionRank();
	}

	Collection<DisjunctionNode> getPotentialsFor(DisjunctionNode request) {

		return getPotentialsFor(disjunctsToSingletonNamesList(request));
	}

	List<DisjunctionNode> getAllOptions() {

		return definitions;
	}

	List<Names> getOptionMatchNames(DisjunctionNode option, int startRank, int stopRank) {

		return disjunctsToSingletonNamesList(option);
	}

	Names resolveNamesForRegistration(Names names, int rank) {

		return names;
	}

	Names resolveNamesForRetrieval(Names names, int rank) {

		return new MatchNamesExpander(MatchRole.DISJUNCT, false).expand(names);
	}

	boolean unionRankOptionsForRetrieval() {

		return true;
	}

	private List<Names> disjunctsToSingletonNamesList(DisjunctionNode d) {

		return Collections.singletonList(d.getDisjuncts());
	}
}
