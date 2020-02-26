package adventures.task

import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class TaskAdventuresSpec extends Specification {

  "TaskAdventures" should {
    "immediately executing Task should return 43" in {
      result(TaskAdventures.immediatelyExecutingTask()) must beEqualTo(43)
    }

    "side effectful helloWorld Task" should {
      "have no side effect if not executed" in new LoggingScope {
        TaskAdventures.helloWorld(logger)

        logged.toList must beEmpty
      }

      "have single side effect if run once" in new LoggingScope {
        result(TaskAdventures.helloWorld(logger))

        logged.toList must beEqualTo(List("hello world"))
      }

      "have two side effects if run twice" in new LoggingScope {
        val helloWorldTask = TaskAdventures.helloWorld(logger)
        result(helloWorldTask)
        result(helloWorldTask)

        logged.toList must beEqualTo(List("hello world", "hello world"))
      }
    }

    "failing Task should fail" in {
      result(TaskAdventures.alwaysFailingTask()) must throwA[Exception]()
    }

    "get Current Temp In F" in {
      result(TaskAdventures.getCurrentTempInF(() => Task(45))) must beEqualTo(113)
    }

    "get Current Temp In F again" in {
      val currentTemp = () => Task(45)

      def cToF(c: Int) = Task {
        c * 9/5 + 32
      }

      result(TaskAdventures.getCurrentTempInFAgain(currentTemp, cToF)) must beEqualTo(113)
    }

    "calculate string complexity in parallel" in {
      val source = List("a", "b", "c", "e", "f", "g")

      def complexity(string: String) = Task(string.length).delayExecution(1.second)

      val task: Task[Int] = TaskAdventures.calculateStringComplexityInParallel(source, complexity)

      val scheduler = Scheduler.fixedPool(name = "test1", poolSize = 10)

      val result = task.runToFuture(scheduler)

      // if not run in parallel this will timeout.
      Await.result(result, 2.seconds) must beEqualTo(6)
    }

    "calculate string complexity in parallel again" in {
      val source = List("a", "b", "c", "e", "f", "g")

      def complexity(string: String) = Task(string.length).delayExecution(1.second)

      val task: Task[Int] = TaskAdventures.calculateStringComplexityInParallelAgain(source, complexity)

      val scheduler = Scheduler.fixedPool(name = "test2", poolSize = 10)

      val result = task.runToFuture(scheduler)

      // if not run in parallel this will timeout.
      Await.result(result, 2.seconds) must beEqualTo(6)
    }

    "retry on failure" should {
      "not retry on success" in new RetryScope(0) {
        result(task) must beEqualTo(10)

        calls must beEqualTo(1)
      }

      "not retry before delay" in new RetryScope(1) {
        task.runToFuture(scheduler)

        calls must beEqualTo(1)
      }

      "retry after one failure with delay" in new RetryScope(1) {
        result(task) must beEqualTo(20)

        scheduler.tick(1.second)

        calls must beEqualTo(2)
      }

      "return success result of 10th retry" in new RetryScope(10) {
        task.runToFuture(scheduler)

        scheduler.tick(10.seconds)

        calls must beEqualTo(11)
      }

      "return failure result of 10th retry" in new RetryScope(11) {
        val futureResult = task.runToFuture(scheduler)

        scheduler.tick(10.seconds)

        calls must beEqualTo(11)

        Await.result(futureResult, 1.second) must throwA[IllegalArgumentException]
      }

      "give up after 10 retries" in new RetryScope(100) {
        task.runToFuture(scheduler)

        scheduler.tick(1.minute)

        calls must beEqualTo(11)
      }
    }
  }

  class LoggingScope extends Scope {
    val logged = ListBuffer[String]()

    def logger(log: String): Unit = logged.append(log)
  }

  class RetryScope(failures: Int) extends Scope {
    var calls = 0
    private val taskToRetry = Task.eval {
      calls = calls + 1
      calls
    }.flatMap { i =>
      if (i <= failures) Task.raiseError(new IllegalArgumentException(s"failing call $i"))
      else Task.now(i * 10)
    }

    val scheduler = TestScheduler()

    val task = TaskAdventures.retryOnFailure(taskToRetry, 10, 1.second)
  }

  private def result[T](task: Task[T]): T = {
    import monix.execution.Scheduler.Implicits.global

    Await.result(task.runToFuture, 2.seconds)
  }
}
