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
class InverseRelationsAdder {

	private List<PatternNode> newPatternNodes = new ArrayList<PatternNode>();

	InverseRelationsAdder(MatchableNodes matchables) {

		for (PatternNode n : matchables.getAllPatternNodes()) {

			if (n.getName() instanceof IndividualName) {

				addAnyFor(n);
			}
		}

		for (PatternNode n : newPatternNodes) {

			matchables.addPatternNode(n);
		}
	}

	private void addAnyFor(PatternNode n) {

		for (Relation r : n.getProfile().getDirectRelations()) {

			if (r instanceof SomeRelation) {

				addAnyFor(n.getName(), (SomeRelation)r);
			}
		}
	}

	private void addAnyFor(NodeName forwardSource, SomeRelation forwardRel) {

		Collection<ObjectPropertyName> ips = forwardRel.getObjectProperty().getInverses();

		if (!ips.isEmpty()) {

			NodeValue invSource = forwardRel.getNodeTarget();
			NodeValue invTarget = new NodeValue(forwardSource);

			PatternNode n = resolvePatternNode(invSource.getValueNode());

			for (ObjectPropertyName ip : ips) {

				n.addProfileRelation(new SomeRelation(ip, invTarget));
			}
		}
	}

	private PatternNode resolvePatternNode(NodeName name) {

		PatternNode n = name.getPatternNode();

		if (n == null) {

			n = new PatternNode(name);

			newPatternNodes.add(n);
		}

		return n;
	}
}
