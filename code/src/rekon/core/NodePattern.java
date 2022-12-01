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

	private SignatureMode signatureMode = SignatureMode.EXPANDED;

	private enum SignatureMode {RESTRICTED, EXPANDING, EXPANDED}

	private class PatternSignatureRelationCollector extends SignatureRelationCollector {

		private Set<Relation> initialCollected;
		private boolean additions = false;

		private NodeVisitMonitor visitMonitor;

		PatternSignatureRelationCollector() {

			this(new NodeVisitMonitor(getSingleName()));
		}

		PatternSignatureRelationCollector(NodeVisitMonitor visitMonitor) {

			super(visitMonitor);

			this.visitMonitor = visitMonitor;

			initialCollected = getCollected();
		}

		Set<Relation> collect() {

			if (signatureMode != SignatureMode.RESTRICTED) {

				collectFromSubsumers(getSingleName());
			}

			if (signatureMode != SignatureMode.EXPANDING) {

				collectFromRelationExpansions(relations);
			}

			return getCollected();
		}

		Set<Relation> getInitialCollected() {

			return signatureMode == SignatureMode.EXPANDING ? signatureRelations : relations;
		}

		Set<Relation> ensureUpdatable(Set<Relation> collected) {

			if (collected == initialCollected) {

				additions = true;

				if (signatureMode == SignatureMode.EXPANDING) {

					return initialCollected;
				}

				return new HashSet<Relation>(initialCollected);
			}

			return collected;
		}

		boolean additions() {

			return additions;
		}
	}

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

	void setRestrictedSignature() {

		signatureMode = SignatureMode.RESTRICTED;
	}

	boolean setExpandedSignature() {

		if (signatureRelations == null) {

			signatureMode = SignatureMode.EXPANDED;

			return false;
		}

		signatureMode = SignatureMode.EXPANDING;

		boolean additions = setSignatureRelations();

		signatureMode = SignatureMode.EXPANDED;

		return additions;
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

	NodePattern extend(Relation extraRelation) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.add(extraRelation);

		return new NodePattern(names, newRelations);
	}

	NodePattern extend(Collection<Relation> extraRelations) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.addAll(extraRelations);

		return new NodePattern(names, newRelations);
	}

	NodeName getSingleName() {

		if (names.size() > 1) {

			throw new Error("Multiple names!");
		}

		return (NodeName)names.get(0);
	}

	NodeName toSingleName() {

		if (names.size() == 1 && relations.isEmpty()) {

			return (NodeName)names.getFirstName();
		}

		return null;
	}

	Names getNames() {

		return names;
	}

	boolean nestedPattern(boolean signature) {

		return !getRelations(signature).isEmpty();
	}

	void registerDefinitionRefedNames() {

		registerAsDefinitionRefed(names, PatternNameRole.ROOT);

		for (Relation r : relations) {

			r.getProperty().registerAsDefinitionRefed(PatternNameRole.RELATION);

			registerAsDefinitionRefed(r.getTargetNodes(), PatternNameRole.VALUE);
		}
	}

	void collectNames(NameCollector collector) {

		collector.collectNames(names);

		if (!collector.lastRequiredRank()) {

			for (Relation r : getRelations(collector.signature())) {

				r.collectNames(collector.forNextRank());
			}
		}
	}

	boolean subsumes(NodePattern p) {

		return subsumesAllNames(p) && allRelationsSubsumeSignatureRelations(p);
	}

	boolean classifiable(boolean initialPass) {

		if (getSingleName().classifierTargetRoot(initialPass)) {

			return true;
		}

		for (Relation r : getSignatureRelations()) {

			if (r.getProperty().classifierTargetProperty()) {

				for (Name tn : r.getTargetNodes().getNames()) {

					if (((NodeName)tn).classifierTargetValue(initialPass)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	boolean potentialSignatureUpdates() {

		for (Name n : names.getNames()) {

			if (n.anyNewSubsumers(NodeMatcher.STRUCTURED)) {

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

	Collection<Relation> getDirectRelations() {

		return relations;
	}

	Collection<Relation> getSignatureRelations() {

		if (signatureRelations == null) {

			setSignatureRelations();
		}

		return signatureRelations;
	}

	Collection<Relation> resolveSignatureRelations(NodeVisitMonitor visitMonitor) {

		if (signatureRelations == null) {

			Set<Relation> collected = collectSignatureRelations(visitMonitor);

			if (visitMonitor.incompleteTraversal()) {

				return collected;
			}

			signatureRelations = collected;
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

	private void registerAsDefinitionRefed(Names regNames, PatternNameRole role) {

		for (Name n : regNames.getNames()) {

			n.registerAsDefinitionRefed(role);
		}
	}

	private boolean setSignatureRelations() {

		PatternSignatureRelationCollector collector = new PatternSignatureRelationCollector();

		signatureRelations = collector.collect();

		return collector.additions();
	}

	private Set<Relation> collectSignatureRelations(NodeVisitMonitor visitMonitor) {

		return new PatternSignatureRelationCollector(visitMonitor).collect();
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

	private void purgeSubsumers(NameSet target, Names purger) {

		for (Name n : purger.getNames()) {

			target.removeAll(n.getSubsumers().getNames());
		}
	}

	private Collection<Relation> getRelations(boolean signature) {

		return signature ? getSignatureRelations() : relations;
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
