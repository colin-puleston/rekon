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
class Classification {

	final ClassOps classOps;
	final IndividualOps individualOps;
	final ObjectPropertyOps objectPropertyOps;
	final DataPropertyOps dataPropertyOps;

	final EntailmentTester entailmentTester;

	private InstanceBoxCreator instanceBoxCreator;

	private class InstanceBoxCreator {

		private MappedNames names;
		private ExpressionConverter exprConverter;

		private InstanceOps instanceOps;

		InstanceBoxCreator(
			Ontology ontology,
			MappedNames names,
			ExpressionConverter exprConverter) {

			this.names = names;
			this.exprConverter = exprConverter;

			instanceOps = ontology.createInstanceOps();
		}

		RekonInstanceBox create() {

			return new RekonInstanceBox(instanceOps, names, exprConverter);
		}
	}

	private class Initialiser {

		private OWLOntologyManager manager;
		private OWLDataFactory factory;

		private MappedNames names;
		private ExpressionConverter exprConverter;
		private CoreBuilder coreBuilder;

		private Ontology ontology;
		private QueriablesAccessor queriables;

		Initialiser(OWLOntologyManager manager) {

			this.manager = manager;

			factory = manager.getOWLDataFactory();
			names = new MappedNames(manager);
			exprConverter = new ExpressionConverter(factory, names);
			coreBuilder = new CoreBuilder(names);
			ontology = createOntology();
			queriables = createQueriablesAccessor();
		}

		ClassOps createClassOps() {

			return new ClassOps(factory, names, queriables);
		}

		IndividualOps createIndividualOps() {

			return new IndividualOps(factory, names, queriables);
		}

		ObjectPropertyOps createObjectPropertyOps() {

			return new ObjectPropertyOps(factory, names);
		}

		DataPropertyOps createDataPropertyOps() {

			return new DataPropertyOps(factory, names);
		}

		InstanceBoxCreator createInstanceBoxCreator() {

			return new InstanceBoxCreator(ontology, names, exprConverter);
		}

		private Ontology createOntology() {

			return new Ontology(names, createStructureBuilder());
		}

		private StructureBuilder createStructureBuilder() {

			return coreBuilder.createStructureBuilder(createAxiomConverter());
		}

		private AxiomConverter createAxiomConverter() {

			return new AxiomConverter(manager, names, exprConverter);
		}

		private QueriablesAccessor createQueriablesAccessor() {

			return new QueriablesAccessor(ontology, names, coreBuilder, exprConverter);
		}
	}

	Classification(OWLOntologyManager manager) {

		Initialiser init = new Initialiser(manager);

		classOps = init.createClassOps();
		individualOps = init.createIndividualOps();
		objectPropertyOps = init.createObjectPropertyOps();
		dataPropertyOps = init.createDataPropertyOps();

		entailmentTester = new EntailmentTester(this);

		instanceBoxCreator = init.createInstanceBoxCreator();
	}

	RekonInstanceBox createInstanceBox() {

		return instanceBoxCreator.create();
	}
}
