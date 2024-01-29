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

import rekon.build.*;

/**
 * @author Colin Puleston
 */
class OwlBuildLogger implements BuildLogger {

	static private final String CLASS_DESCRIPTION = "CLASS";
	static private final String EQUIV_DESCRIPTION = "EQUIVALENT";
	static private final String SUPER_DESCRIPTION = "SUPER";
	static private final String INDIVIDUAL_DESCRIPTION = "INDIVIDUAL";
	static private final String TYPE_DESCRIPTION = "TYPE";
	static private final String VALUE_DESCRIPTION = "VALUE";

	static private final String EQUIV_AXIOM_DESCRIPTION = "EQUIVALENT-CLASSES";
	static private final String SUBCLASS_AXIOM_DESCRIPTION = "SUB-CLASS-OF";

	static private final String AXIOM_GENERIC_DESCRIPTION = "AXIOM";

	public void logOutOfScopeEquivalent(InputClass cls, InputExpression equiv) {

		logOutOfScopeRef(CLASS_DESCRIPTION, EQUIV_DESCRIPTION, cls, equiv);
	}

	public void logOutOfScopeSuper(InputClass cls, InputExpression sup) {

		logOutOfScopeRef(CLASS_DESCRIPTION, SUPER_DESCRIPTION, cls, sup);
	}

	public void logOutOfScopeType(InputIndividual ind, InputExpression type) {

		logOutOfScopeRef(INDIVIDUAL_DESCRIPTION, TYPE_DESCRIPTION, ind, type);
	}

	public void logOutOfScopeValue(InputIndividual ind, InputRelation value) {

		logOutOfScopeRef(INDIVIDUAL_DESCRIPTION, VALUE_DESCRIPTION, ind, value);
	}

	public void logOutOfScopeEquivalence(InputEquivalence equivs) {

		logOutOfScopeAxiom(EQUIV_AXIOM_DESCRIPTION, equivs);
	}

	public void logOutOfScopeSubSuper(InputSubSuper subSuper) {

		logOutOfScopeAxiom(SUBCLASS_AXIOM_DESCRIPTION, subSuper);
	}

	private void logOutOfScopeRef(
					String referDesc,
					String refedDesc,
					InputName<?> refer,
					InputObject refed) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(referDesc + " " + refedDesc);
		logger.logLine(referDesc + ": " + refer.getName());
		logger.logLine(refedDesc + ": " + refed.getSourceObject());
		logger.logSeparatorLine();
	}

	private void logOutOfScopeAxiom(String axiomDesc, InputObject axiom) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(axiomDesc);
		logger.logLine(AXIOM_GENERIC_DESCRIPTION + ": " + axiom.getSourceObject());
		logger.logSeparatorLine();
	}
}
