# rekon #

Simple OWL reasoner. Implements OWL API.

## Supported OWL constructs ##

### Class-expressions ###

- <code>ObjectIntersectionOf</code>
- <code>ObjectUnionOf</code> _restriction fillers only (possible incomplete reasoning - see below)_
- <code>ObjectSomeValuesFrom</code>
- <code>ObjectAllValuesFrom</code>
- <code>ObjectHasValue</code>
- <code>ObjectOneOf</code> _single individuals only_
- <code>DataUnionOf</code> _numeric values only_
- <code>DataSomeValuesFrom</code> _numeric and boolean values only_
- <code>DataHasValue</code> _numeric and boolean values only_
            
### Axioms ###

- <code>SubClassOf</code>
- <code>EquivalentClasses</code>
- <code>SubObjectPropertyOf</code> _including property chains_
- <code>SubDataPropertyOf</code>
- <code>EquivalentObjectProperties</code>
- <code>EquivalentDataProperties</code>
- <code>InverseObjectProperties</code>
- <code>SymmetricObjectProperty</code>
- <code>TransitiveObjectProperty</code>
- <code>ObjectPropertyDomain</code>
- <code>ObjectPropertyRange</code>
- <code>DataPropertyDomain</code>
- <code>SameIndividual</code>
- <code>ClassAssertion</code>
- <code>ObjectPropertyAssertion</code>
- <code>DataPropertyAssertion</code> _numeric and boolean values only_

## Unsupported OWL constructs ##

### Class-expressions ###

- <code>ObjectMaxCardinality</code>
- <code>ObjectMinCardinality</code>
- <code>ObjectExactCardinality</code>
- <code>ObjectHasSelf</code>
- <code>DataIntersectionOf</code>
- <code>DataOneOf</code>
- <code>DataMaxCardinality</code>
- <code>DataMinCardinality</code>
- <code>DataExactCardinality</code>
- <code>DataAllValuesFrom</code>

### Axioms ###

- <code>ObjectComplementOf</code>
- <code>DataComplementOf</code>
- <code>DisjointClasses</code>
- <code>DisjointUnion</code>
- <code>DifferentIndividuals</code>,
- <code>DisjointObjectProperties</code>
- <code>DisjointDataProperties</code>
- <code>ReflexiveObjectProperty</code>
- <code>IrreflexiveObjectProperty</code>
- <code>AsymmetricObjectProperty</code>
- <code>FunctionalObjectProperty</code>
- <code>InverseFunctionalObjectProperty</code>
- <code>FunctionalDataProperty</code>
- <code>DataPropertyRange</code>
- <code>NegativeObjectPropertyAssertion</code>
- <code>NegativeDataPropertyAssertion</code>
- <code>HasKey</code>

**Note:** Reasoning may be incomplete if <code>ObjectUnionOf</code> construct provides filler for: 

- _Existential restriction_ on property that is either transitive or involved in any property chains
- _Universal restriction_ on any property when the ontology contains any individuals
