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

		newNodes.absorbAll(other.nodes);
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

		registerAsDefinitionRefed(nodes, MatchRole.PATTERN_ROOT);

		for (Relation r : directRelations) {

			r.getProperty().registerAsDefinitionRefed(MatchRole.PATTERN_RELATION);

			registerAsDefinitionRefed(r.getTargetNodes(), MatchRole.PATTERN_VALUE);
		}
	}

	void collectNames(NameCollector collector) {

		collector.collectNames(nodes);

		if (collector.continueForNextRelationsRank()) {

			Collection<Relation> rels = getRelations(collector.profile());

			if (!rels.isEmpty()) {

				collector = collector.forNextRank();

				for (Relation r : rels) {

					r.collectNames(collector);
				}
			}
		}
	}

	boolean expandedProfile() {

		return getProfileRelations().expanded();
	}

	boolean subsumes(Pattern p) {

		return subsumesAllNames(p) && subsumesRelations(p);
	}

	boolean subsumesRelations(Pattern p) {

		for (Relation r : directRelations) {

			if (!p.getProfileRelations().anySubsumedBy(r)) {

				return false;
			}
		}

		return true;
	}

	boolean matchable(boolean initialPass) {

		for (NodeX n : nodes.asNodes()) {

			if (n.matchablePatternRoot(initialPass)) {

				return true;
			}
		}

		for (Relation r : getProfileRelations().getAll()) {

			if (r.getProperty().matchablePatternProperty()) {

				for (NodeX tn : r.getTargetNodes().asNodes()) {

					if (tn.matchablePatternValue(initialPass)) {

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

	NodeX getSingleNode() {

		if (nodes.size() == 1) {

			return (NodeX)nodes.getFirstName();
		}

		throw new Error("Not a single-node pattern!");
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

	private void registerAsDefinitionRefed(Names regNames, MatchRole role) {

		for (NodeX n : regNames.asNodes()) {

			n.registerAsDefinitionRefed(role);
		}
	}

	private boolean subsumesAllNames(Pattern p) {

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

	private Collection<Relation> getRelations(boolean profile) {

		return profile ? getProfileRelations().getAll() : directRelations;
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

