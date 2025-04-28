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

	private class DefinitionPropagator extends PatternCrawler {

		boolean visit(NodeX n) {

			if (!n.mapped()) {

				n.ensurePatternProfiledImpliesPatternDefined();

				return true;
			}

			return false;
		}

		boolean visit(PatternMatcher m) {

			return true;
		}
	}

	public void checkAddProfile(NodeX node, Collection<Relation> relations) {

		if (node.getClassifier().multipleAssertedSubsumers() || !relations.isEmpty()) {

			addProfile(node, new Pattern(node, relations));
		}
	}

	public PatternMatcher addProfile(NodeX node, Pattern profile) {

		ensurePatternNodesAreSubsumers(node, profile);

		PatternMatcher m = node.addProfile(profile);

		onAddedProfile(m);

		return m;
	}

	public PatternMatcher addDefinition(NodeX node, Pattern defn) {

		ensurePatternNodesAreSubsumers(node, defn);
		ensureProfile(node, defn);

		PatternMatcher m = node.addDefinitionMatcher(defn);

		definitionPropagator.crawlFrom(defn);

		return m;
	}

	public ClassNode createFreeClass() {

		return freeClasses.create();
	}

	MatchStructures(FreeClasses freeClasses) {

		this.freeClasses = freeClasses;
	}

	void onAddedProfile(PatternMatcher matcher) {
	}

	private void ensureProfile(NodeX node, Pattern defn) {

		PatternMatcher pm = node.getProfileMatcher();

		if (pm == null) {

			pm = node.addProfile();

			onAddedProfile(pm);
		}

		Collection<Relation> newRels = pm.absorbDefinitionIntoProfile(defn);
	}

	private void ensurePatternNodesAreSubsumers(NodeX node, Pattern pattern) {

		node.addSubsumers(pattern.getNodes());
	}
}
