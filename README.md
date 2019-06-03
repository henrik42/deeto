# Deeto

__!!!!!!!! THIS IS WORK IN PROGRESS !!!!!!!__


__A Java dynamic proxy factory for interface-typed DTOs__.

Deeto is a Clojure [1] library for Java developers. With Deeto you can
define your DTO (data transfer object [2, 3]) types with
__interfaces__ in Java. You do __not need to implement these
interfaces__. Instead you can ask Deeto to analyze (via reflection
[4]) the interface class `I` and then give you a factory [5] for
`I`. On each invovation this factory will return a new instance `i`
with the following properties:

* `i`'s class is a Java dynamic proxy class [6]

* `i` uses a (Clojure based) handler. This handler will process method
  invocations on `i` (see below).

* `i`'s class `P` will implement `I`, `Cloneable` and `Serializable`.

* `i` is statefull. Calling setter `void set<q>(Q)` (of `I`) on `i`
  will set the __property__ (see below) `q` of type `Q`. Calling `Q
  get<q>()` (of `I`) on `i` will return `i`'s current value of
  property `q` of type `Q`.

* `P`'s `boolean equals(Object o)` implementation (of `boolean Object.equals(Object)`)
  returns `true` if `o` and `i` are of the
  __same `Class`__ and if all properties are `equals(Object)`.

* `P`'s `int hashCode()` implementation (of `int Object.hashCode()`)
  is consistent with `P`'s `boolean equals(Object)` implementation.

* `P`'s `P clone()` implementation (of `Object Object.clone()`) will
  return a copy of `i` with clones of each property value. Note that
  there is no guarantee on the properties' clones (i.e. Deeto does not
  guarentee deep clones [7]. So Deeto's clone semantics rely on the
  properties clone semantics).

* `P`'s serialized form [...] __TBD__

[1] Clojure  
[2] DTO - Wikipedia  
[3] antipattern  
[4] Reflection  
[5] Factory  
[6] Java dynamic proxy  
[7] Deep Clone  

## Properties

Data transfer objects are __containers__ -- used to transport a bunch
of values (_properties_) that belong together somehow. They usually
have a setter and a getter for each property `q` (of matching types
`Q`). Java Beans have similiar semantics but support the observer
pattern.

Deeto analyzes the interface `I` and derives the set of properties and
their types. 

## Usage

In your Java code you define an interface for your DTO:

	interface MyDto {
		String getString();
		void setString(String x);
	}

Now you use Deeto to create a factory:

__TBD: create factory__

Finally you create instances and use them:

__create instances and use__

## TODOs

* Create deep clones via serialization

* Create read-only views

## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
