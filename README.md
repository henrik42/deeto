# Deeto


__!!!!!!!! THIS IS WORK IN PROGRESS !!!!!!!__


__A Java dynamic proxy factory for interface-typed data transfer objects__

Deeto is a Clojure [1] library for Java developers. With Deeto you can
define your data transfer object (DTO [2, 3]) types via __interfaces__
in Java. You do __not need to implement these interfaces__. Instead
you can ask Deeto to analyze (via reflection [4]) the interface class
`I` and then give you a factory [5] for `I`. On each invovation this
factory will return a new instance `i` with the following properties:

* `i`'s class `P` is a Java dynamic proxy class [6] wich implements
  `I`, `Cloneable` and `Serializable`.

* `i` uses a (Clojure based) invocation _handler_. This _handler_ will
  process method invocations on `i`. `i`'s _handler_ is
  statefull. Calling setter `void set<q>(Q)` (of `I`) on `i` will set
  the __property__ (see below) `q` of type `Q`. Calling `Q get<q>()`
  (of `I`) on `i` will return `i`'s current value of property `q` of
  type `Q`. These statefull semantics are implemented in the
  _handler_.

* `P`'s `boolean equals(Object o)` implementation (of `boolean
  Object.equals(Object)`) returns `true` if `o` and `i` are of the
  __same `Class`__ and if all properties are `equals(Object)`. The
  `equals` semantics are implemented in the _handler_.

* `P`'s `int hashCode()` implementation (of `int Object.hashCode()`)
  is consistent with `P`'s `boolean equals(Object)`
  implementation. The `hashCode` semantics are implemented in the
  _handler_.

* `P`'s `P clone()` implementation (of `Object Object.clone()`) will
  return a _serialization copy_ of `i`. I.e. the copy will be created
  by deserializing the serialized `i`. Note that this will usually
  mean that the copy is a __deep clone/copy__ of the original `i` (but
  there is no guarantee since technically there are ways to explicitly
  and intentionally screw-up on this).

__TBD__ Say something about `P`'s serialized form and how it can pass
  process boundaries.

[1] `https://clojure.org/`  
[2] `https://en.wikipedia.org/wiki/Data_transfer_object`  
[3] `https://martinfowler.com/bliki/LocalDTO.html`  
[4] `https://www.oracle.com/technetwork/articles/java/javareflection-1536171.html`  
[5] `https://en.wikipedia.org/wiki/Factory_method_pattern`  
[6] `https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html`  
[7] `https://www.journaldev.com/17129/java-deep-copy-object`  

## Properties

Data transfer objects are __mutable containers__ -- used to transport
a bunch of values (_properties_) that belong together somehow
[1]. They usually have a setter and a getter for each property `q` (of
matching types `Q`). Java Beans have similiar semantics but support
the observer pattern.

Deeto analyzes the interface `I` and derives the set of properties and
their types. The _handler_ then implements the statefull
get-and-set-semantics.

[1] I don't want to argue about what DTOs exactly are, what to use
them for and why you shouldn't and how they are different from value
objects. Deeto is for those who do use DTOs for one reason or the
other.

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

