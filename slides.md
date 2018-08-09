---
title: Types for Quality
revealOptions:
    transition: 'fade'
    transitionSpeed: 'fast'
theme: 'night'
---

## Monix Adventures

Luke Stephenson
https://github.com/lukestephenson/monix-adventures

---

### Prerequisites

- Scala
- Understanding of scala.concurrent.Future

---

### First steps

https://github.com/lukestephenson/monix-adventures

- Star the project!
- Install sbt `brew install sbt`
- Download the internet `sbt test`

---

### Modelling results
- Synchronous execution
```scala
val fahrenheit: Int = c * 9 / 5 + 32
```
- Asynchronous execution
```scala
val result: F[User] = loadUserFromDb()
```
- So we need some `F` to represent an asynchronous computation

---

### Asynchronous execution

#### Eager
```scala
val result: Future[User] = loadUserFromDb()
```
#### Lazy
```scala
val result: Task[User] = loadUserFromDb()
```

---

### Lazy asynchronous execution

```scala
val result: Task[User] = loadUserFromDb()
result.delayExecution(1.second)
```
- Just data
- Control of side effect execution
- Referential transparency
- Laws

---

### Multiple values
#### Synchronous execution
```scala
val iterable: Iterable[Int] = List(1, 2, 3).toIterable
```
#### Asynchronous execution
```scala
val result: Iterable[Future[String]] = readLinesFromAPI()
```

Note:
What does this mean?  As values are pulled from the Iterable, those Futures are not complete.  Iterable is a data
structure which doesn't model / represent asynchronous behaviour.

---

### Multiple asynchronous values

```scala
val result: Observable[String] = readLinesFromAPI()
```

Note:
Asynchrony is a first class citizen

---

### Start coding
