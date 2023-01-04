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

	static private abstract class ResultsResolver {

		Names resolve(NameSet inferreds, boolean direct) {

			return filter(direct ? toDirects(inferreds) : toAll(inferreds));
		}

		boolean expandable() {

			return true;
		}

		abstract Names getAllLinked(Name n);

		abstract Names filter(Names inferreds);

		private Names toDirects(NameSet inferreds) {

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

	private PatternSubsumersFinder subsumersFinder;
	private PatternSubsumedsFinder subsumedsFinder;

	private DynamicPattern dynamicPattern;
	private NodePattern nodePattern;

	public Names getEquivalents() {

		return nodePattern != null ? inferEquivs() : Names.NO_NAMES;
	}

	public Names getSupers(boolean direct) {

		return nodePattern != null ? inferSupers(direct) : Names.NO_NAMES;
	}

	public Names getSubs(boolean direct) {

		return nodePattern != null ? inferSubs(direct) : Names.NO_NAMES;
	}

	public Names getInstances(boolean direct) {

		return nodePattern != null ? inferInstances(direct) : Names.NO_NAMES;
	}

	DynamicPatternOpsHandler(
		PatternSubsumersFinder subsumersFinder,
		PatternSubsumedsFinder subsumedsFinder,
		PatternCreator patternCreator) {

		this.subsumersFinder = subsumersFinder;
		this.subsumedsFinder = subsumedsFinder;

		dynamicPattern = new DynamicPattern(patternCreator);
		nodePattern = dynamicPattern.getPattern();
	}

	DynamicOpsHandler checkResolveToNodeOpsHandler() {

		if (nodePattern != null) {

			NodeName n = nodePattern.toSingleName();

			if (n != null) {

				return new DynamicNodeOpsHandler(n);
			}
		}

		return this;
	}

	Collection<NodePattern> getProfiles() {

		return nodePattern != null
				? Collections.singletonList(nodePattern)
				: Collections.emptyList();
	}

	Collection<NodePattern> getDefinitions() {

		return getProfiles();
	}

	private Names inferEquivs() {

		NameSet subsumeds = inferSubsumedClasses();

		if (subsumeds.isEmpty()) {

			return NameSet.NO_NAMES;
		}

		return inferEquivsForSubsumeds(subsumeds);
	}

	private Names inferSupers(boolean direct) {

		NameSet subsumers = inferSubsumers();

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

			return NameSet.NO_NAMES;
		}

		NameSet equivs = inferEquivsForSubsumeds(subsumeds);

		if (!equivs.isEmpty()) {

			return equivs.getFirstName().getSubs(ClassName.class, direct);
		}

		return subClassesResolver.resolve(subsumeds, direct);
	}

	private Names inferInstances(boolean direct) {

		NameSet subs = inferSubsumedInstances();

		if (subs.isEmpty()) {

			return NameSet.NO_NAMES;
		}

		return instancesResolver.resolve(subs, direct);
	}

	private NameSet inferEquivsForSubsumeds(NameSet subsumeds) {

		NameSet subsumers = inferSubsumersForSubsumeds(subsumeds);

		if (subsumers.isEmpty()) {

			return NameSet.NO_NAMES;
		}

		NameSet equivs = new NameSet(subsumeds);

		equivs.retainAll(subsumers);

		return equivs;
	}

	private NameSet inferSubsumers() {

		return subsumersFinder.inferSubsumers(dynamicPattern);
	}

	private NameSet inferSubsumersForSubsumeds(NameSet subsumeds) {

		return subsumersFinder.inferSubsumersForSubsumeds(dynamicPattern, subsumeds);
	}

	private NameSet inferSubsumedClasses() {

		return subsumedsFinder.inferSubsumedClasses(nodePattern);
	}

	private NameSet inferSubsumedClasses(NameSet filterNames) {

		return subsumedsFinder.inferSubsumedClasses(nodePattern, filterNames);
	}

	private NameSet inferSubsumedInstances() {

		return subsumedsFinder.inferSubsumedInstances(nodePattern);
	}
}
