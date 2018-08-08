# monix-adventures
In general you should complete the exercises in the master branch.
There is a solution branch with  ... guess what...

The exercises below are designed to be followed in order.

Please note these exercises are using Monix 2.3.2.  Please refer to the docs at
https://monix.io/docs/2x/eval/task.html

## Adventure 1: Aysnchrony with Monix Task
The aim is:

1. Gain practice/confidence in working with Monix Task.

Complete the exercises in: adventures.task.TaskAventures

There are a series of tests that you need to make pass in TaskAventuresSpec.
Run them with `sbt "testOnly *.TaskAventuresSpec"`.  Tests can also be run in the IDE.


## Adventure 2: Aysnchrony with Monix Observable
The aim is:

1. Gain practice/confidence in working with Monix Observable.

Where Task is for a single asynchronous action, an Observable represents an stream of
asynchronous actions.

Complete the exercises in: adventures.task.ObservableAdventures

There are a series of tests that you need to make pass in ObservableAdventuresSpec.
Run them with `sbt "testOnly *.ObservableAdventuresSpec"`.  Tests can also be run in the IDE.
