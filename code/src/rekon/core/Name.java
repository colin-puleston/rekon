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
public abstract class Name {

	private NameClassifier classifier = new NameClassifier(this);
	private NameClassification classification = null;

	private Set<PatternNameRole> definitionRoles = new HashSet<PatternNameRole>();

	public void addSubsumer(Name subsumer) {

		classifier.addAssertedSubsumer(subsumer);
	}

	public void addEquivalent(Name equiv) {

		classifier.addAssertedEquivalent(equiv);
	}

	public String toString() {

		return getClass().getSimpleName() + "(" + getLabel() + ")";
	}

	public abstract String getLabel();

	public boolean mapped() {

		return true;
	}

	public Names getEquivalents() {

		return getClassification().getEquivalents();
	}

	public Names getSupers(boolean direct) {

		return getClassification().getSupers(direct);
	}

	public Names getSubs(Class<? extends Name> type, boolean direct) {

		return getClassification().getSubs(type, direct);
	}

	public boolean rootName() {

		return classifier != null ? classifier.rootName() : classification.rootName();
	}

	void registerAsDefinitionRefed(PatternNameRole role) {

		definitionRoles.add(role);
	}

	void setClassification() {

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

	boolean classified() {

		return classification != null;
	}

	boolean dynamic() {

		return false;
	}

	boolean definitionRefed() {

		return !definitionRoles.isEmpty();
	}

	boolean definitionRefed(PatternNameRole role) {

		return definitionRoles.contains(role);
	}

	Names getSubsumers() {

		return classifier != null ? classifier.getSubsumers() : classification.getSubsumers();
	}

	boolean subsumes(Name name) {

		return name == this || name.hasSubsumer(this);
	}

	boolean newSubsumers(NodeMatcher matcher) {

		return false;
	}

	private boolean hasSubsumer(Name name) {

		return classifier != null
				? classifier.isSubsumer(name)
				: classification.isSubsumer(name);
	}
}
