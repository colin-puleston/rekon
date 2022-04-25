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
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.Version;

/**
 * @author Colin Puleston
 */
public class RekonReasoner extends UnsupportedOWLReasonerOps implements OWLReasoner {

	static final String REASONER_NAME = "REKON";
	static final Version REASONER_VERSION = new Version(1, 0, 0, 0);

	private OWLOntologyManager manager;
	private RekonOps operations = null;

	private class FlushOnUpdateInvoker implements OWLOntologyChangeListener {

		public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {

			flush();
		}
	}

	public RekonReasoner(OWLOntology rootOntology) {

		manager = rootOntology.getOWLOntologyManager();

		manager.addOntologyChangeListener(new FlushOnUpdateInvoker());

		NameRenderer.SINGLETON.setDefaultLabel(manager);
	}

	public String getReasonerName() {

		return REASONER_NAME;
	}

	public Version getReasonerVersion() {

		return REASONER_VERSION;
	}

	public void precomputeInferences(InferenceType... types) {

		resolveOperations();
	}

	public Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		return resolveOperations().getEquivalentClasses(expr);
	}

	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		return resolveOperations().getSuperClasses(expr, direct);
	}

	public NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		return resolveOperations().getSubClasses(expr, direct);
	}

	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression expr, boolean direct) {

		return resolveOperations().getInstances(expr, direct);
	}

	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		return resolveOperations().getTypes(ind, direct);
	}

	public boolean isEntailed(OWLAxiom axiom) {

		return resolveOperations().isEntailed(axiom);
	}

	public void flush() {

		operations = null;
	}

	private RekonOps resolveOperations() {

		if (operations == null) {

			operations = new RekonOps(manager);
		}

		return operations;
	}
}
