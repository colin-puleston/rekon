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
import rekon.owl.convert.*;

/**
 * @author Colin Puleston
 */
class Classification {

	final ClassOps classOps;
	final IndividualOps individualOps;
	final ObjectPropertyOps objectPropertyOps;
	final DataPropertyOps dataPropertyOps;

	final EntailmentTester entailmentTester;

	private OwlConverter converter;
	private InstanceOps instanceOps;

	private class Initialiser {

		private OWLOntologyManager manager;
		private OWLDataFactory factory;

		private Ontology ontology;
		private CoreBuilder coreBuilder;

		private NameMapper names;
		private OwlQueryables queryables;

		Initialiser(OWLOntologyManager manager, StartupMonitor monitor) {

			this.manager = manager;

			factory = manager.getOWLDataFactory();

			monitor.onLoadingStart();
			performLoad();
			monitor.onLoadingComplete();

			monitor.onClassificationStart();
			performClassification(monitor);
			monitor.onClassificationComplete();

			queryables = createQueryables();
			instanceOps = ontology.createInstanceOps();
		}

		ClassOps createClassOps() {

			return new ClassOps(factory, names, queryables);
		}

		IndividualOps createIndividualOps() {

			return new IndividualOps(factory, names, queryables);
		}

		ObjectPropertyOps createObjectPropertyOps() {

			return new ObjectPropertyOps(factory, names);
		}

		DataPropertyOps createDataPropertyOps() {

			return new DataPropertyOps(factory, names);
		}

		private void performLoad() {

			converter = new OwlConverter(manager);
			names = converter.getNameMapper();

			coreBuilder = new CoreBuilder(names);
			ontology = new Ontology(names, createStructureBuilder());
		}

		private void performClassification(StartupMonitor monitor) {

			ontology.classify(monitor.createClassifyListener());
		}

		private StructureBuilder createStructureBuilder() {

			return coreBuilder.createStructureBuilder(converter.createAxiomConverter());
		}

		private OwlQueryables createQueryables() {

			return new OwlQueryables(ontology, coreBuilder, converter);
		}
	}

	Classification(OWLOntologyManager manager, StartupMonitor monitor) {

		Initialiser init = new Initialiser(manager, monitor);

		classOps = init.createClassOps();
		individualOps = init.createIndividualOps();
		objectPropertyOps = init.createObjectPropertyOps();
		dataPropertyOps = init.createDataPropertyOps();

		entailmentTester = new EntailmentTester(this);
	}

	RekonInstanceBox createInstanceBox() {

		return new RekonInstanceBox(instanceOps, converter);
	}
}
