# rekon #
Simple OWL reasoner. Implements OWL API.

## Supported OWL constructs ##

### Class-expressions ###

- <code>ObjectIntersectionOf</code>
- <code>ObjectUnionOf</code>(limited usage - see below)
- <code>ObjectSomeValuesFrom</code>
- <code>ObjectAllValuesFrom</code>
- <code>ObjectHasValue</code>
- <code>ObjectOneOf</code>
- <code>DataUnionOf</code>(numeric values only)
- <code>DataSomeValuesFrom</code>(numeric and boolean values only)
- <code>DataHasValue</code>(numeric and boolean values only)
    
**Permitted <code>ObjectUnionOf</code> Usage:** Only allowed as restriction fillers, though not allowed for:

- _Existential restrictions_ on properties that are either transitive or involved in property chains
- _Universal restrictions_ on any properties if any individuals are present in the ontology
        
### Axioms ###

- <code>SubClassOf</code>
- <code>EquivalentClasses</code>
- <code>SubObjectPropertyOf</code>(including property chains)
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
- <code>DataPropertyAssertion</code>(numeric and boolean values only)

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
