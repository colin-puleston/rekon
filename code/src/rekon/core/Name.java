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

	private NameLinksHandler linksHandler;
	private Set<MatchRole> definitionRoles = new HashSet<MatchRole>();

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

	public boolean rootName() {

		return false;
	}

	public boolean mapped() {

		return true;
	}

	public Names getEquivalents() {

		return linksHandler.getEquivalents();
	}

	public Names getSupers(boolean direct) {

		return linksHandler.getSupers(direct);
	}

	public Names getSubs(boolean direct) {

		return linksHandler.getSubs(direct);
	}

	public Names getSubs(Class<? extends Name> type, boolean direct) {

		return getSubs(direct).filterForType(type);
	}

	Name() {

		linksHandler = createInitialLinksHandler();
	}

	void setClassification() {

		linksHandler = getClassifier().createClassification();
	}

	void resetClassifier() {

		linksHandler = createInitialLinksHandler();
	}

	void configureAsRootName(Collection<? extends Name> allSubs) {

		getRootLinksHandler().configure(allSubs);
	}

	void registerAsDefinitionRefed(MatchRole role) {

		definitionRoles.add(role);
	}

	boolean classified() {

		return !(linksHandler instanceof NameClassifier);
	}

	NameClassifier getClassifier() {

		return getLinksHandler(NameClassifier.class);
	}

	NameClassification getClassification() {

		return getLinksHandler(NameClassification.class);
	}

	boolean local() {

		return false;
	}

	boolean definitionRefed() {

		return !definitionRoles.isEmpty();
	}

	boolean definitionRefed(MatchRole role) {

		return definitionRoles.contains(role);
	}

	Names getSubsumers() {

		return linksHandler.getSubsumers();
	}

	boolean subsumes(Name name) {

		return rootName() || name == this || name.hasSubsumer(this);
	}

	boolean anyNewSubsumers() {

		return anyNewSubsumers(NodeSelector.ANY);
	}

	boolean anyNewSubsumers(NodeSelector selector) {

		return linksHandler.anyNewSubsumers(selector);
	}

	NameClassifier createClassifier() {

		return new NameClassifier(this);
	}

	private NameLinksHandler createInitialLinksHandler() {

		return rootName() ? new RootNameLinksHandler() : createClassifier();
	}

	private boolean hasSubsumer(Name name) {

		return linksHandler.hasSubsumer(name);
	}

	private RootNameLinksHandler getRootLinksHandler() {

		return getLinksHandler(RootNameLinksHandler.class);
	}

	private <T extends NameLinksHandler>T getLinksHandler(Class<T> type) {

		if (type.isAssignableFrom(linksHandler.getClass())) {

			return type.cast(linksHandler);
		}

		throw new Error(
					"Unexpected classification-handler type: "
					+ linksHandler.getClass()
					+ ", for: " + this);
	}
}
