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

package rekon.owl;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.*;

/**
 * @author Colin Puleston
 */
public class RekonInstanceBox {

	private InstanceOps instanceOps;
	private MappedNames mappedNames;
	private OwlInputObjects inputObjects;

	private CoreBuilder instancesCoreBuilder;
	private CoreBuilder queriesCoreBuilder;

	private Map<IRI, MappedInstance> instances = new HashMap<IRI, MappedInstance>();

	private class MappedInstance extends Instance {

		final IRI iri;

		public Object getInstanceId() {

			return iri;
		}

		public String getLabel() {

			return iri.toURI().getFragment();
		}

		MappedInstance(IRI iri) {

			this.iri = iri;

			instances.put(iri, this);
		}
	}

	private class InstanceBoxBuildCustomiser implements BuildCustomiser {

		private boolean queries;

		public NodeX checkToCustomAtomicNode(InputExpression source) {

			Object owlSource = source.getSourceObject();

			if (owlSource instanceof RekonOWLInstanceRef) {

				return toInstanceNode((RekonOWLInstanceRef)owlSource);
			}

			return null;
		}

		InstanceBoxBuildCustomiser(boolean queries) {

			this.queries = queries;
		}

		private InstanceNode toInstanceNode(RekonOWLInstanceRef source) {

			IRI iri = source.getIRI();
			Instance i = instances.get(iri);

			if (i == null) {

				if (queries) {

					throw new RekonInstanceBoxException("Instance does not exist: " + iri);
				}

				i = new MappedInstance(iri);

				instanceOps.add(i);
			}

			return i.getNode();
		}
	}

	public synchronized void add(IRI iri, OWLClassExpression profile) {

		instanceOps.add(new MappedInstance(iri), createInstanceExprBuilder(profile));
	}

	public synchronized void remove(IRI iri) {

		MappedInstance instance = instances.remove(iri);

		if (instance == null) {

			throw new RekonInstanceBoxException("Instance does not exist: " + iri);
		}

		instanceOps.remove(instance);
	}

	public synchronized List<IRI> match(OWLClassExpression query) {

		return extractIRIs(instanceOps.match(createQueryExprBuilder(query)));
	}

	public synchronized boolean matches(OWLClassExpression query, OWLClassExpression profile) {

		return instanceOps.matches(
					createQueryExprBuilder(query),
					createInstanceExprBuilder(profile));
	}

	public OWLClassExpression createInstanceRef(IRI iri) {

		return new RekonOWLInstanceRef(iri);
	}

	RekonInstanceBox(
		InstanceOps instanceOps,
		MappedNames mappedNames,
		OwlInputObjects inputObjects) {

		this.instanceOps = instanceOps;
		this.mappedNames = mappedNames;
		this.inputObjects = inputObjects;

		instancesCoreBuilder = createCoreBuilder(false);
		queriesCoreBuilder = createCoreBuilder(true);
	}

	private CoreBuilder createCoreBuilder(boolean queries) {

		return new CoreBuilder(mappedNames, new InstanceBoxBuildCustomiser(queries));
	}

	private SinglePatternBuilder createInstanceExprBuilder(OWLClassExpression expr) {

		return instancesCoreBuilder.createSinglePatternBuilder(toInputExpression(expr));
	}

	private MultiPatternBuilder createQueryExprBuilder(OWLClassExpression expr) {

		return queriesCoreBuilder.createMultiPatternBuilder(toInputExpression(expr));
	}

	private InputExpression toInputExpression(OWLClassExpression expr) {

		return inputObjects.createExpression(expr);
	}

	private List<IRI> extractIRIs(Collection<Instance> instances) {

		List<IRI> iris = new ArrayList<IRI>();

		for (Instance inst : instances) {

			iris.add(((MappedInstance)inst).iri);
		}

		return iris;
	}
}
