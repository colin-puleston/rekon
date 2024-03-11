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
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.Version;

/**
 * @author Colin Puleston
 */
public class RekonReasoner extends UnsupportedOps implements OWLReasoner {

	static final String REASONER_NAME = "REKON";
	static final Version REASONER_VERSION = new Version(1, 0, 0, 0);

	private OWLOntology rootOntology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;

	private RekonOps operations = null;
	private UpdateHandler updateHandler;

	public RekonReasoner(OWLOntology rootOntology) {

		this.rootOntology = rootOntology;

		manager = rootOntology.getOWLOntologyManager();
		factory = manager.getOWLDataFactory();

		updateHandler = new UpdateHandler(this, manager);

		NameRenderer.SINGLETON.setDefaultLabel(manager);
	}

	public RekonInstanceBox createInstanceBox() {

		updateHandler.notifyInstanceBoxPresent();

		return resolveOps().createInstanceBox();
	}

	public String getReasonerName() {

		return REASONER_NAME;
	}

	public Version getReasonerVersion() {

		return REASONER_VERSION;
	}

    public BufferingMode getBufferingMode() {

		return BufferingMode.BUFFERING;
    }

	public FreshEntityPolicy getFreshEntityPolicy() {

		return FreshEntityPolicy.ALLOW;
	}

	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {

		return IndividualNodeSetPolicy.BY_SAME_AS;
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {

		return Collections.singleton(InferenceType.CLASS_HIERARCHY);
	}

	public boolean isPrecomputed(InferenceType inferenceType) {

		return inferenceType == InferenceType.CLASS_HIERARCHY;
	}

	public void precomputeInferences(InferenceType... types) {

		resolveOps();
	}

	public List<OWLOntologyChange> getPendingChanges() {

		return updateHandler.getPendingChanges();
	}

	public Set<OWLAxiom> getPendingAxiomAdditions() {

		return updateHandler.getPendingAxiomAdditions();
	}

	public Set<OWLAxiom> getPendingAxiomRemovals() {

		return updateHandler.getPendingAxiomRemovals();
	}

	public OWLOntology getRootOntology() {

		return rootOntology;
	}

	public Node<OWLClass> getTopClassNode() {

		return NodeCreator.forClass(factory.getOWLThing());
	}

	public Node<OWLClass> getBottomClassNode() {

		return NodeCreator.forClass(factory.getOWLNothing());
	}

	public Node<OWLClass> getEquivalentClasses(OWLClassExpression expr) {

		return NodeCreator.forClasses(getClassOps().getEquivalents(expr));
	}

	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression expr, boolean direct) {

		return NodeCreator.forClassSets(getClassOps().getSupers(expr, direct));
	}

	public NodeSet<OWLClass> getSubClasses(OWLClassExpression expr, boolean direct) {

		return NodeCreator.forClassSets(getClassOps().getSubs(expr, direct));
	}

	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {

		return NodeCreator.forClassSets(getClassOps().getTypes(ind, direct));
	}

	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression expr, boolean direct) {

		return NodeCreator.forIndividualSets(getIndividualOps().getIndividuals(expr, direct));
	}

	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) {

		return NodeCreator.forIndividuals(getIndividualOps().getEquivalents(ind));
	}

	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {

		return NodeCreator.forObjectProperty(factory.getOWLTopObjectProperty());
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {

		return NodeCreator.forObjectProperty(factory.getOWLBottomObjectProperty());
	}

	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
													OWLObjectPropertyExpression prop) {

		if (prop instanceof OWLObjectProperty) {

			return getEquivalentObjectProperties((OWLObjectProperty)prop);
		}

		return NodeCreator.forNoObjectProperties();
	}

	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
													OWLObjectPropertyExpression prop,
													boolean direct) {

		if (prop instanceof OWLObjectProperty) {

			return getSuperObjectProperties((OWLObjectProperty)prop, direct);
		}

		return NodeCreator.forNoObjectPropertySets();
	}

	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
													OWLObjectPropertyExpression prop,
													boolean direct) {

		if (prop instanceof OWLObjectProperty) {

			return getSubObjectProperties((OWLObjectProperty)prop, direct);
		}

		return NodeCreator.forNoObjectPropertySets();
	}

	public Node<OWLDataProperty> getTopDataPropertyNode() {

		return NodeCreator.forDataProperty(factory.getOWLTopDataProperty());
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {

		return NodeCreator.forDataProperty(factory.getOWLBottomDataProperty());
	}

	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty prop) {

		return NodeCreator.forDataProperties(getDataPropertyOps().getEquivalents(prop));
	}

	public NodeSet<OWLDataProperty> getSuperDataProperties(
										OWLDataProperty prop,
										boolean direct) {

		return NodeCreator.forDataPropertySets(getDataPropertyOps().getSupers(prop, direct));
	}

	public NodeSet<OWLDataProperty> getSubDataProperties(
										OWLDataProperty prop,
										boolean direct) {

		return NodeCreator.forDataPropertySets(getDataPropertyOps().getSubs(prop, direct));
	}

	public boolean isConsistent() {

		return true;
	}

	public boolean isEntailed(OWLAxiom axiom) {

		return getEntailmentTester().entailed(axiom);
	}

	public boolean isEntailed(Set<? extends OWLAxiom> axioms) {

		return getEntailmentTester().entailed(axioms);
	}

	public Node<OWLClass> getUnsatisfiableClasses() {

		return NodeCreator.forNoClasses();
	}

	public void flush() {

		operations = null;
	}

	public void dispose() {

		operations = null;
	}

	private Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
														OWLObjectProperty prop) {

		return NodeCreator.forObjectProperties(getObjectPropertyOps().getEquivalents(prop));
	}

	private NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
														OWLObjectProperty prop,
														boolean direct) {

		return NodeCreator.forObjectPropertySets(getObjectPropertyOps().getSupers(prop, direct));
	}

	private NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
														OWLObjectProperty prop,
														boolean direct) {

		return NodeCreator.forObjectPropertySets(getObjectPropertyOps().getSubs(prop, direct));
	}

	private ClassOps getClassOps() {

		return resolveOps().classOps;
	}

	private IndividualOps getIndividualOps() {

		return resolveOps().individualOps;
	}

	private ObjectPropertyOps getObjectPropertyOps() {

		return resolveOps().objectPropertyOps;
	}

	private DataPropertyOps getDataPropertyOps() {

		return resolveOps().dataPropertyOps;
	}

	private EntailmentTester getEntailmentTester() {

		return resolveOps().entailmentTester;
	}

	private RekonOps resolveOps() {

		if (operations == null) {

			operations = new RekonOps(manager);

			updateHandler.reset();
		}

		return operations;
	}
}
