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

package rekon;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.vocab.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.util.*;

/**
 * @author Colin Puleston
 */
class NameRenderer {

	static final NameRenderer SINGLETON = new NameRenderer();

	static private final IRI DEFAULT_LABEL_ANNO_IRI = OWLRDFVocabulary.RDFS_LABEL.getIRI();

	private OWLObjectRenderer labelsRenderer = null;

	private class LabelsRendererCreator {

		private OWLOntologyManager manager;
		private OWLDataFactory factory;

		private Collection<IRI> annoPropertyIRIs;

		LabelsRendererCreator(OWLOntologyManager manager) {

			this(manager, Collections.singletonList(DEFAULT_LABEL_ANNO_IRI));
		}

		LabelsRendererCreator(OWLOntologyManager manager, Collection<IRI> annoPropertyIRIs) {

			this.manager = manager;
			this.annoPropertyIRIs = annoPropertyIRIs;

			factory = manager.getOWLDataFactory();

			labelsRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
			labelsRenderer.setShortFormProvider(createShortFormProvider());
		}

		ShortFormProvider createShortFormProvider() {

			return new AnnotationValueShortFormProvider(
							getAnnoProperties(),
							Collections.emptyMap(),
							manager);
		}

		private List<OWLAnnotationProperty> getAnnoProperties() {

			List<OWLAnnotationProperty> props = new ArrayList<OWLAnnotationProperty>();

			for (IRI iri : annoPropertyIRIs) {

				props.add(factory.getOWLAnnotationProperty(iri));
			}

			return props;
		}
	}

	void setDefaultLabel(OWLOntologyManager manager) {

		new LabelsRendererCreator(manager);
	}

	void setLabels(OWLOntologyManager manager, Collection<IRI> annotationPropertyIRIs) {

		new LabelsRendererCreator(manager, annotationPropertyIRIs);
	}

	String render(OWLObject object) {

		return labelsRenderer == null ? object.toString() : labelsRenderer.render(object);
	}

	String renderAll(Collection<? extends OWLObject> objects) {

		StringBuilder s = new StringBuilder();

		s.append("\n[\n");

		for (OWLObject o : objects) {

			s.append("\t" + render(o) + "\n");
		}

		s.append("]\n");

		return s.toString();
	}
}
