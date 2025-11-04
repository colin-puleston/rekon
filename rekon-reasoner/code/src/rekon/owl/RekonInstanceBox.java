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
import rekon.build.input.*;
import rekon.owl.convert.*;

/**
 * @author Colin Puleston
 */
public class RekonInstanceBox {

	private InstanceOps instanceOps;
	private DynamicConverter dynamicConverter;

	private BuildCustomiser instanceCustomiser = new InstanceCustomiser();
	private BuildCustomiser queryCustomiser = new QueryCustomiser();

	private Map<IRI, MappedInstance> instances = new HashMap<IRI, MappedInstance>();

	private class MappedInstance extends Instance {

		final IRI iri;

		public Object getInstanceId() {

			return iri;
		}

		public String getLabel() {

			return iri.toURI().getFragment();
		}

		MappedInstance(IRI iri, boolean undefinedRef) {

			super(undefinedRef);

			this.iri = iri;

			instances.put(iri, this);
		}
	}

	private abstract class InstanceBoxBuildCustomiser implements BuildCustomiser {

		public NodeX checkToCustomNode(InputNode source) {

			Object owlSource = source.getSourceObject();

			if (owlSource instanceof RekonInstanceRef) {

				return toInstanceNode((RekonInstanceRef)owlSource);
			}

			return null;
		}

		abstract Instance checkCreateRefedInstance(IRI iri);

		private InstanceNode toInstanceNode(RekonInstanceRef source) {

			IRI iri = source.getIRI();
			Instance i = instances.get(iri);

			if (i == null) {

				i = checkCreateRefedInstance(iri);
			}

			return i.getNode();
		}
	}

	private class InstanceCustomiser extends InstanceBoxBuildCustomiser {

		Instance checkCreateRefedInstance(IRI iri) {

			return new MappedInstance(iri, true);
		}
	}

	private class QueryCustomiser extends InstanceBoxBuildCustomiser {

		Instance checkCreateRefedInstance(IRI iri) {

			throw new RekonInstanceBoxException("Instance does not exist: " + iri);
		}
	}

	public synchronized void add(IRI iri, OWLClassExpression profile) {

		MappedInstance instance = instances.get(iri);

		if (instance != null) {

			if (!instance.undefinedRef()) {

				throw new RekonInstanceBoxException("Instance already exists: " + iri);
			}
		}
		else {

			instance = new MappedInstance(iri, false);
		}

		instanceOps.add(instance, createInstanceExprBuilder(profile));
	}

	public synchronized void remove(IRI iri) {

		MappedInstance instance = instances.get(iri);

		if (instance == null) {

			throw new RekonInstanceBoxException("Instance does not exist: " + iri);
		}

		if (instanceOps.remove(instance)) {

			instances.remove(iri);
		}
	}

	public synchronized List<IRI> match(OWLClassExpression query) {

		try {

			return extractIRIs(instanceOps.match(createQueryExprBuilder(query)));
		}
		catch (InstanceOpsException e) {

			throw new RekonInstanceBoxException(e);
		}
	}

	public synchronized boolean matches(OWLClassExpression query, OWLClassExpression profile) {

		try {

			return instanceOps.matches(
						createQueryExprBuilder(query),
						createInstanceExprBuilder(profile));
		}
		catch (InstanceOpsException e) {

			throw new RekonInstanceBoxException(e);
		}
	}

	public OWLClassExpression createInstanceRef(IRI iri) {

		return new RekonInstanceRef(iri);
	}

	RekonInstanceBox(InstanceOps instanceOps, DynamicConverter dynamicConverter) {

		this.instanceOps = instanceOps;
		this.dynamicConverter = dynamicConverter;
	}

	private DynamicPatternBuilder createInstanceExprBuilder(OWLClassExpression expr) {

		return createExprBuilder(expr, instanceCustomiser);
	}

	private DynamicPatternBuilder createQueryExprBuilder(OWLClassExpression expr) {

		return createExprBuilder(expr, queryCustomiser);
	}

	private DynamicPatternBuilder createExprBuilder(
									OWLClassExpression expr,
									BuildCustomiser customiser) {

		DynamicPatternBuilder bldr = dynamicConverter.toDynamicPatternBuilder(expr);

		bldr.setCustomiser(customiser);

		return bldr;
	}

	private List<IRI> extractIRIs(Collection<Instance> instances) {

		List<IRI> iris = new ArrayList<IRI>();

		for (Instance inst : instances) {

			iris.add(((MappedInstance)inst).iri);
		}

		return iris;
	}
}
