package adventures.task

import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class TaskAdventuresSpec extends Specification {

  "TaskAdventuresSpec" should {
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
      def currentTemp() = Task(45)

      def cToF(c: Int) = Task {
        c * 9/5 + 32
      }

      result(TaskAdventures.getCurrentTempInFAgain(currentTemp, cToF)) must beEqualTo(113)
    }

    "calculate string complexity in parallel" in {
      val source = List("a", "b", "c")

      def complexity(string: String) = Task(string.length).delayExecution(1.second)

      val task: Task[Int] = TaskAdventures.calculateStringComplexityInParallel(source, complexity)

      val scheduler = TestScheduler()

      val result = task.runAsync(scheduler)

      scheduler.tick(1.second)

      Await.result(result, 2.seconds) must beEqualTo(3)
    }

    "calculate string complexity in parallel again" in {
      val source = List("a", "b", "c")

      def complexity(string: String) = Task(string.length).delayExecution(1.second)

      val task: Task[Int] = TaskAdventures.calculateStringComplexityInParallelAgain(source, complexity)

      val scheduler = TestScheduler()

      val result = task.runAsync(scheduler)

      scheduler.tick(1.second)

      Await.result(result, 2.seconds) must beEqualTo(3)
    }

    "retry on failure" should {
      "not retry on success" in new RetryScope(0) {
        result(task) must beEqualTo(1)

        calls must beEqualTo(1)
      }

      "not retry before delay" in new RetryScope(1) {
        task.runAsync(scheduler)

        calls must beEqualTo(1)
      }

      "retry after one failure with delay" in new RetryScope(1) {
        result(task) must beEqualTo(2)

        scheduler.tick(1.second)

        calls must beEqualTo(2)
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
    }.flatMap{ i =>
      if (i < failures) Task.raiseError(new RuntimeException(s"failing call $i"))
      else Task.now(i)
    }

    val scheduler = TestScheduler()

    val task = TaskAdventures.retryOnFailure(taskToRetry, 10, 1.second)
  }

  private def result[T](task: Task[T]): T = {
    import monix.execution.Scheduler.Implicits.global

    Await.result(task.runAsync, 2.seconds)
  }
}
