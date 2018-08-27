package adventures.task

import cats.Applicative
import monix.eval
import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

/**
  * If a result 'A' is available synchronously, then that same result asynchronously
  * could be represented as a 'Task[A]'
  */
object TaskAdventures {

  /**
    * 1.  Create a task which returns 43
    *
    * See https://monix.io/docs/2x/eval/task.html#simple-builders for ways to construct Tasks
    */
  def immediatelyExecutingTask(): Task[Int] = {
    Task.now(43)
  }

  /**
    * 2.	Create a Task which when executed logs “hello world” (using `logger`)
    */
  def helloWorld(logger: String => Unit): Task[Unit] = {
    Task.eval(logger("hello world"))
  }

  /**
    * 3.	Create a Task which always fails.
    *
    * See https://monix.io/docs/2x/eval/task.html#taskraiseerror
    */
  def alwaysFailingTask(): Task[Unit] = {
    Task.raiseError(new Exception("Die"))
  }

  /**
    * 4.	There is 1 remote service which will return you a task that provides the current temperature in celsius.
    *
    */
  def getCurrentTempInF(currentTemp: () => Task[Int]): Task[Int] = {
    def cToF(c: Int) = c * 9 / 5 + 32

    currentTemp().map(cToF)
  }

  /**
    * 5.	There is 1 remote service which will return you a task that provides the current temperature in celsius.
    * The conversion is complex so we have decided to refactor it out to another remote microservice.
    * Make use of both of these services to return the current temperature in fahrenheit.
    */
  def getCurrentTempInFAgain(currentTemp: () => Task[Int], converter: Int => Task[Int]): Task[Int] = {
    currentTemp().flatMap(converter)
  }

  /**
    * 6. Computing the complexity of a string is a very expensive op.  Implement this function so that complexity
    * of the calculation will be done in parallel.  Sum the returned results to
    * provide the overall complexity for all Strings.  (try using operations from monix)
    *
    * Also, check out the spec for this to see how the Monix TestScheduler can be used to simulate the passage of time
    * in tests.
    */
  def calculateStringComplexityInParallel(strings: List[String], complexity: String => Task[Int]): Task[Int] = {
    val tasks: Seq[Task[Int]] = strings.map(complexity)

    Task.gather(tasks).map(_.sum)
  }

  /**
    * 6.b. As above, but try to implement the parallel processing using the monix Applicative instance for Task
    * and the cats `sequence` function. (if you haven't heard of cats / sequence skip this - even if you have consider
    * this as optional).
    *
    * The following imports will help.
    * import cats.implicits._
    * implicit def parTaskApplicative: Applicative[eval.Task.Par] = Task.catsParallel.applicative
    *
    * Note that you will also need to convert from Task to Task.Par for the cats sequence operator to execute the tasks
    * in parallel.
    */
  def calculateStringComplexityInParallelAgain(strings: List[String], complexity: String => Task[Int]): Task[Int] = {
    import cats.implicits._

    implicit def parTaskApplicative: Applicative[eval.Task.Par] = Task.catsParallel.applicative

    val tasks: List[Task.Par[Int]] = strings.map(complexity).map(Task.Par.apply)

    val parTask = tasks.sequence[Task.Par, Int].map(_.sum)
    Task.Par.unwrap(parTask)
  }

  /**
    * 7.	Write a function which given a Task, will reattempt that task after a specified delay for a maximum number of
    * attempts if the supplied Task fails.
    */
  def retryOnFailure[T](task: Task[T], maxRetries: Int, delay: FiniteDuration): Task[T] = {
    def retry(remainingAttempts: Int): Task[T] = {
      task.onErrorHandleWith { error =>
        if (remainingAttempts > 0)
          retry(remainingAttempts - 1).delayExecution(delay)
        else
          Task.raiseError(error)
      }
    }
    retry(maxRetries)
  }

}
