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
public class Pattern extends Expression {

	private NameList nodes = new NameList();

	private Set<Relation> relations = new HashSet<Relation>();
	private Set<Relation> profileRelations = relations;

	private ProfileExpansionOption profileExpansionOption = ProfileExpansionOption.NONE;

	private enum ProfileExpansionOption {NONE, PRE_EXPANDED, CHECK_EXPANSION}

	private class ProfileExpander extends ProfileRelationCollector {

		ProfileExpander(NodeVisitMonitor visitMonitor) {

			super(visitMonitor, profileRelations);
		}

		Set<Relation> ensureCollectorSetUpdatable() {

			if (profileRelations == relations) {

				profileRelations = new HashSet<Relation>(relations);
			}

			return profileRelations;
		}
	}

	public Pattern(NodeX node) {

		nodes.add(node);
	}

	public Pattern(Names nodes) {

		this.nodes.addAll(nodes);
	}

	public Pattern(NodeX node, Relation relation) {

		this(node);

		relations.add(relation);
	}

	public Pattern(NodeX node, Collection<Relation> relations) {

		this(node);

		this.relations.addAll(relations);
	}

	public Pattern(Names nodes, Collection<Relation> relations) {

		this(nodes);

		this.relations.addAll(relations);
	}

	public NodeX toSingleNode() {

		if (nodes.size() == 1 && relations.isEmpty()) {

			return (NodeX)nodes.getFirstName();
		}

		return null;
	}

	Pattern combineWith(Pattern other) {

		NameSet newNodes = new NameSet(nodes);
		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newNodes.addAll(other.nodes);
		newRelations.addAll(other.relations);

		if (newNodes.size() > 1) {

			checkRemoveRoot(newNodes);
		}

		purgeSubsumers(newNodes, nodes);
		purgeSubsumers(newNodes, other.nodes);

		return new Pattern(newNodes, newRelations);
	}

	Pattern extend(Relation extraRelation) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.add(extraRelation);

		return new Pattern(nodes, newRelations);
	}

	Pattern extend(Collection<Relation> extraRelations) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.addAll(extraRelations);

		return new Pattern(nodes, newRelations);
	}

	void registerDefinitionRefedNames() {

		registerAsDefinitionRefed(nodes, MatchRole.PATTERN_ROOT);

		for (Relation r : relations) {

			r.getProperty().registerAsDefinitionRefed(MatchRole.PATTERN_RELATION);

			registerAsDefinitionRefed(r.getTargetNodes(), MatchRole.PATTERN_VALUE);
		}
	}

	void setProfileExpansionCheckRequired() {

		profileExpansionOption = ProfileExpansionOption.CHECK_EXPANSION;
	}

	boolean updateForProfileExpansion() {

		boolean expanded = false;

		switch (profileExpansionOption) {

			case CHECK_EXPANSION:
				expanded = checkProfileExpansion(null);
				break;

			case PRE_EXPANDED:
				expanded = true;
				break;

			case NONE:
				throw new Error("Should never happen!");
		}

		profileExpansionOption = ProfileExpansionOption.NONE;

		return expanded;
	}

	void collectNames(NameCollector collector) {

		collector.collectNames(nodes);

		if (collector.continueForNextRelationsRank()) {

			collector = collector.forNextRank();

			for (Relation r : getRelations(collector.profile())) {

				r.collectNames(collector);
			}
		}
	}

	boolean subsumes(Pattern p) {

		return subsumesAllNames(p) && subsumesRelations(p);
	}

	boolean subsumesRelations(Pattern p) {

		for (Relation r : relations) {

			if (!subsumesAnyProfileRelation(r, p)) {

				return false;
			}
		}

		return true;
	}

	boolean classifiable(boolean initialPass) {

		for (Name n : nodes.getNames()) {

			if (((NodeX)n).classifiablePatternRoot(initialPass)) {

				return true;
			}
		}

		for (Relation r : getProfileRelations()) {

			if (r.getProperty().classifiablePatternProperty()) {

				for (Name tn : r.getTargetNodes().getNames()) {

					if (((NodeX)tn).classifiablePatternValue(initialPass)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	Names getNodes() {

		return nodes;
	}

	Collection<Relation> getDirectRelations() {

		return relations;
	}

	Collection<Relation> getProfileRelations() {

		return profileRelations;
	}

	Collection<Relation> getExpandedProfileRelations(NodeVisitMonitor visitMonitor) {

		if (profileExpansionOption == ProfileExpansionOption.CHECK_EXPANSION) {

			Set<Relation> preProfileRels = new HashSet<Relation>(profileRelations);

			if (checkProfileExpansion(visitMonitor)) {

				if (visitMonitor.incompleteTraversal()) {

					Set<Relation> postProfileRels = new HashSet<Relation>(profileRelations);

					profileRelations = preProfileRels;

					return postProfileRels;
				}

				profileExpansionOption = ProfileExpansionOption.PRE_EXPANDED;
			}
		}

		return profileRelations;
	}

	boolean nestedPattern(boolean profile) {

		return !getRelations(profile).isEmpty();
	}

	void render(PatternRenderer r) {

		r.addLine(nodesToString());

		r = r.nextLevel();

		for (Relation re : relations) {

			re.render(r);
		}
	}

	private void registerAsDefinitionRefed(Names regNames, MatchRole role) {

		for (Name n : regNames.getNames()) {

			n.registerAsDefinitionRefed(role);
		}
	}

	private boolean checkProfileExpansion(NodeVisitMonitor visitMonitor) {

		boolean newSubsumers = anyLastPhaseInferredSubsumers();
		boolean expandableRels = anyExpandableRelations();

		if (newSubsumers || expandableRels) {

			if (visitMonitor == null) {

				visitMonitor = new NodeVisitMonitor(nodes);
			}

			ProfileExpander e = new ProfileExpander(visitMonitor);

			if (newSubsumers) {

				e.collectFromSubsumers(nodes);
			}

			if (expandableRels) {

				e.collectFromRelationExpansions(relations);
			}

			return e.anyAdditions();
		}

		return false;
	}

	private boolean anyLastPhaseInferredSubsumers() {

		for (Name n : nodes.getNames()) {

			if (((NodeX)n).getNodeClassifier().anyLastPhaseInferredSubsumers()) {

				return true;
			}
		}

		return false;
	}

	private boolean anyExpandableRelations() {

		for (Relation r : relations) {

			if (r.expandableRelation()) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesAllNames(Pattern p) {

		for (Name n : nodes.getNames()) {

			if (!subsumesAnyName(n, p)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAnyName(Name n, Pattern p) {

		for (Name pn : p.nodes.getNames()) {

			if (n.subsumes(pn)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesAnyProfileRelation(Relation r, Pattern p) {

		for (Relation sr : p.getProfileRelations()) {

			if (r.subsumes(sr)) {

				return true;
			}
		}

		return false;
	}

	private void checkRemoveRoot(NameSet target) {

		for (Name n : target.copyNames()) {

			if (n.rootName()) {

				target.remove(n);

				break;
			}
		}
	}

	private void purgeSubsumers(NameSet target, Names purger) {

		for (Name n : purger.getNames()) {

			target.removeAll(n.getSubsumers().getNames());
		}
	}

	private Collection<Relation> getRelations(boolean profile) {

		return profile ? getProfileRelations() : relations;
	}

	private String nodesToString() {

		if (nodes.size() == 1) {

			return nodes.getFirstName().getLabel();
		}

		List<String> l = new ArrayList<String>();

		for (Name n : nodes.getNames()) {

			l.add(n.getLabel());
		}

		return l.toString();
	}
}

