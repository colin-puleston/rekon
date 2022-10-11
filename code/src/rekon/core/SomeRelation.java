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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
public class SomeRelation extends ObjectRelation {

	static private class Expander {

		private NameSet visitedNodes;

		private Set<Relation> allExpansions = new HashSet<Relation>();

		private List<SomeRelation> passExpansions = new ArrayList<SomeRelation>();
		private List<SomeRelation> lastPassExpansions = new ArrayList<SomeRelation>();

		private abstract class ExpansionTypeCollector {

			void collectFromNested(SomeRelation current) {

				Value target = current.getTarget();

				if (target instanceof NodeValue) {

					NodeName n = ((NodeValue)target).toSingleName();

					if (n != null) {

						collectFromTarget(n);
					}
				}
			}

			abstract void collectFrom(SomeRelation current);

			private void collectFromTarget(NodeName target) {

				for (Relation r : getAllFromTarget(target)) {

					if (r instanceof SomeRelation) {

						collectFrom((SomeRelation)r);
					}
				}
			}

			private Set<Relation> getAllFromTarget(NodeName target) {

				return new SignatureRelationCollector(visitedNodes).collectFromName(target);
			}
		}

		private class TransitivityBasedCollector extends ExpansionTypeCollector {

			private PropertyName transProp;

			TransitivityBasedCollector(PropertyName transProp) {

				this.transProp = transProp;
			}

			void collectFrom(SomeRelation current) {

				if (transProp.subsumes(current.getProperty())) {

					addExpansion(current);
				}
			}
		}

		private class ChainBasedCollector extends ExpansionTypeCollector {

			private PropertyChain chain;
			private int tailsSubsIndex = 0;

			ChainBasedCollector(PropertyChain chain) {

				this.chain = chain;
			}

			void collectFrom(SomeRelation current) {

				if (chain.hasTailSub(current.getProperty(), tailsSubsIndex)) {

					if (chain.lastTailSub(tailsSubsIndex)) {

						addExpansion(createLinkRelation(current));
					}
					else {

						tailsSubsIndex++;
						collectFromNested(current);
						tailsSubsIndex--;
					}
				}
			}

			private SomeRelation createLinkRelation(SomeRelation endSub) {

				return chain.createLinkRelation(endSub.getObjectTarget());
			}
		}

		Expander(SomeRelation relation, NameSet visitedNodes) {

			this.visitedNodes = visitedNodes;

			allExpansions.add(relation);
			lastPassExpansions.add(relation);

			List<ExpansionTypeCollector> typeCollectors = getExpansionTypeCollectors(relation);

			if (!typeCollectors.isEmpty()) {

				collectExpansions(typeCollectors);
			}
		}

		Collection<Relation> expand() {

			return allExpansions;
		}

		private void collectExpansions(List<ExpansionTypeCollector> typeCollectors) {

			while (true) {

				for (ExpansionTypeCollector typeCol : typeCollectors) {

					for (SomeRelation r : lastPassExpansions) {

						typeCol.collectFromNested(r);
					}
				}

				if (passExpansions.isEmpty()) {

					break;
				}

				lastPassExpansions = passExpansions;
				passExpansions = new ArrayList<SomeRelation>();
			}
		}

		private List<ExpansionTypeCollector> getExpansionTypeCollectors(SomeRelation relation) {

			List<ExpansionTypeCollector> typeCollectors = new ArrayList<ExpansionTypeCollector>();
			ObjectPropertyName transProp = relation.lookForMostGeneralTransitiveProperty();

			if (transProp != null) {

				typeCollectors.add(new TransitivityBasedCollector(transProp));
			}

			for (PropertyChain chain : relation.getAllChains()) {

				typeCollectors.add(new ChainBasedCollector(chain));
			}

			return typeCollectors;
		}

		private void addExpansion(SomeRelation relation) {

			if (allExpansions.add(relation)) {

				passExpansions.add(relation);
			}
		}
	}

	public SomeRelation(ObjectPropertyName property, ObjectValue target) {

		super(property, target);
	}

	Collection<Relation> expandForSignature(NameSet visitedNodes) {

		return new Expander(this, visitedNodes).expand();
	}

	NodeValue getNodeTarget() {

		return (NodeValue)getTarget();
	}

	boolean potentialNewSignatureRelations() {

		ObjectPropertyName prop = getObjectProperty();

		if (prop.transitive() || prop.anyChains()) {

			return getTarget().newSubsumers(NodeMatcher.structureFor(prop));
		}

		return false;
	}

	private ObjectPropertyName lookForMostGeneralTransitiveProperty() {

		return getObjectProperty().lookForMostGeneralTransitiveProperty();
	}

	private Collection<PropertyChain> getAllChains() {

		return getObjectProperty().getAllChains();
	}
}
