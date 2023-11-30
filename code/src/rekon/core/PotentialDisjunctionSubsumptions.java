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
abstract class PotentialDisjunctionSubsumptions
					extends
						PotentialSubsumptions<DisjunctionMatcher> {

	private List<DisjunctionMatcher> options;

	PotentialDisjunctionSubsumptions(List<DisjunctionMatcher> options) {

		this.options = options;

		registerSingleOptionRank();
	}

	Collection<DisjunctionMatcher> getPotentialsFor(DisjunctionMatcher request) {

		return getPotentialsFor(requestToSingletonNamesList(request));
	}

	List<DisjunctionMatcher> getAllOptions() {

		return options;
	}

	List<Names> getOptionMatchNames(DisjunctionMatcher option, int startRank, int stopRank) {

		return Collections.singletonList(resolveRegistrationDisjuncts(option.getDisjuncts()));
	}

	abstract Names resolveRegistrationDisjuncts(Names disjuncts);

	abstract Names resolveRequestDisjuncts(Names disjuncts);

	private List<Names> requestToSingletonNamesList(DisjunctionMatcher d) {

		return Collections.singletonList(resolveRequestDisjuncts(d.getDisjuncts()));
	}
}
