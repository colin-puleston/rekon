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

import java.io.*;
import java.util.*;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
public class RekonLogger {

	static final RekonLogger SINGLETON = new RekonLogger();

	static private final String LOGGING_SYSTEM_PROPERTY = "rekon.logging";

	static private boolean loggingOn = false;
	static private boolean firstLogLine = true;

	static {

		loggingOn = Boolean.valueOf(System.getProperty(LOGGING_SYSTEM_PROPERTY));
	}

	static public void setLoggingOn(boolean on) {

		loggingOn = on;
	}

	void logExecutionPoint(String text) {

		System.out.println("\nREKON: " + text);
	}

	void logOutOfScopeAxiomTypes(Set<AxiomType<?>> outOfScopeTypes) {

		logWarningLine("Ignoring out-of-scope axiom types");

		for (AxiomType<?> axType : outOfScopeTypes) {

			logLine("Axiom-type: " + axType);
		}

		logSeparatorLine();
	}

	void logOutOfScopeAxiom(OWLAxiom axiom, OWLObject outOfScopeExpr) {

		logOutOfScopeAxiom(axiom, Collections.singleton(outOfScopeExpr));
	}

	void logOutOfScopeAxiom(OWLAxiom axiom, Collection<? extends OWLObject> outOfScopeExprs) {

		logWarningLine("Ignoring axiom containing out-of-scope expression(s)");
		logLine("Axiom: " + axiom);

		for (OWLObject e : outOfScopeExprs) {

			logLine("Expression: " + e);
		}

		logSeparatorLine();
	}

	void logOutOfScopeQueryExpression(OWLObject expr) {

		logWarningLine("Cannot handle out-of-scope query");
		logLine("Expression: " + expr);
		logLine("No results returned!");

		logSeparatorLine();
	}

	void logNoValueAxiomExpressionReplacement(
				OWLAxiom axiom,
				OWLClassExpression replaced,
				OWLClassExpression replacement) {

		logNoValueExpressionReplacement(axiom, replaced, replacement, "axiom/expression");
	}

	void logNoValueQueryExpressionReplacement(
				OWLClassExpression replaced,
				OWLClassExpression replacement) {

		logNoValueExpressionReplacement(null, replaced, replacement, "query");
	}

	private void logNoValueExpressionReplacement(
					OWLAxiom axiom,
					OWLClassExpression replaced,
					OWLClassExpression replacement,
					String description) {

		logWarningLine("Replacing out-of-scope \"No Value\" " + description);

		if (axiom != null) {

			logLine("Axiom: " + axiom);
		}

		logLine("Replacing: " + replaced);
		logLine("Replacement: " + replacement);
		logLine("CAUTION!!! Weaker constraint. No contradiction checking.");

		logSeparatorLine();
	}

	private void logWarningLine(String warning) {

		logLine("REKON WARNING: " + warning + "...");
	}

	private void logSeparatorLine() {

		logLine("");
	}

	private void logLine(String line) {

		if (loggingOn) {

			if (firstLogLine) {

				firstLogLine = false;

				logSeparatorLine();
			}

			System.out.println(line);
		}
	}
}
