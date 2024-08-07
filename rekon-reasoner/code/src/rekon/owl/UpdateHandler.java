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

/**
 * @author Colin Puleston
 */
class UpdateHandler implements OWLOntologyChangeListener {

	private boolean instanceBoxPresent = false;
	private List<OWLOntologyChange> pendingChanges = new ArrayList<OWLOntologyChange>();

	public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {

		checkLegalUpdate();

		pendingChanges.addAll(changes);
	}

	UpdateHandler(OWLOntologyManager manager) {

		manager.addOntologyChangeListener(this);
	}

	void notifyInstanceBoxPresent() {

		instanceBoxPresent = true;
	}

	void reset() {

		pendingChanges.clear();
	}

	List<OWLOntologyChange> getPendingChanges() {

		return pendingChanges;
	}

	Set<OWLAxiom> getPendingAxiomAdditions() {

		return getPendingAxiomChanges(AddAxiom.class);
	}

	Set<OWLAxiom> getPendingAxiomRemovals() {

		return getPendingAxiomChanges(RemoveAxiom.class);
	}

	private Set<OWLAxiom> getPendingAxiomChanges(Class<? extends OWLAxiomChange> type) {

		Set<OWLAxiom> changesOfType = new HashSet<OWLAxiom>();

		for (OWLOntologyChange c : pendingChanges) {

			if (type.isAssignableFrom(c.getClass())) {

				changesOfType.add(type.cast(c).getAxiom());
			}
		}

		return changesOfType;
	}

	private void checkLegalUpdate() {

		if (instanceBoxPresent) {

			throw new RekonInstanceBoxException(
						"Cannot edit ontology after "
						+ "instance-box has been created!");
		}
	}
}
