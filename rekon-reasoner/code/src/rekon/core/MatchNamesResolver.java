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
class MatchNamesResolver {

	static Names resolve(Names leafNames) {

		return resolve(leafNames, null);
	}

	static Names resolve(Names leafNames, int rank) {

		return resolve(leafNames, MatchRole.rankToRoles(rank));
	}

	static Names resolveForSimplePattern(Names leafNames) {

		return resolve(leafNames, MatchRole.SIMPLE_PATTERN_NODE.roleAsList());
	}

	static private Names resolve(Names leafNames, List<MatchRole> roles) {

		NameSet resolved = new NameSet();

		for (Name n : leafNames) {

			checkAdd(resolved, n, roles);

			for (Name s : n.getSubsumers()) {

				checkAdd(resolved, s, roles);
			}
		}

		return resolved;
	}

	static private void checkAdd(Names resolved, Name n, List<MatchRole> roles) {

		if (roles == null || definitionRefed(n, roles)) {

			resolved.add(n);
		}
	}

	static private boolean definitionRefed(Name n, List<MatchRole> roles) {

		for (MatchRole r : roles) {

			if (n.definitionRefed(r)) {

				return true;
			}
		}

		return false;
	}
}