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
import org.semanticweb.owlapi.reasoner.*;

/**
 * @author Colin Puleston
 */
public class RekonReasonerFactory implements OWLReasonerFactory {

	private List<RekonStartupListener> reasonerStartupListeners
								= new ArrayList<RekonStartupListener>();

	public void addReasonerStartupListener(RekonStartupListener listener) {

		reasonerStartupListeners.add(listener);
	}

	public String getReasonerName() {

		return RekonReasoner.REASONER_NAME;
	}

	public OWLReasoner createReasoner(OWLOntology ontology) {

		RekonReasoner reasoner = new RekonReasoner(ontology);

		reasoner.addStartupListeners(reasonerStartupListeners);

		return reasoner;
	}

 	public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) {

 		return createReasoner(ontology);
 	}

	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {

		throw new UnsupportedOperationException();
	}

	public OWLReasoner createNonBufferingReasoner(
							OWLOntology ontology,
							OWLReasonerConfiguration config) {

		throw new UnsupportedOperationException();
	}
}
