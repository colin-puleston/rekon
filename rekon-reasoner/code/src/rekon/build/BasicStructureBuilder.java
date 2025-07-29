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

package rekon.build;

import java.util.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class BasicStructureBuilder {

	private OntologyNames names;
	private InputAxioms axioms;

	private abstract class TypeNameHandler<N extends Name, NS> {

		TypeNameHandler() {

			addInterNameLinks();
			addOrphansAsRootSubs();
		}

		abstract Name getHierarchyRootName();

		abstract Iterable<N> getAllTypeNames();

		abstract Iterable<? extends InputEquivalence<NS>> getTypeEquivalences();

		void addInterNameLinks() {

			for (InputEquivalence<NS> ax : getTypeEquivalences()) {

				N first = nameSourceToName(ax.getFirst());

				if (first != null) {

					N second = nameSourceToName(ax.getSecond());

					if (second != null) {

						first.addEquivalent(second);
					}
				}
			}
		}

		abstract N nameSourceToName(NS source);

		private void addOrphansAsRootSubs() {

			Name root = getHierarchyRootName();

			for (N n : getAllTypeNames()) {

				if (n != root && n.getSubsumers().isEmpty()) {

					n.addSubsumer(root);
				}
			}
		}
	}

	private abstract class HierarchyNameHandler
								<N extends Name, NS>
								extends TypeNameHandler<N, NS> {

		HierarchyNameHandler() {

			getHierarchyRootName().setAsRoot();
		}

		void addInterNameLinks() {

			super.addInterNameLinks();

			for (InputSubSuper<NS> ax : getTypeSubSupers()) {

				N sub = nameSourceToName(ax.getSub());

				if (sub != null) {

					N sup = nameSourceToName(ax.getSuper());

					if (sup != null) {

						sub.addSubsumer(sup);
					}
				}
			}
		}

		abstract Iterable<? extends InputSubSuper<NS>> getTypeSubSupers();
	}

	private class ClassNodeHandler extends HierarchyNameHandler<ClassNode, InputNode> {

		ClassNode getHierarchyRootName() {

			return names.getRootClassNode();
		}

		Iterable<ClassNode> getAllTypeNames() {

			return names.getClassNodes();
		}

		Iterable<InputClassEquivalence> getTypeEquivalences() {

			return axioms.getClassEquivalences();
		}

		Iterable<InputClassSubSuper> getTypeSubSupers() {

			return axioms.getClassSubSupers();
		}

		ClassNode nameSourceToName(InputNode source) {

			return asClassNodeOrNull(source);
		}
	}

	private class IndividualNodeHandler
						extends
							TypeNameHandler<IndividualNode, IndividualNode> {

		IndividualNodeHandler() {

			for (InputIndividualType ax : axioms.getIndividualTypes()) {

				ClassNode type = asClassNodeOrNull(ax.getType());

				if (type != null) {

					ax.getIndividual().addSubsumer(type);
				}
			}
		}

		ClassNode getHierarchyRootName() {

			return names.getRootClassNode();
		}

		Iterable<IndividualNode> getAllTypeNames() {

			return names.getIndividualNodes();
		}

		Iterable<InputIndividualEquivalence> getTypeEquivalences() {

			return axioms.getIndividualEquivalences();
		}

		IndividualNode nameSourceToName(IndividualNode source) {

			return source;
		}
	}

	private abstract class PropertyHandler<P extends PropertyX> extends HierarchyNameHandler<P, P> {

		P nameSourceToName(P source) {

			return source;
		}
	}

	private class NodePropertyHandler extends PropertyHandler<NodeProperty> {

		NodePropertyHandler() {

			for (InputNodePropertyInverse ax : axioms.getNodePropertyInverses()) {

				ax.getProperty().setInverse(ax.getInverse());
			}

			for (InputNodePropertySymmetric ax : axioms.getNodePropertySymmetrics()) {

				ax.getProperty().setSymmetric();
			}

			for (InputNodePropertyChain ax : axioms.getNodePropertyChains()) {

				new PropertyChain(ax.getProperty(), ax.getChain()).checkInverseChain();
			}

			for (InputNodePropertyTransitive ax : axioms.getNodePropertyTransitives()) {

				new PropertyChain(ax.getProperty());
			}
		}

		NodeProperty getHierarchyRootName() {

			return names.getRootNodeProperty();
		}

		Iterable<NodeProperty> getAllTypeNames() {

			return names.getNodeProperties();
		}

		Iterable<InputNodePropertyEquivalence> getTypeEquivalences() {

			return axioms.getNodePropertyEquivalences();
		}

		Iterable<InputNodePropertySubSuper> getTypeSubSupers() {

			return axioms.getNodePropertySubSupers();
		}
	}

	private class DataPropertyHandler extends PropertyHandler<DataProperty> {

		DataProperty getHierarchyRootName() {

			return names.getRootDataProperty();
		}

		Iterable<DataProperty> getAllTypeNames() {

			return names.getDataProperties();
		}

		Iterable<InputDataPropertyEquivalence> getTypeEquivalences() {

			return axioms.getDataPropertyEquivalences();
		}

		Iterable<InputDataPropertySubSuper> getTypeSubSupers() {

			return axioms.getDataPropertySubSupers();
		}
	}

	BasicStructureBuilder(OntologyNames names, InputAxioms axioms) {

		this.names = names;
		this.axioms = axioms;

		new ClassNodeHandler();
		new IndividualNodeHandler();
		new NodePropertyHandler();
		new DataPropertyHandler();
	}

	private ClassNode asClassNodeOrNull(InputNode in) {

		return in.getNodeType() == InputNodeType.CLASS ? in.asClassNode() : null;
	}
}
