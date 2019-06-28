# Deeto

__A Java dynamic proxy factory for interface-typed data transfer objects__

Deeto is a Clojure [1] library for Java developers. With Deeto you can
define your data transfer object (DTO [2, 3]) types via __interfaces__
in Java. You do __not need to implement these interfaces__. Instead
you can ask Deeto to analyze (via reflection [4]) the interface class
`I` and then give you a factory [5] `Deeto.factory().newInstance(I)`
for `I`. On each invovation this factory will return a new
(__initializd__; see below) instance `i` with the following
properties:

* `i`'s class `P` is a Java dynamic proxy class [6] which implements
  `I`, `Cloneable` and `Serializable`.

* `i` uses a (Clojure based) invocation _handler_. This _handler_ will
  process method invocations on `i`. `i`'s _handler_ is
  stateful. Calling setter `void set<q>(Q)` (of `I`) on `i` will set
  the __property__ (see below) `q` of type `Q`. Calling
  _build-mutator_ `I <q>(<Q>)` will also set property `q` but return
  `i`. Calling `Q get<q>()` (of `I`) on `i` will return `i`'s current
  value of property `q` of type `Q` [9]. These stateful semantics are
  implemented in the _handler_.

* `P`'s `boolean equals(Object o)` implementation (of `boolean
  Object.equals(Object)`) returns `true` if `o` and `i` are of the
  __same `Class`__ and if all properties are `Q.equals(Object)`
  [10]. I.e. the `equals` semantics for `P` are implemented in the
  _handler_ -- the `equals` semantics for each of the properties are
  implemented in each properties' class/type `Q`.

* `P`'s `int hashCode()` implementation (of `int Object.hashCode()`)
  is consistent with `P`'s `boolean equals(Object)`
  implementation. The `P.hashCode()` semantics are implemented in the
  _handler_ and it's based on `Q.hashCode()` of the properties. So
  again, `P.hashCode` is consistent with `P.equals(Object)` only if
  each `Q.hashCode()` is consistent with `Q.equals(Object)`.

* `P`'s `P clone()` implementation (of `Object Object.clone()`) will
  return a _serialization copy_ of `i` [8]. I.e. the copy will be
  created by deserializing the serialized `i`. Note that this will
  usually mean that the copy is a __deep clone/copy__ of the original
  `i` (but there is no guarantee since technically there are ways to
  explicitly and intentionally screw-up on this).

[1] https://clojure.org/  
[2] https://en.wikipedia.org/wiki/Data_transfer_object  
[3] https://martinfowler.com/bliki/LocalDTO.html  
[4] https://www.oracle.com/technetwork/articles/java/javareflection-1536171.html  
[5] https://en.wikipedia.org/wiki/Factory_method_pattern  
[6] https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html  
[7] https://www.journaldev.com/17129/java-deep-copy-object  
[8] Implementation note: at the moment `clone()` does __not__ use
    serialization but just creates a new proxy with the original's
    value. This may change in future releases though.  
[9] If `Q` is `boolean` (not `Boolean`!) getters of the form `boolean
  is<q>()` are also recognized.  
[10] See __Native-typed properties__ below for more detail on when two
  DTOs are considered being equal.

## Properties

Data transfer objects are __mutable containers__ -- used to transport
a bunch of values (_properties_) that belong together somehow
[1]. They usually have a setter and a getter for each property `q` (of
matching types `Q`). Java Beans [2] have similiar semantics but
support the observer pattern and more.

Deeto analyzes the interface `I` and derives the set of properties and
their types. The _handler_ then implements the stateful
get-and-set-semantics.

On construction these properties are set to an __initialization
value__ (see below).

__Note:__ Deeto supports interface-definitions with only a getter or
setter for a property even if those have limited usability. 

[1] I don't want to argue about what DTOs exactly are, what to use
  them for and why you shouldn't and how they are different from value
  objects. Deeto is for those who do use DTOs for one reason or
  the other.  
[2] https://en.wikipedia.org/wiki/JavaBeans

## Native-typed properties

Deeto supports native-typed properties (like `boolean` and
`double`). Internally these values are stored as instances of their
wrapper classes (like `Boolean` and `Double`).

### floating point datatypes

Java has a _mismatch_ when it comes to compare floating point values
[1]. You have `0.0 = -0.0` is `true` but `new Double(-0.0).equals(new
Double(0.0))` is `false`.

So for the implementation of `equals(Object)` Deeto will compare
native-typed (floating point) properties (`float` and `double`) via
`=`. But for properties of the wrapper types (`Float` and `Double`)
Deeto will use the type's `equals(Object)` method.

This is probably closest to what a Java programmer would do, if she
was asked to code an implementation for `equals(Object)` and the class
had a `float`/`double` typed field.

Special care must be taken when refactoring existing Java code which
does not follow this path.

Note thet Deeto's implementation of `hashCode()` is consistent with
these semantics.

As a side note: notice that `Arrays.equals(new double[] { -0.0 }, new
double[] { 0.0 })` is `false` which puzzles me. But the Java docs for
`Arrays.equals(double[], double[])` say just that.

[1] https://stackoverflow.com/questions/45544180/signed-zero-double-equals-as-in-but-double-comparedouble-double-0/45544483  

### initialization of native-typed properties

Java initializes reference-typed fields (incl. arrays) with `null`
(i.e. `nil`). For native-typed fields (like `boolean` and `float`)
Java defines an __initialization value__ for each type (see JLS 4.12.5
_Initial Values of Variables_ [1]).

Deeto mimics this behavior. So when constructing a DTO, properties
will be _bound_ to their corresponding type's initialization value.

Currently there is no way for clients to define this initialization
value for Deeto's DTOs (like through an annotation or so). You'll have
to put that logic into your factories (see below).

[1] https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5

## Fluent interface/builder pattern

A builder [1] is a stateful-container that lets you set/build/collect
state (values) and then finally use it as a factory for some
object. Usually the builder will provide "setter"-methods that can be
*chained* (fluent API [2]).

Deeto supports this kind of usage by providing/supporting
implementations for (_build-mutator_) methods of the form `I
<q>(<Q>)`.

**Example:** here the property `foo` of some `SomeDto dto` can be set
  via `dto.foo(String)` which returns `dto` (i.e. the __mutated__
  instance that the method was invoked on).

    interface SomeDto extends IDeeto<SomeDto> {
    
        String getFoo();
        void setFoo(String x);
        SomeDto foo(String x);

With Deeto you're not using a seperate builder but you're using the
stateful DTO directly. So all this feature gives you is the option to
use method chaining on _build-mutators_ instead of calling setters.

__Note__: the _build-mutators_ __are__ __mutators__ and not
__factory__ __methods__. So they act like setters and they return the
__mutated__ instance (proxy) on which they are invoked.

The _build-mutators_ (methods) of some interface `I` are discovered by
Deeto through their signature:

* their return type is `I`
* they take one argument
* their (one) argument is `q`'s type `Q`

The following rules are not enforced at the moment (but may in future
releases!):

* their name matches the name `q` of one of `I`´s _properties_
* There must at least be a getter for `q` when there is a
  _build-mutator_ for `q`.

__Note:__ Deeto supports interface-definitions with just a
_build-mutator_ for a property and no getter even if those have
limited usability.

[1] https://en.wikipedia.org/wiki/Builder_pattern  
[2] https://en.wikipedia.org/wiki/Fluent_interface  

## Read-only views to DTOs

Sometimes you may want to construct a DTO but then give clients just
read-only access to the DTO. At the moment Deeto has no special
feature for this use-case.

But you can implement this by using __two__ interfaces to define the
DTO: one `public` `interface` for the read-only access for the client
and an additional _builder_ `interface` for your factory [1].

__Example:__ Here we define `FooDto` with two properties. The factory
method `FooDto.newInstance` uses (the just locally visable) `Builder`
with the mutating builder methods. Clients access the DTO through the
read-only view `FooDto`.

    package deeto_user;

    import deeto.Deeto;
    import deeto.IDeeto;

    interface Builder extends FooDto {

        Builder foo(long x);
        Builder bar(boolean x);

    }

    public interface FooDto extends IDeeto<Builder> {

        long getFoo();
        boolean isBar();

        static FooDto newInstance(long foo, boolean bar, double doo) {
            return Deeto.factory().newInstance(Builder.class).foo(foo).bar(bar);
        }
    }

Note that this DTO is __not immutable__ _per se_. Deeto has no feature
that let's you express that. It's just the case that there aren't any
mutators accessible to clients (other than using reflection ...).

[1] Deeto could have used true factory-like builder-methods instead of
making them a mutator. Then you could have put them into just one
interface and still implement the read-only view. But then builders
would create a new instance/clone each time they're called which
introduces a performance penalty (which I haven't measured -- of
course ;-) Anyway, they are mutators and the pattern from above should
work fine.

## Map-based access

DTOs behave almost like mutable maps with typed key/values
(properties). Deeto supports __converting__ between __maps__ and
__DTOs__.

The DTO's interface can/may define

	java.util.Map<String, Object> toMap();

When this method is called on a Deeto proxy it returns a map which
maps each/all property name (capitalized string; e.g. `"FooBar"`) to
its cloned/copied value (possibly `nil`). I.e. the map will contain an
entry for each (all!) property.

Note that values for properties with __native__ type (like `int` and
`float`) will be returned with their __wrapper__ class (like `Integer`
and `Float`) [1].

The DTO's interface can/may also define

    <T> T fromMap(java.util.Map<String, Object> source);

When this method is called on a Deeto proxy it will use the entries in
`source` to set the property values of the instance on which the
method was called. It then returns the (mutated) instance. So this is
a __mutator__ and __not a factory__.

Again you have to supply wrapper-typed values for properties with
native type.

Note that this method throws an exception if any given value is not
type-compatible (as of `assignableFrom`) with the corresponding
property. Wrapper-types are handled as exspected. No other conversion,
transformation, cast etc. is done though.

[1] https://www.w3schools.com/java/java_wrapper_classes.asp

## Immutability

Deeto's proxys are __mutable, stateful containers__ with getters and
setters for access to their __properties values__. And eventhough the
__properties__ are mutable, their __values__ are meant to be
__immutable__.

So Deeto's setter implementation creates a _serialization copy_ (see
above) or _defensive copy_ [1, 2] of its argument value (which
therefore must be `Serializable` [4]). So there is no way for users of
Deeto to give a setter method (or one of the mutators) a mutable
object and keep a reference to that object through which the client
could afterwards change that very object and thus change the DTO's
value _behind the scene_. The only way to change the __property__ is
by using the setters/mutators. There just __is no way you can change a
value__ [3].

The same is true for Deeto's getter/toMap which also return only a
_defensive copy_ of their internal (possibly mutable) value object.

[1] http://www.javacreed.com/what-is-defensive-copying/  
[2] http://www.javapractices.com/topic/TopicAction.do?Id=15  
[3] https://clojure.org/about/state  
[4] Deeto supports `Serializable` typed properties only. One could
  imagine to support `Cloneable` typed properties also. But
  `Cloneable`/`clone` is implemented easily in a way that introduces
  reference leaks and thus breaks with the kind of immutablility which
  Deeto aims to support. I hope that DTOs usually contain
  `Serializable` typed values so that this restriction does not hinder
  the applicability of Deeto.

## Concurrency

Deeto proxies are __not thread-safe__ _per se_! I.e. the Clojure stuff
__is__ thread-safe [1] but serialization of the __mutable__
__property__ __values__ may or may not be thread-safe depending on the
classes involved.

Note that the critical part is the cloneing/copying of argument values
via serialization (which may or may not be affected by
__race-conditions__) when invoking setters and mutators. Once the
values have been copied any further access to the DTO (even the
mutation of the DTO's internal state) __is thread-safe__!

So Deeto can make no guarantee on the overall thread-safeness.

[1] Internally Deeto uses persistent/immutable maps [2] to store
property values and `swap!` [3] to mutate this state `atom` [4]. All
that is thread-safe.  
[2] https://clojure.org/reference/data_structures#Maps  
[3] https://clojure.org/reference/refs  
[4] https://clojure.org/reference/atoms  

## Mapping DTOs

When using DTOs you often need to __map__ between DTOs and other
classes/containers (like JPA entities) back and forth.

_Mapping_ means to use a getter on one container to retrieve a value
and then use that value with a setter on another container.

The source and target properties in those containers may or may not
have the same __name__. If they have different __types__ then one
needs to __convert__ the source-typed value to a target-typed value
before using the setter.

In some cases one may even want/have to apply some sort of _business
logic_ to the mapping (like special/default values, handling of
`null`s and more).

Deeto does not support mappings in any special way. There are other
Java tools for that [1]:

* http://mapstruct.org/
* https://github.com/modelmapper/modelmapper
* https://dozermapper.github.io/
* https://orika-mapper.github.io/orika-docs/
* https://github.com/jmapper-framework/jmapper-core

[1] https://www.baeldung.com/java-performance-mapping-frameworks

## Performance

I haven't done any performance measurements. But I expect Deeto to be
orders of magnitude slower than a Java implementation of the DTO
classes.

This may or may not be a problem for your use-case.

[1] https://dzone.com/articles/java-reflection-but-faster

## Usage

You can download Deeto JAR from clojars:
https://clojars.org/repo/deeto/deeto/

In addition you need Clojure JAR [1]:
https://repo1.maven.org/maven2/org/clojure/clojure/1.8.0/clojure-1.8.0.jar

For Maven users (you need to add clojars to your SNAPSHOT repos):

	<dependency>
	  <groupId>deeto</groupId>
	  <artifactId>deeto</artifactId>
	  <version>0.1.0-SNAPSHOT</version>
	</dependency>

In your Java code you define an interface for your DTO (`SomeDto`)
with getters and setters. If you like, you can put a static factory
method in there too (which will be ignored by Deeto).

Note that `SomeDto extends deeto.IDeeto` so that the DTO type
implements `Cloneable` and `Serializable` __and__ has a `public`
`clone` method so that `clone` can be called without problems (and no
need to cast). The interface also has definitions for `fromMap` and
`toMap` (see above).

Using `deeto.IDeeto` is totally optional for using Deeto.

    package deeto_user;

    import deeto.Deeto;
    import deeto.IDeeto;

    interface SomeDto extends IDeeto<SomeDto> {

        String getFoo();
        void setFoo(String x);
	    SomeDto foo(String x);

        int getBar();
        void setBar(int x);
	    SomeDto bar(int x);

        static SomeDto newInstance() {
            return Deeto.factory().newInstance(SomeDto.class);
        }
    }

    public class DeetoExample {

        public static void main(String[] args) throws Exception {

            SomeDto foo = SomeDto.newInstance();
            System.out.println("foo = " + foo);

            foo.setFoo("FOO!");
            System.out.println("foo = " + foo);

            System.out.println("foo.equals(null) = " + foo.equals(null));
            System.out.println("foo.equals(\"\") = " + foo.equals(""));

            SomeDto bar = foo.clone();
            System.out.println("bar.equals(foo) = " + bar.equals(foo));

            SomeDto fooCopy = Deeto.factory().copyOf(foo);
            System.out.println("fooCopy = " + fooCopy);

            SomeDto barCopy = Deeto.factory().copyOf(bar);
            System.out.println("barCopy = " + barCopy);

            System.out.println("barCopy.equals(fooCopy) = " + barCopy.equals(fooCopy));

            foo.foo("y").foo("x");
            bar.foo("x");

            System.out.println("foo.equals(bar) = " + foo.equals(bar));

            Map fooMap = new HashMap();
            fooMap.put("Bar", 42);
            System.out.println("foo.fromMap(fooMap) = " + foo.fromMap(fooMap));

            System.out.println("foo.toMap() = " + foo.toMap());

        }
    }

[1] I've tested version 1.8.0 but it should work with any recent
  Clojure release.

## Notes

* setters and mutators do only some checks on their arguments. These
  checks will become stricter in future releases.

* `toString()` implementation is __not stable yet__. So you have to
  expect __changes in future releases__.

* the Clojure code is pretty bad at the moment. It will be refactored
  but sematics will not change (hopefully :-)

* serial form will __change in the future__. You must expect that
  stored serial forms will __not be compatible with future releases__.

## TODOS

* add documentation about serial form
