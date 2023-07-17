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
public class InstanceOps {

	private PatternSubsumers patternSubsumers;
	private PatternSubsumeds patternSubsumeds;

	private Map<Object, Instance> ghosts = new HashMap<Object, Instance>();

	private class Updater {

		private Set<Instance> added = new HashSet<Instance>();

		void add(Instance instance, PatternCreator profileCreator) {

			InstanceName name = instance.getName();
			InstancePattern ip = new InstancePattern(name, profileCreator);

			patternSubsumers.inferSubsumers(ip);
			name.completeClassification();

			patternSubsumeds.checkAddInstanceOption(name);

			if (instance.addReferencers()) {

				instance.setProfileRecreator(profileCreator);
			}

			added.add(instance);
			updateReferencers(instance);
		}

		void remove(Instance instance) {

			InstanceName name = instance.getName();

			name.removeFromClassification();
			patternSubsumeds.checkRemoveInstanceOption(name);

			instance.removeFromReferenceds();

			updateReferencers(instance);
		}

		private void updateReferencers(Instance instance) {

			for (Instance r : instance.getReferencers()) {

				if (!added.contains(r)) {

					remove(r);
					r.reset();
					add(r, r.getProfileRecreator());
				}
			}
		}
	}

	public InstanceOps(Ontology ontology) {

		patternSubsumers = ontology.getPatternSubsumers();
		patternSubsumeds = ontology.getPatternSubsumeds();
	}

	public void add(Instance instance) {

		registerAsGhost(instance);
	}

	public void add(Instance instance, PatternCreator profileCreator) {

		checkReplaceGhost(instance);

		new Updater().add(instance, profileCreator);
	}

	public void remove(Instance instance) {

		new Updater().remove(instance);

		checkRegisterAsGhost(instance);
	}

	public List<Instance> match(PatternCreator queryCreator) {

		Pattern qp = createDynamicPattern(queryCreator);
		NameSet matches = patternSubsumeds.inferAllSubsumedNodes(qp);

		return matches.isEmpty()
				? Collections.emptyList()
				: matchesToInstances(matches);
	}

	public boolean matches(PatternCreator queryCreator, PatternCreator profileCreator) {

		Pattern qp = createDynamicPattern(queryCreator);
		Pattern pp = createDynamicPattern(profileCreator);

		return qp.subsumes(pp);
	}

	private void checkReplaceGhost(Instance addition) {

		Instance ghost = ghosts.remove(addition.getInstanceId());

		if (ghost != null) {

			addition.replaceGhost(ghost);
		}
	}

	private void checkRegisterAsGhost(Instance removal) {

		if (removal.anyReferencers()) {

			registerAsGhost(removal);
		}
	}

	private void registerAsGhost(Instance instance) {

		ghosts.put(instance.getInstanceId(), instance);
	}

	private Pattern createDynamicPattern(PatternCreator creator) {

		return new DynamicPattern(creator).getPattern();
	}

	private List<Instance> matchesToInstances(NameSet matches) {

		Set<Instance> instances = new HashSet<Instance>();

		addInstances(instances, matches.filterForType(InstanceName.class));

		for (Name n : matches.getNames()) {

			if (n instanceof ClassName) {

				addInstances(instances, n.getSubs(InstanceName.class, false));
			}
		}

		return new ArrayList<Instance>(instances);
	}

	private void addInstances(Set<Instance> instances, Names names) {

		for (Name n : names.getNames()) {

			instances.add(((InstanceName)n).getInstance());
		}
	}
}
