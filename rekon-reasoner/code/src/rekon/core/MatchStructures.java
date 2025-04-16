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

		boolean visit(DisjunctionMatcher m) {

			m.ensureDefinition();

			return true;
		}
	}

	private class ProfileRelationsDisjunctionTargetConverter {

		private NodeX node;

		ProfileRelationsDisjunctionTargetConverter(NodeX node) {

			this.node = node;
		}

		void checkAll(Collection<Relation> relations) {

			for (Relation r : relations) {

				if (r instanceof SomeRelation) {

					check((SomeRelation)r);
				}
			}
		}

		private void check(SomeRelation relation) {

			NodeProperty p = relation.getNodeProperty();
			NodeX v = relation.getNodeValueTarget().getValueNode();

			for (DisjunctionMatcher d : v.getAllDisjunctionMatchers()) {

				convert(p, d);
			}
		}

		private void convert(NodeProperty p, DisjunctionMatcher disjunction) {

			List<ClassNode> newDjs = new ArrayList<ClassNode>();

			for (NodeX d : disjunction.getExpandedDisjuncts().asNodes()) {

				newDjs.add(addNewDisjunct(p, new NodeValue(d)));
			}

			node.addSubsumer(addNewDisjunction(newDjs));
		}

		private ClassNode addNewDisjunct(NodeProperty p, NodeValue target) {

			ClassNode c = createPatternClass();
			SomeRelation r = new SomeRelation(p, target);

			addProfilePattern(c, new Pattern(c, r));

			return c;
		}

		private ClassNode addNewDisjunction(List<ClassNode> disjuncts) {

			ClassNode c = createDisjunctionClass();

			addDisjunction(c, disjuncts, false);

			return c;
		}
	}

	public void checkAddProfilePattern(NodeX node, Collection<Relation> relations) {

		if (node.getClassifier().multipleAssertedSubsumers() || !relations.isEmpty()) {

			addProfilePattern(node, new Pattern(node, relations));
		}
	}

	public PatternMatcher addProfilePattern(NodeX node, Pattern profile) {

		ensurePatternNodesAreSubsumers(node, profile);
		checkEnhanceNewProfilePattern(node, profile);

		PatternMatcher m = node.addProfilePatternMatcher(profile);

		onAddedProfileMatcher(m);

		return m;
	}

	public PatternMatcher addDefinitionPattern(NodeX node, Pattern defn) {

		ensurePatternNodesAreSubsumers(node, defn);
		ensureProfilePatternMatcher(node, defn);

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

	public ClassNode createDisjunctionClass() {

		return freeClasses.createDisjunctionClass();
	}

	public ClassNode createMultiDefinitionClass() {

		return freeClasses.createMultiDefinitionClass();
	}

	MatchStructures(FreeClasses freeClasses) {

		this.freeClasses = freeClasses;
	}

	void onAddedProfileMatcher(NodeMatcher matcher) {
	}

	private void ensureProfilePatternMatcher(NodeX node, Pattern defn) {

		PatternMatcher pm = node.getProfilePatternMatcher();

		if (pm == null) {

			pm = node.addProfilePatternMatcher();

			onAddedProfileMatcher(pm);
		}

		Collection<Relation> newRels = pm.absorbDefinitionIntoProfile(defn);

		checkEnhanceProfilePattern(node, pm.getPattern(), newRels);
	}

	private void ensurePatternNodesAreSubsumers(NodeX node, Pattern pattern) {

		node.addSubsumers(pattern.getNodes());
	}

	private void checkEnhanceNewProfilePattern(NodeX node, Pattern profile) {

		checkEnhanceProfilePattern(node, profile, profile.getDirectRelations());
	}

	private void checkEnhanceProfilePattern(
					NodeX node,
					Pattern profile,
					Collection<Relation> newRels) {

		if (!newRels.isEmpty()) {

			new ProfileRelationsDisjunctionTargetConverter(node).checkAll(newRels);
		}
	}
}
