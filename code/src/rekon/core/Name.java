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

	private NameClassificationHandler classificationHandler = new NameClassifier(this);
	private Set<PatternNameRole> definitionRoles = new HashSet<PatternNameRole>();

	public void addSubsumer(Name subsumer) {

		getClassifier().addAssertedSubsumer(subsumer);
	}

	public void addEquivalent(Name equiv) {

		getClassifier().addAssertedEquivalent(equiv);
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

	public Names getSubs(boolean direct) {

		return getClassification().getSubs(direct);
	}

	public Names getSubs(Class<? extends Name> type, boolean direct) {

		return getSubs(direct).filterForType(type);
	}

	public boolean rootName() {

		return classificationHandler.rootName();
	}

	void registerAsDefinitionRefed(PatternNameRole role) {

		definitionRoles.add(role);
	}

	void setClassification() {

		classificationHandler = getClassifier().createClassification();
	}

	NameClassifier getClassifier() {

		return getClassificationHandler(NameClassifier.class);
	}

	NameClassification getClassification() {

		return getClassificationHandler(NameClassification.class);
	}

	boolean classified() {

		return classificationHandler.classified();
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

		return classificationHandler.getSubsumers();
	}

	boolean subsumes(Name name) {

		return name == this || name.hasSubsumer(this);
	}

	boolean anyDefinitionRefs() {

		if (definitionRefed()) {

			return true;
		}

		for (Name s : getSubsumers().getNames()) {

			if (s.definitionRefed()) {

				return true;
			}
		}

		return false;
	}

	private boolean hasSubsumer(Name name) {

		return classificationHandler.isSubsumer(name);
	}

	private <T extends NameClassificationHandler>T getClassificationHandler(Class<T> type) {

		if (classificationHandler.getClass() == type) {

			return type.cast(classificationHandler);
		}

		throw new Error(
					"Unexpected classification-handler type: "
					+ classificationHandler.getClass()
					+ ", for: " + this);
	}
}
