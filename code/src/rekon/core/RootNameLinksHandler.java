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
class RootNameLinksHandler extends NameLinksHandler {

	private Iterable<? extends Name> allSubs = null;

	void configure(Iterable<? extends Name> allSubs) {

		this.allSubs = allSubs;
	}

	Names getSubsumers() {

		return Names.NO_NAMES;
	}

	Names getEquivalents() {

		return Names.NO_NAMES;
	}

	Names getSupers(boolean direct) {

		return Names.NO_NAMES;
	}

	Names getSubs(boolean direct) {

		if (allSubs == null) {

			throw new Error("Subs have not been set!");
		}

		Names subs = new NameList();

		for (Name s : allSubs) {

			if (!direct || s.getSubsumers().isEmpty()) {

				subs.add(s);
			}
		}

		return subs;
	}

	boolean hasSubsumer(Name test) {

		return false;
	}
}
