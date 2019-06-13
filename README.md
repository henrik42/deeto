# Deeto

__A Java dynamic proxy factory for interface-typed data transfer objects__

Deeto is a Clojure [1] library for Java developers. With Deeto you can
define your data transfer object (DTO [2, 3]) types via __interfaces__
in Java. You do __not need to implement these interfaces__. Instead
you can ask Deeto to analyze (via reflection [4]) the interface class
`I` and then give you a factory [5] for `I`. On each invovation this
factory will return a new instance `i` with the following properties:

* `i`'s class `P` is a Java dynamic proxy class [6] which implements
  `I`, `Cloneable` and `Serializable`.

* `i` uses a (Clojure based) invocation _handler_. This _handler_ will
  process method invocations on `i`. `i`'s _handler_ is
  stateful. Calling setter `void set<q>(Q)` (of `I`) on `i` will set
  the __property__ (see below) `q` of type `Q`. Calling `Q get<q>()`
  (of `I`) on `i` will return `i`'s current value of property `q` of
  type `Q`. These stateful semantics are implemented in the _handler_.

* `P`'s `boolean equals(Object o)` implementation (of `boolean
  Object.equals(Object)`) returns `true` if `o` and `i` are of the
  __same `Class`__ and if all properties are
  `Q.equals(Object)`. I.e. the `equals` semantics for `P` are
  implemented in the _handler_ -- the `equals` semantics for each of
  the properties are implemented in each properties' class `Q`.

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

## Properties

Data transfer objects are __mutable containers__ -- used to transport
a bunch of values (_properties_) that belong together somehow
[1]. They usually have a setter and a getter for each property `q` (of
matching types `Q`). Java Beans [2] have similiar semantics but
support the observer pattern and more.

Deeto analyzes the interface `I` and derives the set of properties and
their types. The _handler_ then implements the stateful
get-and-set-semantics.

[1] I don't want to argue about what DTOs exactly are, what to use
  them for and why you shouldn't and how they are different from value
  objects. Deeto is for those who do use DTOs for one reason or
  the other.  
[2] https://en.wikipedia.org/wiki/JavaBeans

## Immutability

Deeto's proxys are __mutable, stateful containers__ with getters and
setters for access to their __properties values__. And eventhough the
__properties__ are mutable, their __values__ are meant to be
__immutable__.

So Deeto's setter implementation creates a _serialization copy_ (see
above) or _defensive copy_ [1, 2] of its argument value (which
therefore must be `Serializable` [4]). So there is no way for users of
Deeto to give a setter method a mutable object and keep a reference to
that object through which the client could afterwards change that very
object and thus change the DTO's value _behind the scene_. The only
way to change the __property__ is by using the setter. There just __is
no way you can change a value__ [3].

The same is true for Deeto's getter which also return only a
_defensive copy_ of their internal (possibly mutable) value
object.

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
need to cast). Using `deeto.IDeeto` is totally optional for using
Deeto.

    package deeto_user;

    import deeto.Deeto;
    import deeto.IDeeto;

    interface SomeDto extends IDeeto {

        String getFoo();
        void setFoo(String x);

        int getBar();
        void setBar(int x);

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

        }
    }

[1] I've tested version 1.8.0 but it should work with any recent
  Clojure release.

## Notes

* `toString()` implementation is __not stable yet__. So you have to
  expect __changes in future releases__.

* the Clojure code is pretty bad at the moment. It will be refactored
  but sematics will not change (hopefully :-)

* serial form will __change in the future__. You must expect that
  stored serial forms will __not be compatible with future releases__.

## TODOS

* add documentation about serial form
