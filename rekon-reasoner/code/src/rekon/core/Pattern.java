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
public class Pattern extends PatternComponent {

	private NameList nodes = new NameList();
	private Set<Relation> directRelations = new HashSet<Relation>();

	private ProfileRelations profileRelations = null;

	public Pattern(NodeX node) {

		nodes.add(node);
	}

	public Pattern(Names nodes) {

		this.nodes.addAll(nodes);
	}

	public Pattern(NodeX node, Relation directRelation) {

		this(node);

		directRelations.add(directRelation);
	}

	public Pattern(NodeX node, Collection<Relation> directRelations) {

		this(node);

		this.directRelations.addAll(directRelations);
	}

	public Pattern(Names nodes, Collection<Relation> directRelations) {

		this(nodes);

		this.directRelations.addAll(directRelations);
	}

	public NodeX toSingleNode() {

		return nodes.size() == 1 && directRelations.isEmpty() ? getSingleNode() : null;
	}

	Pattern combineWith(Pattern other) {

		NameSet newNodes = new NameSet(nodes);
		Set<Relation> newRelations = new HashSet<Relation>(directRelations);

		newNodes.retainMostSpecific(other.nodes);
		newRelations.addAll(other.directRelations);

		return new Pattern(newNodes, newRelations);
	}

	Pattern extend(Relation extraRelation) {

		Set<Relation> newRelations = new HashSet<Relation>(directRelations);

		newRelations.add(extraRelation);

		return new Pattern(nodes, newRelations);
	}

	Pattern extend(Collection<Relation> extraRelations) {

		Set<Relation> newRelations = new HashSet<Relation>(directRelations);

		newRelations.addAll(extraRelations);

		return new Pattern(nodes, newRelations);
	}

	void registerDefinitionRefedNames() {

		MatchRole nodesMatchRole = getDefinitionNodesMatchRole();

		for (NodeX n : nodes.asNodes()) {

			n.registerAsDefinitionRefed(nodesMatchRole);
		}

		for (Relation r : directRelations) {

			r.registerAsDefinitionRefed();
		}
	}

	boolean initialPassMatchableProfile() {

		if (matchableSimpleProfileNode(false)) {

			return true;
		}

		if (anyProfileRelations()) {

			return matchableNestedProfileNode(false)
					&& anyMatchableProfileRelations(false);
		}

		return false;
	}

	boolean nonInitialPassMatchableProfile() {

		if (matchableSimpleProfileNode(true)) {

			return true;
		}

		if (anyProfileRelations()) {

			if (matchableNestedProfileNode(true)) {

				return anyMatchableProfileRelations(false)
						|| anyMatchableProfileRelations(true);
			}

			if (anyMatchableProfileRelations(true)) {

				return matchableNestedProfileNode(false);
			}
		}

		return false;
	}

	boolean expandedProfile() {

		return getProfileRelations().expanded();
	}

	boolean subsumes(Pattern p) {

		return subsumesNames(p) && subsumesRelations(p);
	}

	boolean subsumesRelations(Pattern p) {

		for (Relation r : directRelations) {

			if (!p.getProfileRelations().anySubsumedBy(r)) {

				return false;
			}
		}

		return true;
	}

	Names getNodes() {

		return nodes;
	}

	NodeX getSingleNode() {

		if (nodes.size() == 1) {

			return (NodeX)nodes.getFirstName();
		}

		throw new Error("Not a single-node pattern!");
	}

	Collection<Relation> getRelations(boolean profile) {

		return profile ? getProfileRelations().getAll() : directRelations;
	}

	Collection<Relation> getDirectRelations() {

		return directRelations;
	}

	ProfileRelations getProfileRelations() {

		if (profileRelations == null) {

			profileRelations = new ProfileRelations(this);
		}

		return profileRelations;
	}

	boolean nestedPattern(boolean profile) {

		return !getRelations(profile).isEmpty();
	}

	void render(PatternRenderer r) {

		r.addLine(nodesToString());

		r = r.nextLevel();

		for (Relation re : directRelations) {

			re.render(r);
		}
	}

	private boolean subsumesNames(Pattern p) {

		for (NodeX n : nodes.asNodes()) {

			if (!subsumesAnyName(n, p)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAnyName(Name n, Pattern p) {

		for (Name pn : p.nodes) {

			if (n.subsumes(pn)) {

				return true;
			}
		}

		return false;
	}

	private MatchRole getDefinitionNodesMatchRole() {

		return directRelations.isEmpty()
				? MatchRole.SIMPLE_PATTERN_NODE
				: MatchRole.NESTED_PATTERN_ROOT;
	}

	private boolean matchableSimpleProfileNode(boolean newInferencesOnly) {

		return matchableProfileNode(NodeSelector.SIMPLE_PATTERN_NODE, newInferencesOnly);
	}

	private boolean matchableNestedProfileNode(boolean newInferencesOnly) {

		return matchableProfileNode(NodeSelector.NESTED_PATTERN_ROOT, newInferencesOnly);
	}

	private boolean matchableProfileNode(NodeSelector selector, boolean newInferencesOnly) {

		return getSingleNode().matchable(selector, newInferencesOnly);
	}

	private boolean anyMatchableProfileRelations(boolean newInferencesOnly) {

		for (Relation r : getProfileRelations().getAll()) {

			if (r.matchable(newInferencesOnly)) {

				return true;
			}
		}

		return false;
	}

	private boolean anyProfileRelations() {

		return getProfileRelations().anyRelations();
	}

	private String nodesToString() {

		if (nodes.size() == 1) {

			return getSingleNode().getLabel();
		}

		List<String> l = new ArrayList<String>();

		for (Name n : nodes) {

			l.add(n.getLabel());
		}

		return l.toString();
	}
}

