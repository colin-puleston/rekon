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
public class NodePattern extends Expression {

	private NameList names = new NameList();

	private Set<Relation> relations = new HashSet<Relation>();
	private Set<Relation> signatureRelations = null;

	public NodePattern(NodeName name) {

		names.add(name);
	}

	public NodePattern(Names names) {

		this.names.addAll(names);
	}

	public NodePattern(NodeName name, Relation relation) {

		this(name);

		relations.add(relation);
	}

	public NodePattern(NodeName name, Collection<Relation> relations) {

		this(name);

		this.relations.addAll(relations);
	}

	public NodePattern(Names names, Collection<Relation> relations) {

		this(names);

		this.relations.addAll(relations);
	}

	NodePattern combineWith(NodePattern other) {

		NameSet newNames = new NameSet(names);
		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newNames.addAll(other.names);
		newRelations.addAll(other.relations);

		purgeSubsumers(newNames, names);
		purgeSubsumers(newNames, other.names);

		return new NodePattern(newNames, newRelations);
	}

	NodePattern extend(Collection<Relation> otherRelations) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.addAll(otherRelations);

		return new NodePattern(names, newRelations);
	}

	NodeName asNodeName() {

		if (names.size() == 1 && relations.isEmpty()) {

			return (NodeName)names.getFirstName();
		}

		return null;
	}

	Names getNames() {

		return names;
	}

	boolean nestedPattern() {

		return !relations.isEmpty();
	}

	void registerDefinitionRefedNames() {

		names.registerAsDefinitionRefed(PatternNameRole.ROOT);

		for (Relation r : relations) {

			r.registerDefinitionRefedNames();
		}
	}

	void collectNames(NameCollector collector) {

		collector.collectNames(names);

		for (Relation r : getCollectorRelations(collector)) {

			r.collectNames(collector.forNextRank());
		}
	}

	boolean subsumes(NodePattern p) {

		return subsumesAllNames(p) && allRelationsSubsumeSignatureRelations(p);
	}

	boolean reclassifiable() {

		for (Name n : names.getNames()) {

			if (n.newSubsumers(NodeMatcher.ANY)) {

				return true;
			}
		}

		for (Relation r : getSignatureRelations()) {

			if (r.getTarget().newSubsumers(NodeMatcher.ANY)) {

				return true;
			}
		}

		return false;
	}

	boolean potentialSignatureUpdates() {

		for (Name n : names.getNames()) {

			if (n.newSubsumers(NodeMatcher.STRUCTURED)) {

				return true;
			}
		}

		for (Relation r : getSignatureRelations()) {

			if (r.potentialNewSignatureRelations()) {

				return true;
			}
		}

		return false;
	}

	Collection<Relation> getSignatureRelations() {

		if (signatureRelations == null) {

			signatureRelations = new HashSet<Relation>();

			addSignatureRelations(this);

			for (Name n : names.getNames()) {

				addAllImpliedSignatureRelations((NodeName)n);
			}
		}

		return signatureRelations;
	}

	void resetSignatureRefs() {

		signatureRelations = null;
	}

	void render(PatternRenderer r) {

		r.addLine(namesToString());

		r = r.nextLevel();

		for (Relation re : relations) {

			re.render(r);
		}
	}

	private boolean subsumesAllNames(NodePattern p) {

		for (Name n : names.getNames()) {

			if (!subsumesAnyName(n, p)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAnyName(Name n, NodePattern p) {

		for (Name pn : p.names.getNames()) {

			if (n.subsumes(pn)) {

				return true;
			}
		}

		return false;
	}

	private boolean allRelationsSubsumeSignatureRelations(NodePattern p) {

		for (Relation r : relations) {

			if (!subsumesAnySignatureRelation(r, p)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAnySignatureRelation(Relation r, NodePattern p) {

		for (Relation sr : p.getSignatureRelations()) {

			if (r.subsumes(sr)) {

				return true;
			}
		}

		return false;
	}

	private void addAllImpliedSignatureRelations(NodeName name) {

		addSignatureRelations(name);

		for (Name sn : name.getSubsumers().getNames()) {

			addSignatureRelations((NodeName)sn);
		}
	}

	private void addSignatureRelations(NodeName name) {

		NodePattern p = name.getProfile();

		if (p != null && p != this) {

			addSignatureRelations(p);
		}
	}

	private void addSignatureRelations(NodePattern p) {

		for (Relation r : p.relations) {

			for (Relation sr : r.expandForSignature()) {

				checkAddSignatureRelation(sr);
			}
		}
	}

	private void checkAddSignatureRelation(Relation r) {

		for (Relation sr : new HashSet<Relation>(signatureRelations)) {

			if (r.subsumes(sr)) {

				return;
			}

			if (sr.subsumes(r)) {

				signatureRelations.remove(sr);
			}
		}

		signatureRelations.add(r);
	}

	private void purgeSubsumers(NameSet target, Names purger) {

		for (Name n : purger.getNames()) {

			target.removeAll(n.getSubsumers().getNames());
		}
	}

	private Collection<Relation> getCollectorRelations(NameCollector collector) {

		return collector.signature() ? getSignatureRelations() : relations;
	}

	private String namesToString() {

		if (names.size() == 1) {

			return names.getFirstName().getLabel();
		}

		List<String> l = new ArrayList<String>();

		for (Name n : names.getNames()) {

			l.add(n.getLabel());
		}

		return l.toString();
	}
}
