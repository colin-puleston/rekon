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

package rekon;

import java.util.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
class DynamicPatternOps {

	static private SuperClassesResolver superClassesResolver = new SuperClassesResolver();
	static private SubClassesResolver subClassesResolver = new SubClassesResolver();
	static private InstancesResolver instancesResolver = new InstancesResolver();

	static private PotentialEquivalents potentialEquivalents = new PotentialEquivalents();

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

	private Ontology ontology;

	private PatternSubsumptions patternSubsumptions;
	private DynamicClassifier dynamicClassifier;

	private class PatternOpsHandler extends DynamicOpsHandler {

		private NodePattern pattern;

		private MatchableNodes dynamicMatchables = new MatchableNodes();
		private FreeClasses dynamicClasses = FreeClasses.forDynamicExprs(dynamicMatchables);

		PatternOpsHandler(OWLClassExpression expr) {

			pattern = createPatternComponents().toNodePattern(expr);
		}

		DynamicOpsHandler checkResolveToNodeOpsHandler() {

			if (pattern != null) {

				NodeName n = pattern.asNodeName();

				if (n != null) {

					return new DynamicNodeOpsHandler(n);
				}
			}

			return this;
		}

		NodeName patternAsNodeName(NodePattern pattern) {

			return pattern.asNodeName();
		}

		Names getEquivalents() {

			return pattern != null ? inferEquivs() : Names.NO_NAMES;
		}

		Names getSupers(boolean direct) {

			return pattern != null ? inferSupers(direct) : Names.NO_NAMES;
		}

		Names getSubs(boolean direct) {

			return pattern != null ? inferSubs(direct) : Names.NO_NAMES;
		}

		Names getInstances(boolean direct) {

			return pattern != null ? inferInstances(direct) : Names.NO_NAMES;
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

			return inferSubsumers(potentialEquivalents.getPotentials(subsumeds));
		}

		private NameSet inferSubsumers(Collection<MatchableNode> preFilteredDefineds) {

			ClassName n = dynamicClasses.create(pattern);

			dynamicClassifier.classify(dynamicMatchables, preFilteredDefineds);

			return n.getClassifier().getSubsumers();
		}

		private NameSet inferSubsumedClasses() {

			return patternSubsumptions.inferSubsumedClasses(pattern);
		}

		private NameSet inferSubsumedClasses(NameSet filterNames) {

			return patternSubsumptions.inferSubsumedClasses(pattern, filterNames);
		}

		private NameSet inferSubsumedInstances() {

			return patternSubsumptions.inferSubsumedInstances(pattern);
		}

		private PatternComponents createPatternComponents() {

			return new PatternComponents(ontology.getMappedNames(), dynamicClasses, true);
		}
	}

	DynamicPatternOps(Ontology ontology) {

		this.ontology = ontology;

		patternSubsumptions = new PatternSubsumptions(ontology);
		dynamicClassifier = new DynamicClassifier(ontology);
	}

	DynamicOpsHandler createHandler(OWLClassExpression expr) {

		return new PatternOpsHandler(expr).checkResolveToNodeOpsHandler();
	}
}
