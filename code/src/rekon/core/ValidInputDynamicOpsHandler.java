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
abstract class ValidInputDynamicOpsHandler implements DynamicOpsHandler {

	public boolean equivalentTo(DynamicOpsHandler other) {

		if (other instanceof ValidInputDynamicOpsHandler) {

			return equivalentTo((ValidInputDynamicOpsHandler)other);
		}

		return false;
	}

	public boolean subsumes(DynamicOpsHandler other) {

		if (other instanceof ValidInputDynamicOpsHandler) {

			return subsumes((ValidInputDynamicOpsHandler)other);
		}

		return false;
	}

	boolean equivalentTo(ValidInputDynamicOpsHandler other) {

		return subsumes(other) && other.subsumes(this);
	}

	boolean subsumes(ValidInputDynamicOpsHandler other) {

		Collection<NodeMatcher> defns = getAllDefinitionMatchers();

		if (!defns.isEmpty()) {

			Collection<NodeMatcher> oProfs = other.getAllProfileMatchers();

			if (!oProfs.isEmpty()) {

				other.configureProfileExpression();

				return anySubsumptions(defns, oProfs);
			}
		}

		return false;
	}

	abstract Collection<NodeMatcher> getAllProfileMatchers();

	abstract Collection<NodeMatcher> getAllDefinitionMatchers();

	abstract Names getPotentialSubNodes();

	abstract void configureProfileExpression();

	private boolean anySubsumptions(
						Collection<NodeMatcher> ms1,
						Collection<NodeMatcher> ms2) {

		for (NodeMatcher m1 : ms1) {

			for (NodeMatcher m2 : ms2) {

				if (m1.subsumes(m2)) {

					return true;
				}
			}
		}

		return false;
	}
}
