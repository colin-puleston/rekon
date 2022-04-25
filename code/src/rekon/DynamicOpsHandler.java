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
abstract class DynamicOpsHandler {

	abstract Names getEquivalents();

	abstract Names getSupers(boolean direct);

	abstract Names getSubs(boolean direct);

	abstract Names getInstances(boolean direct);

	boolean equivalentTo(DynamicOpsHandler other) {

		Collection<NodePattern> oDefns = other.getDefinitions();

		if (!oDefns.isEmpty()) {

			for (NodePattern d : getDefinitions()) {

				for (NodePattern od : oDefns) {

					if (d.subsumes(od) && od.subsumes(d)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	boolean subsumes(DynamicOpsHandler other) {

		Collection<NodePattern> ops = other.getProfiles();

		if (!ops.isEmpty()) {

			for (NodePattern d : getDefinitions()) {

				for (NodePattern op : ops) {

					if (d.subsumes(op)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	abstract Collection<NodePattern> getProfiles();

	abstract Collection<NodePattern> getDefinitions();
}
