package adventures.observable

import adventures.observable.model.{PageId, PaginatedResult, SourceRecord, TargetRecord}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import org.specs2.mutable.Specification

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

class ObservableAdventuresSpec extends Specification {
  "ObservableAdventures" should {
    "create a simple observable" in {
      val source = List(SourceRecord("1", "1.1"), SourceRecord("2", "2.2"), SourceRecord("3", "3.3"))
      val obs = ObservableAdventures.listToObservable(source)

      runLog(obs) must beEqualTo(source)
    }

    "transform data and filter out invalid data" in {
      val source = Observable(SourceRecord("1", "1.1"), SourceRecord("2", "invalid"), SourceRecord("3", "3.3"))

      val obs = ObservableAdventures.transform(source)

      val expected = List(TargetRecord("1", 1.1), TargetRecord("3", 3.3))
      runLog(obs) must beEqualTo(expected)
    }

    "load in batches" in {
      val loads = ListBuffer[List[TargetRecord]]()

      def esLoad(batch: Seq[TargetRecord]): Task[Unit] = {
        Task(loads.append(batch.toList))
      }

      val source = (1 to 12).map(i => TargetRecord(i.toString, i)).toList
      val obs = ObservableAdventures.load(Observable.fromIterable(source), esLoad)
      val expected = source.grouped(5).toList

      runLog(obs) must beEqualTo(List(5, 5, 2))
      loads.toList must beEqualTo(expected)
    }

    "load in batches and retry on failure" in {
      val loads = ListBuffer[List[TargetRecord]]()

      var i = 0

      def esLoad(batch: Seq[TargetRecord]): Task[Unit] = {
        val result = {
          if (i % 2 == 0) Task.raiseError(new RuntimeException("ES write failed"))
          else Task(loads.append(batch.toList))
        }
        Task { i = i + 1 }.flatMap(_ => result)
      }

      val source = (1 to 12).map(i => TargetRecord(i.toString, i)).toList
      val obs = ObservableAdventures.load(Observable.fromIterable(source), esLoad)
      val expected = source.grouped(5).toList

      runLog(obs) must beEqualTo(List(5, 5, 2))
      loads.toList must beEqualTo(expected)
    }

    "Consume an observable" in {
      val task = ObservableAdventures.execute(Observable(5, 5, 2))

      Await.result(task.runAsync, 10.seconds) must beEqualTo(12)
    }

    "handle a paginated feed" in {
      val pages = Map(
        PageId.FirstPage -> PaginatedResult(List(SourceRecord("1", "1.1")), Some(PageId("2"))),
        PageId("2") -> PaginatedResult(List(SourceRecord("2", "2.2")), Some(PageId("3"))),
        PageId("3") -> PaginatedResult(List(SourceRecord("3", "3.3")), None)
      )

      def readPage(pageId: PageId): Task[PaginatedResult] = {
        Task(pages(pageId))
      }

      val obs = ObservableAdventures.readFromPaginatedDatasource(readPage)

      val expected = List(SourceRecord("1", "1.1"), SourceRecord("2", "2.2"), SourceRecord("3", "3.3"))
      runLog(obs) must beEqualTo(expected)
    }
  }

  def runLog[T](observable: Observable[T], timeout: FiniteDuration = 10.seconds): List[T] =
    Await.result(observable.consumeWith(collectingConsumer).runAsync, timeout).reverse

  /**
    * Consumer which records all elements in a Vector.  Only for test purposes
    */
  private def collectingConsumer[T]: Consumer[T, List[T]] = Consumer.foldLeft(List.empty[T]) { (acc, elem) =>
    elem :: acc
  }
}
