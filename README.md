# rekon #
Simple OWL reasoner. Implements OWL API.

## Supported OWL constructs ##

### Class-expressions ###

    ObjectIntersectionOf
    ObjectUnionOf
    ObjectSomeValuesFrom
    ObjectAllValuesFrom
    ObjectHasValue
    ObjectOneOf
    DataUnionOf (numeric values only)
    DataSomeValuesFrom (numeric and boolean values only)
    DataHasValue (numeric and boolean values only)
    
### Axioms ###

    SubClassOf
    EquivalentClasses
    SubObjectPropertyOf (including property chains)
    SubDataPropertyOf
    EquivalentObjectProperties
    EquivalentDataProperties
    InverseObjectProperties
    SymmetricObjectProperty
    TransitiveObjectProperty
    ObjectPropertyDomain
    ObjectPropertyRange
    DataPropertyDomain
    SameIndividual
    ClassAssertion
    ObjectPropertyAssertion
    DataPropertyAssertion (numeric and boolean values only)

## Unsupported OWL constructs ##

### Class-expressions ###

    ObjectMaxCardinality
    ObjectMinCardinality
    ObjectExactCardinality
    ObjectHasSelf
    DataIntersectionOf
    DataOneOf
    DataMaxCardinality
    DataMinCardinality
    DataExactCardinality
    DataAllValuesFrom

### Axioms ###

    ObjectComplementOf
    DataComplementOf
    DisjointClasses
    DisjointUnion
    DifferentIndividuals,
    DisjointObjectProperties
    DisjointDataProperties
    ReflexiveObjectProperty
    IrreflexiveObjectProperty
    AsymmetricObjectProperty
    FunctionalObjectProperty
    InverseFunctionalObjectProperty
    FunctionalDataProperty
    DataPropertyRange
    NegativeObjectPropertyAssertion
    NegativeDataPropertyAssertion
    HasKey
