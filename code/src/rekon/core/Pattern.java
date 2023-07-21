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

	private NameList names = new NameList();

	private Set<Relation> relations = new HashSet<Relation>();
	private Set<Relation> signatureRelations = null;

	private SignatureMode signatureMode = SignatureMode.EXPANDED;

	private enum SignatureMode {RESTRICTED, EXPANDING, EXPANDED}

	private class PatternSignatureRelationCollector extends SignatureRelationCollector {

		private Set<Relation> initialCollected;
		private boolean additions = false;

		PatternSignatureRelationCollector() {

			this(new NodeVisitMonitor(names));
		}

		PatternSignatureRelationCollector(NodeVisitMonitor visitMonitor) {

			super(visitMonitor);

			initialCollected = getCollected();
		}

		Set<Relation> collect() {

			if (signatureMode != SignatureMode.RESTRICTED) {

				for (Name n : names.getNames()) {

					collectFromSubsumers((NodeName)n);
				}
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

	public Pattern(NodeName name) {

		names.add(name);
	}

	public Pattern(Names names) {

		this.names.addAll(names);
	}

	public Pattern(NodeName name, Relation relation) {

		this(name);

		relations.add(relation);
	}

	public Pattern(NodeName name, Collection<Relation> relations) {

		this(name);

		this.relations.addAll(relations);
	}

	public Pattern(Names names, Collection<Relation> relations) {

		this(names);

		this.relations.addAll(relations);
	}

	public NodeName toSingleName() {

		if (names.size() == 1 && relations.isEmpty()) {

			return (NodeName)names.getFirstName();
		}

		return null;
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

	Pattern combineWith(Pattern other) {

		NameSet newNames = new NameSet(names);
		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newNames.addAll(other.names);
		newRelations.addAll(other.relations);

		purgeSubsumers(newNames, names);
		purgeSubsumers(newNames, other.names);

		return new Pattern(newNames, newRelations);
	}

	Pattern extend(Relation extraRelation) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.add(extraRelation);

		return new Pattern(names, newRelations);
	}

	Pattern extend(Collection<Relation> extraRelations) {

		Set<Relation> newRelations = new HashSet<Relation>(relations);

		newRelations.addAll(extraRelations);

		return new Pattern(names, newRelations);
	}

	Names getNames() {

		return names;
	}

	boolean nestedPattern(boolean signature) {

		return !getRelations(signature).isEmpty();
	}

	void registerDefinitionRefedNames() {

		registerAsDefinitionRefed(names, MatchRole.PATTERN_ROOT);

		for (Relation r : relations) {

			r.getProperty().registerAsDefinitionRefed(MatchRole.PATTERN_RELATION);

			registerAsDefinitionRefed(r.getTargetNodes(), MatchRole.PATTERN_VALUE);
		}
	}

	void collectNames(NameCollector collector) {

		collector.collectNames(names);

		if (collector.continueForNextRelationsRank()) {

			collector = collector.forNextRank();

			for (Relation r : getRelations(collector.signature())) {

				r.collectNames(collector);
			}
		}
	}

	boolean subsumes(Pattern p) {

		return subsumesAllNames(p) && subsumesRelations(p);
	}

	boolean subsumesRelations(Pattern p) {

		for (Relation r : relations) {

			if (!subsumesAnySignatureRelation(r, p)) {

				return false;
			}
		}

		return true;
	}

	boolean classifiable(boolean initialPass) {

		for (Name n : names.getNames()) {

			if (((NodeName)n).classifyTargetPatternRoot(initialPass)) {

				return true;
			}
		}

		for (Relation r : getSignatureRelations()) {

			if (r.getProperty().classifyTargetPatternProperty()) {

				for (Name tn : r.getTargetNodes().getNames()) {

					if (((NodeName)tn).classifyTargetPatternValue(initialPass)) {

						return true;
					}
				}
			}
		}

		return false;
	}

	boolean potentialSignatureUpdates() {

		for (Name n : names.getNames()) {

			if (n.anyNewSubsumers(NodeSelector.STRUCTURED)) {

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

	private void registerAsDefinitionRefed(Names regNames, MatchRole role) {

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

	private boolean subsumesAllNames(Pattern p) {

		for (Name n : names.getNames()) {

			if (!subsumesAnyName(n, p)) {

				return false;
			}
		}

		return true;
	}

	private boolean subsumesAnyName(Name n, Pattern p) {

		for (Name pn : p.names.getNames()) {

			if (n.subsumes(pn)) {

				return true;
			}
		}

		return false;
	}

	private boolean subsumesAnySignatureRelation(Relation r, Pattern p) {

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