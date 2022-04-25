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

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
abstract class Name {

	private NameClassifier classifier = new NameClassifier(this);
	private NameClassification classification = null;

	public String toString() {

		return getClass().getSimpleName() + "(" + getLabel() + ")";
	}

	void completeClassification() {

		classification = classifier.createClassification();

		classifier = null;
	}

	NameClassifier getClassifier() {

		if (classifier == null) {

			throw new Error("Cannot access classifier post-classification: " + this);
		}

		return classifier;
	}

	NameClassification getClassification() {

		if (classification == null) {

			throw new Error("Cannot access classification pre-classification: " + this);
		}

		return classification;
	}

	boolean mapped() {

		return true;
	}

	boolean dynamic() {

		return false;
	}

	void collectNames(NameSet collected) {

		collected.add(this);
	}

	abstract OWLEntity getOWLEntity();

	<E extends OWLEntity>E getOWLEntity(Class<E> type) {

		return type.cast(getOWLEntity());
	}

	String getLabel() {

		return NameRenderer.SINGLETON.render(getOWLEntity());
	}

	boolean reclassifiable() {

		return classifier != null && classifier.newInferreds();
	}

	Names getSubsumers() {

		return classifier != null ? classifier.getSubsumers() : classification.getSubsumers();
	}

	boolean subsumes(Name name) {

		return name == this || name.hasSubsumer(this);
	}

	Names getEquivalents() {

		return getClassification().getEquivalents();
	}

	Names getSupers(boolean direct) {

		return getClassification().getSupers(direct);
	}

	Names getSubs(Class<? extends Name> type, boolean direct) {

		return getClassification().getSubs(type, direct);
	}

	boolean rootName() {

		return classifier != null ? classifier.rootName() : classification.rootName();
	}

	boolean leafName() {

		return getClassification().leafName();
	}

	private boolean hasSubsumer(Name name) {

		return classifier != null
				? classifier.isSubsumer(name)
				: classification.isSubsumer(name);
	}
}
