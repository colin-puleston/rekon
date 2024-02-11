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
class EquivalentsGrouper {

	private Set<Names> groups = new HashSet<Names>();
	private NameSet initialGroupNames = new NameSet();

	Collection<Names> group(Names names) {

		for (Name n : names) {

			Names equivs = n.getEquivalents();

			if (newGroupFor(n, equivs)) {

				Names group = checkCreateGroup(n, equivs);

				if (!group.isEmpty()) {

					groups.add(group);
				}
			}
		}

		return groups;
	}

	private boolean newGroupFor(Name name, Names equivs) {

		if (equivs.isEmpty()) {

			return true;
		}

		if (initialGroupNames.containsAny(equivs)) {

			return false;
		}

		initialGroupNames.add(name);

		return true;
	}

	private Names checkCreateGroup(Name name, Names equivs) {

		Names group = new NameSet();

		checkAddToGroup(group, name);

		for (Name e : equivs) {

			checkAddToGroup(group, e);
		}

		return group;
	}

	private void checkAddToGroup(Names group, Name name) {

		if (name.mapped()) {

			group.add(name);
		}
	}
}
