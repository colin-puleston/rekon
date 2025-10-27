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

package rekon.owl.convert;

import org.semanticweb.owlapi.model.*;

/**
 * @author Colin Puleston
 */
abstract class OwlContainer {

	static private WarningLogger WARNINGS = WarningLogger.SINGLETON;

	static private class OwlContainerAxiom extends OwlContainer {

		private OWLAxiom axiom;

		OwlContainerAxiom(OWLAxiom axiom) {

			this.axiom = axiom;
		}

		void logOutOfScope(OWLClassExpression expr, OutOfScopeExplanation explanation) {

			WARNINGS.logOutOfScopeAxiom(axiom, expr, explanation);
		}

		void logNoValueExprReplacement(OWLClassExpression replaced, OWLRestriction replacement) {

			WARNINGS.logNoValueAxiomExprReplacement(axiom, replaced, replacement);
		}
	}

	static private class OwlContainerQuery extends OwlContainer {

		private OWLClassExpression query;

		OwlContainerQuery(OWLClassExpression query) {

			this.query = query;
		}

		void logOutOfScope(OWLClassExpression expr, OutOfScopeExplanation explanation) {

			WARNINGS.logOutOfScopeQueryExpr(query, expr, explanation);
		}

		void logNoValueExprReplacement(OWLClassExpression replaced, OWLRestriction replacement) {

			WARNINGS.logNoValueQueryExprReplacement(query, replaced, replacement);
		}
	}

	static OwlContainer asContainer(OWLAxiom axiom) {

		return new OwlContainerAxiom(axiom);
	}

	static OwlContainer asContainer(OWLClassExpression query) {

		return new OwlContainerQuery(query);
	}

	private boolean outOfScope = false;

	void checkLogOutOfScope(OWLClassExpression expr, OutOfScopeExplanation explanation) {

		if (!outOfScope) {

			logOutOfScope(expr, explanation);

			outOfScope = true;
		}
	}

	abstract void logOutOfScope(OWLClassExpression expr, OutOfScopeExplanation explanation);

	abstract void logNoValueExprReplacement(OWLClassExpression replaced, OWLRestriction replacement);
}
