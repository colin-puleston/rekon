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

import rekon.util.*;

/**
 * @author Colin Puleston
 */
class WarningLogger {

	static final WarningLogger SINGLETON = new WarningLogger();
	static final Option OPTION = new Option(false, "logging.warnings");

	void logOutOfScopeAxiomTypes(Set<AxiomType<?>> outOfScopeTypes) {

		LogBlock b = createBlock("Ignoring out-of-scope axiom types");

		for (AxiomType<?> axType : outOfScopeTypes) {

			b.addLine("Axiom-type: " + axType);
		}

		writeBlock(b);
	}

	void logOutOfScopeAxiom(
			OWLAxiom axiom,
			OWLObject outOfScopeComponent,
			OutOfScopeExplanation explanation) {

		logOutOfScopeAxiom(
			axiom,
			Collections.singleton(outOfScopeComponent),
			Collections.singleton(explanation));
	}

	void logOutOfScopeAxiom(
			OWLAxiom axiom,
			Collection<? extends OWLObject> outOfScopeComponents,
			Collection<OutOfScopeExplanation> explanations) {

		LogBlock b = createBlock("Ignoring axiom containing out-of-scope component(s)");

		b.addLine(describeAxiom(axiom));

		Iterator<OutOfScopeExplanation> es = explanations.iterator();

		for (OWLObject c : outOfScopeComponents) {

			addOutOfScopeComponent(b, c, es.next());
		}

		writeBlock(b);
	}

	void logOutOfScopeQueryExpr(
			OWLClassExpression query,
			OWLClassExpression outOfScopeComponent,
			OutOfScopeExplanation explanation) {

		LogBlock b = createBlock("Cannot handle out-of-scope query component");

		b.addLine(describeQuery(query));
		addOutOfScopeComponent(b, outOfScopeComponent, explanation);
		b.addLine("No results returned!");

		writeBlock(b);
	}

	void logNoValueAxiomExprReplacement(
				OWLAxiom axiom,
				OWLClassExpression replaced,
				OWLClassExpression replacement) {

		logNoValueExprReplacement(describeAxiom(axiom), replaced, replacement);
	}

	void logNoValueQueryExprReplacement(
				OWLClassExpression query,
				OWLClassExpression replaced,
				OWLClassExpression replacement) {

		logNoValueExprReplacement(describeQuery(query), replaced, replacement);
	}

	private void logNoValueExprReplacement(
					String containerDescription,
					OWLClassExpression replaced,
					OWLClassExpression replacement) {

		LogBlock b = createBlock("Replacing out-of-scope \"No Value\" component");

		b.addLine(containerDescription);
		b.addLine("  Replaced: " + replaced);
		b.addLine("  Replacement: " + replacement);
		b.addLine("CAUTION!!! Weaker constraint. No contradiction checking.");

		writeBlock(b);
	}

	private void addOutOfScopeComponent(
					LogBlock block,
					OWLObject component,
					OutOfScopeExplanation explanation) {

		block.addLine("  Component: " + component);
		block.addLine("  Explanation: " + explanation);
	}

	private LogBlock createBlock(String warning) {

		return new LogBlock("REKON WARNING: " + warning + "...");
	}

	private String describeAxiom(OWLAxiom axiom) {

		return "Axiom: " + axiom;
	}

	private String describeQuery(OWLClassExpression query) {

		return "Query: " + query;
	}

	private void writeBlock(LogBlock block) {

		if (OPTION.enabled()) {

			Logger.SINGLETON.writeBlock(block);
		}
	}
}
