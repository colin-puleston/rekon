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
public class GCIClasses extends FreeOntologyClasses {

	static private class GCIName extends FreeClassName {

		GCIName(int index) {

			super(index);
		}
	}

	private MatchableNodes matchables;
	private int index = 0;

	public FreeClassName create(NodePattern defn) {

		return super.create(null, defn);
	}

	public FreeClassName create(Collection<NodePattern> defns) {

		return super.create(null, defns);
	}

	public FreeClassName create(ClassName sup, NodePattern defn) {

		return super.create(sup, defn);
	}

	public FreeClassName create(ClassName sup, Collection<NodePattern> defns) {

		return super.create(sup, defns);
	}

	GCIClasses(MatchableNodes matchables, Collection<NodeName> ontologyNodes) {

		super(matchables, ontologyNodes);
	}

	FreeClassName createClassName(int index) {

		return new GCIName(index);
	}
}
