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

package rekon.test;

import java.io.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.HermiT.ReasonerFactory;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

import rekon.owl.RekonReasonerFactory;

class TestManager {

	final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

	OWLOntology loadOntology(File ontFile) {

		manager.getIRIMappers().add(createIRIMapper(ontFile));

		try {

			return manager.loadOntologyFromOntologyDocument(ontFile);
		}
		catch (OWLOntologyCreationException e) {

			throw new RuntimeException(e);
		}
	}

	OWLDataFactory getFactory() {

		return manager.getOWLDataFactory();
	}

	OWLReasoner createReasoner(OWLOntology ontology, ReasonerOpt opt) {

		return getReasonerFactory(opt).createReasoner(ontology);
	}

	private OWLReasonerFactory getReasonerFactory(ReasonerOpt opt) {

		switch (opt) {

			case REKON:
				return new RekonReasonerFactory();

			case ELK:
				return new ElkReasonerFactory();

			case FACT:
				return new FaCTPlusPlusReasonerFactory();

			case HERMIT:
				return new ReasonerFactory();
		}

		throw new Error("Unrcognised reasoner option: " + opt);
	}

	private OWLOntologyIRIMapper createIRIMapper(File ontFile) {

		return new AutoIRIMapper(ontFile.getParentFile(), true);
	}
}
