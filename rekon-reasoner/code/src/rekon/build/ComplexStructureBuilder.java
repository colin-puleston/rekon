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

package rekon.build;

import java.util.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
abstract class ComplexStructureBuilder<A extends InputAxiom> {

	void build(Iterable<A> axioms) {

		for (A ax : axioms) {

			InputNode first = getFirst(ax);
			InputNode second = getSecond(ax);

			boolean complex1 = complexClassNode(first);
			boolean complex2 = complexClassNode(second);

			if (complex1 && complex2) {

				buildFor(first, second);
			}
			else {

				if (complex1) {

					buildFor(first, second.asClassNode());
				}
				else if (complex2) {

					buildFor(first.asClassNode(), second);
				}
			}
		}
	}

	abstract InputNode getFirst(A axiom);

	abstract InputNode getSecond(A axiom);

	abstract void buildFor(InputNode first, InputNode second);

	abstract void buildFor(InputNode first, ClassNode second);

	abstract void buildFor(ClassNode first, InputNode second);

	private boolean complexClassNode(InputNode n) {

		switch (n.getNodeType()) {

			case CLASS:

				return false;

			case INDIVIDUAL:

				throw new RuntimeException("Unexpected individual-node: " + n);
		}

		return true;
	}
}
