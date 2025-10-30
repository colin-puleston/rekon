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

import rekon.core.*;
import rekon.build.*;

/**
 * @author Colin Puleston
 */
public class OntologyConverter {

	private NameMapper names;
	private Ontology ontology;

	private ExpressionConverter expressions;

	public OntologyConverter(OWLOntologyManager manager) {

		names = new NameMapper(manager);
		expressions = new ExpressionConverter(manager.getOWLDataFactory(), names);
		ontology = new Ontology(names, createStructureBuilder(manager));
	}

	public NameMapper getNameMapper() {

		return names;
	}

	public Ontology getOntology() {

		return ontology;
	}

	public DynamicConverter createDynamicConverter() {

		return new DynamicConverter(names, ontology, expressions);
	}

	private StructureBuilder createStructureBuilder(OWLOntologyManager manager) {

		return new StructureBuilder(names, createAxiomConverter(manager));
	}

	private AxiomConverter createAxiomConverter(OWLOntologyManager manager) {

		return new AxiomConverter(manager, names, expressions);
	}
}