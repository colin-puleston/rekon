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
class IndividualRelationsInverter {

	static void addAllInvertions(OntologyNames names) {

		for (IndividualNode n : names.getIndividualNodes()) {

			for (PatternMatcher p : n.getProfileMatcherAsList()) {

				invertAnyFor(p);
			}
		}
	}

	static private void invertAnyFor(PatternMatcher p) {

		for (Relation r : p.getPattern().getDirectRelations()) {

			if (r instanceof SomeRelation) {

				invertAnyFor(p.getNode(), (SomeRelation)r);
			}
		}
	}

	static private void invertAnyFor(NodeX forwardSource, SomeRelation forwardRel) {

		NodeProperty prop = forwardRel.getNodeProperty();

		if (prop.hasInverse()) {

			NodeValue invSource = forwardRel.getNodeValueTarget();

			if (invSource.singleValueNode()) {

				NodeValue invTarget = new NodeValue(forwardSource);

				PatternMatcher pp = resolveProfile(invSource.getSingleValueNode());

				pp.addRelation(new SomeRelation(prop.getInverse(), invTarget));
			}
		}
	}

	static private PatternMatcher resolveProfile(NodeX node) {

		PatternMatcher p = node.getProfileMatcher();

		if (p == null) {

			p = node.addProfile();
		}

		return p;
	}
}
