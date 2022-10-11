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
class DynamicPatternOpsHandler extends DynamicOpsHandler {

	static private SuperClassesResolver superClassesResolver = new SuperClassesResolver();
	static private SubClassesResolver subClassesResolver = new SubClassesResolver();
	static private InstancesResolver instancesResolver = new InstancesResolver();

	static private EquivCheckDefineds equivCheckDefineds = new EquivCheckDefineds();

	static private abstract class ResultsResolver {

		Names resolve(NameSet inferreds, boolean direct) {

			return filter(direct ? toDirects(inferreds) : toAll(inferreds));
		}

		boolean expandable() {

			return true;
		}

		abstract Names getAllLinked(Name n);

		abstract Names filter(Names inferreds);

		private NameSet toDirects(NameSet inferreds) {

			for (Name s : inferreds.copyNames()) {

				inferreds.removeAll(getAllLinked(s));
			}

			return inferreds;
		}

		private NameSet toAll(NameSet inferreds) {

			if (expandable()) {

				for (Name s : inferreds.copyNames()) {

					inferreds.addAll(getAllLinked(s));
				}
			}

			return inferreds;
		}
	}

	static private class SuperClassesResolver extends ResultsResolver {

		Names getAllLinked(Name n) {

			return n.getSupers(false);
		}

		Names filter(Names inferreds) {

			return inferreds;
		}
	}

	static private abstract class SubsResolver extends ResultsResolver {

		Names getAllLinked(Name n) {

			return n.getSubs(getNodeType(), false);
		}

		Names filter(Names inferreds) {

			return inferreds.filterForType(getNodeType());
		}

		abstract Class<? extends NodeName> getNodeType();
	}

	static private class SubClassesResolver extends SubsResolver {

		Class<? extends NodeName> getNodeType() {

			return ClassName.class;
		}
	}

	static private class InstancesResolver extends SubsResolver {

		boolean expandable() {

			return false;
		}

		Class<? extends NodeName> getNodeType() {

			return IndividualName.class;
		}
	}

	static private class EquivCheckDefineds {

		Collection<MatchableNode> deriveFromSubsumeds(NameSet subsumeds) {

			Set<MatchableNode> potentials = new HashSet<MatchableNode>();

			for (Name s : subsumeds.getNames()) {

				findAllFrom((NodeName)s, potentials);
			}

			return potentials;
		}

		private void findAllFrom(NodeName n, Set<MatchableNode> potentials) {

			findFrom(n, potentials);

			for (Name ss : n.getSubs(ClassName.class, false).getNames()) {

				findFrom((NodeName)ss, potentials);
			}
		}

		private void findFrom(NodeName n, Set<MatchableNode> potentials) {

			MatchableNode m = n.getMatchable();

			if (m != null && m.hasDefinitions() && potentials.add(m)) {

				findFrom(m, potentials);
			}
		}

		private void findFrom(MatchableNode m, Set<MatchableNode> potentials) {

			for (NodePattern d : m.getDefinitions()) {

				for (Name n : getDefinitionMatchNames(d).getNames()) {

					if (n instanceof NodeName) {

						findFrom((NodeName)n, potentials);
					}
				}
			}
		}

		private Names getDefinitionMatchNames(NodePattern defn) {

			return new NameCollector(true, false).collectUnranked(defn);
		}
	}

	private PatternSubsumptions subsumptions;
	private DynamicClassifier classifier;

	private NodePattern pattern;

	private MatchableNodes matchables = new MatchableNodes();
	private MatchStructures matchStructures;

	public Names getEquivalents() {

		return pattern != null ? inferEquivs() : Names.NO_NAMES;
	}

	public Names getSupers(boolean direct) {

		return pattern != null ? inferSupers(direct) : Names.NO_NAMES;
	}

	public Names getSubs(boolean direct) {

		return pattern != null ? inferSubs(direct) : Names.NO_NAMES;
	}

	public Names getInstances(boolean direct) {

		return pattern != null ? inferInstances(direct) : Names.NO_NAMES;
	}

	DynamicPatternOpsHandler(
		PatternSubsumptions subsumptions,
		DynamicClassifier classifier,
		PatternCreator patternCreator) {

		this.subsumptions = subsumptions;
		this.classifier = classifier;

		matchStructures = createMatchStructures();
		pattern = patternCreator.createNestedPatterns(matchStructures);

		matchables.addAllInverseRelations();
	}

	DynamicOpsHandler checkResolveToNodeOpsHandler() {

		if (pattern != null) {

			NodeName n = pattern.toSingleName();

			if (n != null) {

				return new DynamicNodeOpsHandler(n);
			}
		}

		return this;
	}

	Collection<NodePattern> getProfiles() {

		return pattern != null
				? Collections.singletonList(pattern)
				: Collections.emptyList();
	}

	Collection<NodePattern> getDefinitions() {

		return getProfiles();
	}

	private Names inferEquivs() {

		NameSet subsumeds = inferSubsumedClasses();

		if (subsumeds.isEmpty()) {

			return Names.NO_NAMES;
		}

		return inferEquivsForSubsumeds(subsumeds);
	}

	private Names inferSupers(boolean direct) {

		NameSet subsumers = inferSubsumers(null);

		if (subsumers.isEmpty()) {

			return subsumers;
		}

		NameSet equivs = inferSubsumedClasses(subsumers);

		if (!equivs.isEmpty()) {

			return equivs.getFirstName().getSupers(direct);
		}

		return superClassesResolver.resolve(subsumers, direct);
	}

	private Names inferSubs(boolean direct) {

		NameSet subsumeds = inferSubsumedClasses();

		if (subsumeds.isEmpty()) {

			return Names.NO_NAMES;
		}

		Names equivs = inferEquivsForSubsumeds(subsumeds);

		if (!equivs.isEmpty()) {

			return equivs.getFirstName().getSubs(ClassName.class, direct);
		}

		return subClassesResolver.resolve(subsumeds, direct);
	}

	private Names inferInstances(boolean direct) {

		NameSet subs = inferSubsumedInstances();

		if (subs.isEmpty()) {

			return Names.NO_NAMES;
		}

		return instancesResolver.resolve(subs, direct);
	}

	private Names inferEquivsForSubsumeds(NameSet subsumeds) {

		Names subsumers = inferSubsumersForSubsumeds(subsumeds);

		if (subsumers.isEmpty()) {

			return Names.NO_NAMES;
		}

		NameSet equivs = new NameSet(subsumeds);

		equivs.retainAll(subsumers);

		return equivs;
	}

	private Names inferSubsumersForSubsumeds(NameSet subsumeds) {

		return inferSubsumers(equivCheckDefineds.deriveFromSubsumeds(subsumeds));
	}

	private NameSet inferSubsumers(Collection<MatchableNode> preFilteredDefineds) {

		ClassName n = matchStructures.addPatternClass(pattern);

		classifier.classify(matchables, preFilteredDefineds);

		return n.getClassifier().getSubsumers();
	}

	private NameSet inferSubsumedClasses() {

		return subsumptions.inferSubsumedClasses(pattern);
	}

	private NameSet inferSubsumedClasses(NameSet filterNames) {

		return subsumptions.inferSubsumedClasses(pattern, filterNames);
	}

	private NameSet inferSubsumedInstances() {

		return subsumptions.inferSubsumedInstances(pattern);
	}

	private MatchStructures createMatchStructures() {

		return new MatchStructures(matchables, new DynamicPatternClasses());
	}
}
