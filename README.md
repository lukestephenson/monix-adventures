# HEY!  WHAT ARE YOU DOING ON THIS BRANCH!!!

# monix-adventures
In general you should complete the exercises in the master branch.
There is a solution branch with  ... guess what...

The exercises below are designed to be followed in order.

Please note these exercises are using Monix 3.1.0.  Please refer to the docs at
https://monix.io/docs/3x/eval/task.html

## Adventure 1: Aysnchrony with Monix Task
The aim is:

1. Gain practice/confidence in working with Monix Task.

Complete the exercises in: adventures.task.TaskAdventures

There are a series of tests that you need to make pass in TaskAdventuresSpec.
Run them with `sbt "testOnly *.TaskAdventuresSpec"`.  Tests can also be run in the IDE.


## Adventure 2: Aysnchrony with Monix Observable
The aim is:

1. Gain practice/confidence in working with Monix Observable.

Where Task is for a single asynchronous action, an Observable represents an stream of
asynchronous actions.

Complete the exercises in: adventures.task.ObservableAdventures

The docs for Observable are online https://monix.io/docs/3x/reactive/observable.html.  Note they are very thin and
don't cover the majority of functions on Observable.  For those you are best exploring Observable in your IDE or
browsing https://monix.io/api/3.0/monix/reactive/Observable.html.

There are a series of tests that you need to make pass in ObservableAdventuresSpec.
Run them with `sbt "testOnly *.ObservableAdventuresSpec"`.  Tests can also be run in the IDE.
