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
import org.semanticweb.owlapi.reasoner.impl.*;

/**
 * @author Colin Puleston
 */
class NodeCreator {

	static private ClassNodes classNodes = new ClassNodes();
	static private IndividualNodes individualNodes = new IndividualNodes();
	static private ObjectPropertyNodes objectPropertyNodes = new ObjectPropertyNodes();
	static private DataPropertyNodes dataPropertyNodes = new DataPropertyNodes();

	static private abstract class Nodes<E extends OWLObject, I extends E> {

		Node<E> toNode(Set<I> group) {

			return createNode(toOutputElements(group));
		}

		NodeSet<E> toSet(Set<Set<I>> groups) {

			return createSet(toNodes(groups));
		}

		abstract Node<E> createNode(Set<E> group);

		abstract NodeSet<E> createSet(Set<Node<E>> nodes);

		Set<E> toOutputElements(Set<I> group) {

			return new HashSet<E>(group);
		}

		private Set<Node<E>> toNodes(Set<Set<I>> groups) {

			Set<Node<E>> nodes = new HashSet<Node<E>>();

			for (Set<I> group : groups) {

				nodes.add(toNode(group));
			}

			return nodes;
		}
	}

	static private abstract class SimpleNodes
									<E extends OWLObject>
									extends Nodes<E, E> {

		Set<E> toOutputElements(Set<E> group) {

			return group;
		}
	 }

	static private class ClassNodes extends SimpleNodes<OWLClass> {

		Node<OWLClass> createNode(Set<OWLClass> group) {

			return new OWLClassNode(group);
		}

		NodeSet<OWLClass> createSet(Set<Node<OWLClass>> nodes) {

			return new OWLClassNodeSet(nodes);
		}
	}

	static private class IndividualNodes extends SimpleNodes<OWLNamedIndividual> {

		Node<OWLNamedIndividual> createNode(Set<OWLNamedIndividual> group) {

			return new OWLNamedIndividualNode(group);
		}

		NodeSet<OWLNamedIndividual> createSet(Set<Node<OWLNamedIndividual>> nodes) {

			return new OWLNamedIndividualNodeSet(nodes);
		}
	}

	static private class ObjectPropertyNodes
							extends
								Nodes
									<OWLObjectPropertyExpression,
									OWLObjectProperty> {

		Node<OWLObjectPropertyExpression> createNode(Set<OWLObjectPropertyExpression> group) {

			return new OWLObjectPropertyNode(group);
		}

		NodeSet<OWLObjectPropertyExpression> createSet(Set<Node<OWLObjectPropertyExpression>> nodes) {

			return new OWLObjectPropertyNodeSet(nodes);
		}
	}

	static private class DataPropertyNodes extends SimpleNodes<OWLDataProperty> {

		Node<OWLDataProperty> createNode(Set<OWLDataProperty> group) {

			return new OWLDataPropertyNode(group);
		}

		NodeSet<OWLDataProperty> createSet(Set<Node<OWLDataProperty>> nodes) {

			return new OWLDataPropertyNodeSet(nodes);
		}
	}

	static Node<OWLClass> forClass(OWLClass entity) {

		return new OWLClassNode(entity);
	}

	static Node<OWLClass> forClasses(Set<OWLClass> group) {

		return classNodes.toNode(group);
	}

	static Node<OWLClass> forNoClasses() {

		return new OWLClassNode();
	}

	static NodeSet<OWLClass> forClassSets(Set<Set<OWLClass>> groups) {

		return classNodes.toSet(groups);
	}

	static Node<OWLNamedIndividual> forIndividuals(Set<OWLNamedIndividual> group) {

		return individualNodes.toNode(group);
	}

	static NodeSet<OWLNamedIndividual> forIndividualSets(Set<Set<OWLNamedIndividual>> groups) {

		return individualNodes.toSet(groups);
	}

	static Node<OWLObjectPropertyExpression> forObjectProperty(OWLObjectProperty entity) {

		return new OWLObjectPropertyNode(entity);
	}

	static Node<OWLObjectPropertyExpression> forObjectProperties(Set<OWLObjectProperty> group) {

		return objectPropertyNodes.toNode(group);
	}

	static Node<OWLObjectPropertyExpression> forNoObjectProperties() {

		return new OWLObjectPropertyNode();
	}

	static NodeSet<OWLObjectPropertyExpression> forObjectPropertySets(Set<Set<OWLObjectProperty>> groups) {

		return objectPropertyNodes.toSet(groups);
	}

	static NodeSet<OWLObjectPropertyExpression> forNoObjectPropertySets() {

		return new OWLObjectPropertyNodeSet();
	}

	static Node<OWLDataProperty> forDataProperty(OWLDataProperty entity) {

		return new OWLDataPropertyNode(entity);
	}

	static Node<OWLDataProperty> forDataProperties(Set<OWLDataProperty> group) {

		return dataPropertyNodes.toNode(group);
	}

	static NodeSet<OWLDataProperty> forDataPropertySets(Set<Set<OWLDataProperty>> groups) {

		return dataPropertyNodes.toSet(groups);
	}
}