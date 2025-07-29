# rekon #

Simple OWL reasoner. Implements OWL API.

## Supported OWL constructs ##

### Class-expressions ###

- <code>ObjectIntersectionOf</code>
- <code>ObjectUnionOf</code> _restriction fillers only_
- <code>ObjectSomeValuesFrom</code>
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
- <code>SameIndividual</code>
- <code>ClassAssertion</code>
- <code>ObjectPropertyAssertion</code>
- <code>DataPropertyAssertion</code> _numeric and boolean values only_

## Unsupported OWL constructs ##

### Class-expressions ###

- <code>ObjectAllValuesFrom</code>
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
- <code>ObjectPropertyDomain</code>
- <code>DataPropertyDomain</code>
- <code>ObjectPropertyRange</code>
- <code>DataPropertyRange</code>
- <code>FunctionalObjectProperty</code>
- <code>FunctionalDataProperty</code>
- <code>InverseFunctionalObjectProperty</code>
- <code>ReflexiveObjectProperty</code>
- <code>IrreflexiveObjectProperty</code>
- <code>AsymmetricObjectProperty</code>
- <code>NegativeObjectPropertyAssertion</code>
- <code>NegativeDataPropertyAssertion</code>
- <code>HasKey</code>

**_Note:_** Reasoning may be incomplete if ontology contains any constructs of the form: 

- <code>ObjectSomeValuesFrom</code> restriction on _chained property_, with <code>ObjectUnionOf</code> filler

...where a _chained property_ is a property that is either transitive or involved in one or more property
chains, as either super-property or a sub-property
