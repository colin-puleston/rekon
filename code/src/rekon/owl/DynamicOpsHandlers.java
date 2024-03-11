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

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.*;

/**
 * @author Colin Puleston
 */
class DynamicOpsHandlers {

	private MappedNames names;

	private DynamicOps dynamicOps;

	private CoreBuilder coreBuilder;
	private ExpressionConverter expressionConverter;

	DynamicOpsHandlers(
		MappedNames names,
		DynamicOps dynamicOps,
		CoreBuilder coreBuilder,
		ExpressionConverter expressionConverter) {

		this.names = names;
		this.dynamicOps = dynamicOps;
		this.coreBuilder = coreBuilder;
		this.expressionConverter = expressionConverter;
	}

	DynamicOpsHandler getFor(OWLClassExpression expr) {

		if (expr instanceof OWLClass) {

			return getFor((OWLClass)expr);
		}

		return dynamicOps.createHandler(toDynamicPatternSource(expr));
	}

	DynamicOpsHandler getFor(OWLClass cls) {

		return dynamicOps.createHandler(names.get(cls));
	}

	DynamicOpsHandler getFor(OWLNamedIndividual ind) {

		return dynamicOps.createHandler(names.get(ind));
	}

	private MultiPatternSource toDynamicPatternSource(OWLClassExpression expr) {

		return coreBuilder.createMultiPatternSource(expressionConverter.toNode(expr));
	}
}
