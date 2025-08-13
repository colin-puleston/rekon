# rekon #

Simple OWL reasoner. Implements OWL API.

## Supported OWL constructs ##

### Class Expressions ###

- <code>Class</code>
- <code>ObjectIntersectionOf</code>
- <code>ObjectUnionOf</code> _limited usage (see below)_
- <code>ObjectSomeValuesFrom</code>
- <code>ObjectHasValue</code>
- <code>ObjectOneOf</code> _single individuals only_
- <code>DataUnionOf</code> _numeric values only_
- <code>DataSomeValuesFrom</code> _numeric and boolean values only_
- <code>DataHasValue</code> _numeric and boolean values only_

**_Note:_** <code>ObjectUnionOf</code> objects are only supported when used as restriction fillers,
and only for properties that are neither "chained" (_i.e._ either _(a)_ transitive, or _(b)_ involved
in one or more property chains), nor sub-properties of "chained" properties.

### Property Expressions ###

- <code>ObjectProperty</code>
- <code>DataProperty</code>

### Individuals ###

- <code>NamedIndividual</code>
            
### Built-in Entities ###

- <code>owl:Thing</code>
- <code>owl:topObjectProperty</code>
- <code>owl:topDataProperty</code>
            
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

### Class Expressions ###

- <code>ObjectAllValuesFrom</code>
- <code>ObjectMaxCardinality</code>
- <code>ObjectMinCardinality</code>
- <code>ObjectExactCardinality</code>
- <code>ObjectHasSelf</code>
- <code>DataIntersectionOf</code>
- <code>DataOneOf</code>
- <code>DataAllValuesFrom</code>
- <code>DataMaxCardinality</code>
- <code>DataMinCardinality</code>
- <code>DataExactCardinality</code>

### Property Expressions ###

- <code>ObjectInverseOf</code>

### Individuals ###

- <code>AnonymousIndividual</code>

### Built-in Entities ###

- <code>owl:Nothing</code>
- <code>owl:bottomObjectProperty</code>
- <code>owl:bottomDataProperty</code>

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
