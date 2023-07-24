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

	private List<PatternMatcher> newProfilePatterns = new ArrayList<PatternMatcher>();

	InverseRelationsAdder(NodeMatchers nodeMatchers) {

		for (PatternMatcher p : nodeMatchers.getProfilePatterns()) {

			if (p.getNode() instanceof IndividualNode) {

				addAnyFor(p);
			}
		}

		for (PatternMatcher p : newProfilePatterns) {

			nodeMatchers.addProfilePattern(p);
		}
	}

	private void addAnyFor(PatternMatcher p) {

		for (Relation r : p.getPattern().getDirectRelations()) {

			if (r instanceof SomeRelation) {

				addAnyFor(p.getNode(), (SomeRelation)r);
			}
		}
	}

	private void addAnyFor(GNode forwardSource, SomeRelation forwardRel) {

		Collection<NodeProperty> ips = forwardRel.getNodeProperty().getInverses();

		if (!ips.isEmpty()) {

			NodeValue invSource = forwardRel.getNodeValueTarget();
			NodeValue invTarget = new NodeValue(forwardSource);

			PatternMatcher p = resolveProfilePattern(invSource.getValueNode());

			for (NodeProperty ip : ips) {

				p.addRelation(new SomeRelation(ip, invTarget));
			}
		}
	}

	private PatternMatcher resolveProfilePattern(GNode node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p == null) {

			p = node.addProfilePatternMatcher();

			newProfilePatterns.add(p);
		}

		return p;
	}
}
