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

/**
 * @author Colin Puleston
 */
public class RekonInstanceBox {

	private InstanceOps instanceOps;
	private MappedNames mappedNames;

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

	private abstract class InstanceBoxExprPatternCreator extends SinglePatternCreator {

		private OWLClassExpression expr;

		private class InstanceBoxMatchComponents extends MatchComponents {

			InstanceBoxMatchComponents(MatchStructures matchStructures) {

				super(mappedNames, matchStructures, true);
			}

			InstanceNode toInstanceNode(RekonOWLInstanceRef source) {

				IRI iri = source.getIRI();
				Instance i = instances.get(iri);

				if (i == null) {

					i = checkCreateRefedInstance(iri);
				}

				return i.getNode();
			}
		}

		protected Pattern create(MatchStructures matchStructures) {

			return new InstanceBoxMatchComponents(matchStructures).toPattern(expr);
		}

		InstanceBoxExprPatternCreator(OWLClassExpression expr) {

			this.expr = expr;
		}

		abstract Instance checkCreateRefedInstance(IRI iri);
	}

	private class InstanceExprPatternCreator extends InstanceBoxExprPatternCreator {

		InstanceExprPatternCreator(OWLClassExpression expr) {

			super(expr);
		}

		Instance checkCreateRefedInstance(IRI iri) {

			Instance i = new MappedInstance(iri);

			instanceOps.add(i);

			return i;
		}
	}

	private class QueryExprPatternCreator extends InstanceBoxExprPatternCreator {

		QueryExprPatternCreator(OWLClassExpression expr) {

			super(expr);
		}

		Instance checkCreateRefedInstance(IRI iri) {

			throw new RekonInstanceBoxException("Instance does not exist: " + iri);
		}
	}

	public synchronized void add(IRI iri, OWLClassExpression profile) {

		instanceOps.add(new MappedInstance(iri), new InstanceExprPatternCreator(profile));
	}

	public synchronized void remove(IRI iri) {

		MappedInstance instance = instances.remove(iri);

		if (instance == null) {

			throw new RekonInstanceBoxException("Instance does not exist: " + iri);
		}

		instanceOps.remove(instance);
	}

	public synchronized List<IRI> match(OWLClassExpression query) {

		return extractIRIs(instanceOps.match(new QueryExprPatternCreator(query)));
	}

	public synchronized boolean matches(OWLClassExpression query, OWLClassExpression profile) {

		return instanceOps.matches(
					new QueryExprPatternCreator(query),
					new QueryExprPatternCreator(profile));
	}

	public OWLClassExpression createInstanceRef(IRI iri) {

		return new RekonOWLInstanceRef(iri);
	}

	RekonInstanceBox(InstanceOps instanceOps, MappedNames mappedNames) {

		this.instanceOps = instanceOps;
		this.mappedNames = mappedNames;
	}

	private List<IRI> extractIRIs(Collection<Instance> instances) {

		List<IRI> iris = new ArrayList<IRI>();

		for (Instance inst : instances) {

			iris.add(((MappedInstance)inst).iri);
		}

		return iris;
	}
}
