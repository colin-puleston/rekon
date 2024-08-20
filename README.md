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
    
NOTE: Inclusion of ObjectUnionOf/DataUnionOf make reasoning incomplete

### Axioms ###

    SubClassOf
    EquivalentClasses
    SubObjectPropertyOf (including property chains)
    SubDataPropertyOf
    EquivalentObjectProperties
    EquivalentDataProperties
    TransitiveObjectProperty
    ObjectPropertyDomain
    ObjectPropertyRange
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
    InverseObjectProperties
    ReflexiveObjectProperty
    IrreflexiveObjectProperty
    SymmetricObjectProperty
    AsymmetricObjectProperty
    FunctionalObjectProperty
    InverseFunctionalObjectProperty
    FunctionalDataProperty
    DataPropertyDomain
    DataPropertyRange
    NegativeObjectPropertyAssertion
    NegativeDataPropertyAssertion
    HasKey
