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
class RekonOps {

	final ClassOps classOps;
	final IndividualOps individualOps;
	final ObjectPropertyOps objectPropertyOps;
	final DataPropertyOps dataPropertyOps;

	final EntailmentTester entailmentTester;

	private InstanceBoxCreator instanceBoxCreator;

	private class InstanceBoxCreator {

		private MappedNames names;
		private ExpressionConverter expressionConverter;

		private InstanceOps instanceOps;

		InstanceBoxCreator(
			Ontology ontology,
			MappedNames names,
			ExpressionConverter expressionConverter) {

			this.names = names;
			this.expressionConverter = expressionConverter;

			instanceOps = ontology.createInstanceOps();
		}

		RekonInstanceBox create() {

			return new RekonInstanceBox(instanceOps, names, expressionConverter);
		}
	}

	private class Initialiser {

		private OWLOntologyManager manager;
		private OWLDataFactory factory;

		private MappedNames names;
		private ExpressionConverter expressionConverter;
		private CoreBuilder coreBuilder;

		private Ontology ontology;
		private DynamicOpsHandlers dynamicOpsHdlrs;

		Initialiser(OWLOntologyManager manager) {

			this.manager = manager;

			factory = manager.getOWLDataFactory();
			names = new MappedNames(manager);
			expressionConverter = new ExpressionConverter(factory, names);
			coreBuilder = new CoreBuilder(names);
			ontology = createOntology();
			dynamicOpsHdlrs = createDynamicOpsHandlers();
		}

		ClassOps createClassOps() {

			return new ClassOps(factory, names, dynamicOpsHdlrs);
		}

		IndividualOps createIndividualOps() {

			return new IndividualOps(factory, names, dynamicOpsHdlrs);
		}

		ObjectPropertyOps createObjectPropertyOps() {

			return new ObjectPropertyOps(factory, names);
		}

		DataPropertyOps createDataPropertyOps() {

			return new DataPropertyOps(factory, names);
		}

		InstanceBoxCreator createInstanceBoxCreator() {

			return new InstanceBoxCreator(ontology, names, expressionConverter);
		}

		private Ontology createOntology() {

			return new Ontology(names, createStructureBuilder());
		}

		private StructureBuilder createStructureBuilder() {

			return coreBuilder.createStructureBuilder(createAxiomConverter());
		}

		private AxiomConverter createAxiomConverter() {

			return new AxiomConverter(manager, names, expressionConverter);
		}

		private DynamicOpsHandlers createDynamicOpsHandlers() {

			DynamicOps ops = ontology.createDynamicOps();

			return new DynamicOpsHandlers(names, ops, coreBuilder, expressionConverter);
		}
	}

	RekonOps(OWLOntologyManager manager) {

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
