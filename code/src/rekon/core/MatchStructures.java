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
public class MatchStructures {

	private FreeClasses freeClasses;
	private DefinitionPropagator definitionPropagator = new DefinitionPropagator();

	private Map<Collection<NodeX>, ClassNode> insertedProfileDisjunctions
									= new HashMap<Collection<NodeX>, ClassNode>();

	private class DefinitionPropagator extends PatternCrawler {

		boolean visit(NodeX n) {

			if (n instanceof FreeClassNode) {

				n.ensurePatternProfiledImpliesPatternDefined();

				return true;
			}

			return false;
		}

		boolean visit(PatternMatcher m) {

			return true;
		}

		boolean visit(DisjunctionMatcher m) {

			m.ensureDefinition();

			return true;
		}
	}

	public void checkAddProfilePattern(NodeX node, Collection<Relation> relations) {

		if (node.getClassifier().multipleAssertedSubsumers() || !relations.isEmpty()) {

			addProfilePattern(node, new Pattern(node, relations));
		}
	}

	public PatternMatcher addProfilePattern(NodeX node, Pattern profile) {

		ensurePatternNodesAreSubsumers(node, profile);

		PatternMatcher m = node.addProfilePatternMatcher(profile);

		onAddedProfileMatcher(m);

		return m;
	}

	public PatternMatcher addDefinitionPattern(NodeX node, Pattern defn) {

		ensurePatternNodesAreSubsumers(node, defn);
		ensureProfilePatternMatcher(node).absorbDefinitionIntoProfile(defn);

		PatternMatcher m = node.addDefinitionPatternMatcher(defn);

		definitionPropagator.crawlFrom(defn);

		return m;
	}

	public DisjunctionMatcher addDisjunction(
									NodeX node,
									Collection<? extends NodeX> disjuncts,
									boolean definition) {

		DisjunctionMatcher m = node.addDisjunctionMatcher(disjuncts);

		if (definition) {

			m.ensureDefinition();

			definitionPropagator.crawlFrom(m);
		}

		onAddedProfileMatcher(m);

		return m;
	}

	public ClassNode createPatternClass() {

		return freeClasses.createPatternClass();
	}

	public ClassNode createDefinitionClass() {

		return freeClasses.createDefinitionClass();
	}

	MatchStructures(FreeClasses freeClasses) {

		this.freeClasses = freeClasses;
	}

	ClassNode resolveInsertedProfileDisjunction(Collection<NodeX> disjuncts) {

		ClassNode cn = insertedProfileDisjunctions.get(disjuncts);

		if (cn == null) {

			cn = createPatternClass();

			addDisjunction(cn, disjuncts, false);

			insertedProfileDisjunctions.put(disjuncts, cn);
		}

		return cn;
	}

	void onAddedProfileMatcher(NodeMatcher matcher) {
	}

	private PatternMatcher ensureProfilePatternMatcher(NodeX node) {

		PatternMatcher p = node.getProfilePatternMatcher();

		if (p == null) {

			p = node.addProfilePatternMatcher();

			onAddedProfileMatcher(p);
		}

		return p;
	}

	private void ensurePatternNodesAreSubsumers(NodeX node, Pattern pattern) {

		node.addSubsumers(pattern.getNodes());
	}
}
