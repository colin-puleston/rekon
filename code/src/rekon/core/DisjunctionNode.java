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
class DisjunctionNode extends MatchableNode<DisjunctionNode> {

	private Names disjuncts;

	public String toString() {

		return getClass().getSimpleName() + "(" + getDisjunctLabelsList() + ")";
	}

	DisjunctionNode(ClassName name, Collection<? extends NodeName> disjuncts) {

		super(name);

		this.disjuncts = new NameList(disjuncts);

		for (Name d : disjuncts) {

			if (!name.dynamic()) {

				d.addSubsumer(name);
			}

			d.registerAsDefinitionRefed(MatchRole.DISJUNCT);
		}
	}

	Names getDisjuncts() {

		return disjuncts;
	}

	boolean subsumes(DisjunctionNode other) {

		for (Name d : other.disjuncts.getNames()) {

			if (!subsumesNode((NodeName)d)) {

				return false;
			}
		}

		return true;
	}

	boolean classifiable(boolean initialPass) {

		for (Name d : disjuncts.getNames()) {

			if (((NodeName)d).classifyTargetDisjunct(initialPass)) {

				return true;
			}
		}

		return false;
	}

	void acceptVisitor(MatchableNodeVisitor visitor) {

		visitor.visit(this);
	}

	Class<DisjunctionNode> getMatchableClass() {

		return DisjunctionNode.class;
	}

	private boolean subsumesNode(NodeName n) {

		for (Name d : disjuncts.getNames()) {

			if (d.subsumes(n)) {

				return true;
			}
		}

		return false;
	}

	private String getDisjunctLabelsList() {

		String s = new String();

		for (Name d : disjuncts.getNames()) {

			if (!s.isEmpty()) {

				s += ',';
			}

			s += d.getLabel();
		}

		return s;
	}
}
