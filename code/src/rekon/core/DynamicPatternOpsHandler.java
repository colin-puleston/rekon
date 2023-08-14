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
	static private IndividualsResolver individualsResolver = new IndividualsResolver();

	static private abstract class ResultsResolver {

		Names resolve(NameSet inferreds, boolean direct) {

			return direct ? toDirects(inferreds) : toAll(inferreds);
		}

		abstract Names getAllLinked(Name n);

		private Names toDirects(NameSet inferreds) {

			for (Name n : inferreds.copyNames()) {

				inferreds.removeAll(getAllLinked(n));
			}

			return inferreds;
		}

		private NameSet toAll(NameSet inferreds) {

			for (Name s : inferreds.copyNames()) {

				inferreds.addAll(getAllLinked(s));
			}

			return inferreds;
		}
	}

	static private class SuperClassesResolver extends ResultsResolver {

		Names getAllLinked(Name n) {

			return n.getSupers(false);
		}
	}

	static private class SubClassesResolver extends ResultsResolver {

		Names getAllLinked(Name n) {

			return n.getSubs(ClassNode.class, false);
		}
	}

	static private class IndividualsResolver extends ResultsResolver {

		Names resolve(NameSet inferreds, boolean direct) {

			return filter(super.resolve(inferreds, direct));
		}

		Names getAllLinked(Name n) {

			return n.getSubs(NodeX.class, false);
		}

		private Names filter(Names inferreds) {

			return inferreds.filterForType(IndividualNode.class);
		}
	}

	private PatternSubsumers patternSubsumers;
	private PatternSubsumeds patternSubsumeds;

	private DynamicPattern dynamicPattern;
	private Pattern pattern;

	public Names getEquivalents() {

		return pattern != null ? inferEquivs() : Names.NO_NAMES;
	}

	public Names getSupers(boolean direct) {

		return pattern != null ? inferSupers(direct) : Names.NO_NAMES;
	}

	public Names getSubs(boolean direct) {

		return pattern != null ? inferSubs(direct) : Names.NO_NAMES;
	}

	public Names getIndividuals(boolean direct) {

		return pattern != null ? inferIndividuals(direct) : Names.NO_NAMES;
	}

	DynamicPatternOpsHandler(Ontology ontology, PatternCreator patternCreator) {

		patternSubsumers = ontology.getPatternSubsumers();
		patternSubsumeds = ontology.getPatternSubsumeds();

		dynamicPattern = new DynamicPattern(patternCreator);
		pattern = dynamicPattern.getPattern();
	}

	DynamicOpsHandler checkResolveToNodeOpsHandler() {

		if (pattern != null) {

			NodeX n = pattern.toSingleNode();

			if (n != null) {

				return new DynamicNodeOpsHandler(n);
			}
		}

		return this;
	}

	Collection<Pattern> getProfiles() {

		return pattern != null
				? Collections.singletonList(pattern)
				: Collections.emptyList();
	}

	Collection<Pattern> getDefinitions() {

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

			return equivs.getFirstName().getSubs(ClassNode.class, direct);
		}

		return subClassesResolver.resolve(subsumeds, direct);
	}

	private Names inferIndividuals(boolean direct) {

		NameSet subs = inferAllSubsumedNodes();

		if (subs.isEmpty()) {

			return NameSet.NO_NAMES;
		}

		return individualsResolver.resolve(subs, direct);
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

		return patternSubsumers.inferSubsumers(dynamicPattern);
	}

	private NameSet inferSubsumersForSubsumeds(NameSet subsumeds) {

		return patternSubsumers.inferSubsumersForSubsumeds(dynamicPattern, subsumeds);
	}

	private NameSet inferSubsumedClasses() {

		return patternSubsumeds.inferSubsumedClasses(pattern);
	}

	private NameSet inferSubsumedClasses(NameSet filterNames) {

		return patternSubsumeds.inferSubsumedClasses(pattern, filterNames);
	}

	private NameSet inferAllSubsumedNodes() {

		return patternSubsumeds.inferAllSubsumedNodes(pattern);
	}
}
