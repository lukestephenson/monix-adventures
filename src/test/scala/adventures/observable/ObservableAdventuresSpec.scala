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
        Task {
          i = i + 1
          i
        }.flatMap { loadCount =>
          if (loadCount % 2 == 0) {
            Task.raiseError(new RuntimeException("ES write failed"))
          } else Task(loads.append(batch.toList))
        }
      }

      val source = (1 to 12).map(i => TargetRecord(i.toString, i)).toList
      val obs = ObservableAdventures.loadWithRetry(Observable.fromIterable(source), esLoad)
      val expected = source.grouped(5).toList

      runLog(obs) must beEqualTo(List(5, 5, 2))
      loads.toList must beEqualTo(expected)
    }

    "Consume an observable" in {
      val task = ObservableAdventures.execute(Observable(5, 5, 2))

      Await.result(task.runToFuture, 10.seconds) must beEqualTo(12)
    }

    "handing a paginated feed" should {
      "handle a small set of data" in {
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

      // Monix 2.3 Observable had some stack safety issues
      "not blow the stack when handling a large paginated feed" in {
        val lastPage = 1000
        val pages = (0 to lastPage).map { n =>
          val pageRecords = List(SourceRecord(n.toString, s"$n.$n"))
          val nextPage = if (n >= lastPage) None else Some(PageId((n + 1).toString))

          PageId(n.toString) -> PaginatedResult(pageRecords, nextPage)
        }.toList

        val pagesMap = pages.toMap

        def readPage(pageId: PageId): Task[PaginatedResult] = {
          Task(pagesMap(pageId))
        }

        val obs = ObservableAdventures.readFromPaginatedDatasource(readPage)

        val expected = pages.flatMap(_._2.results)

        runLog(obs) must beEqualTo(expected)
      }

      // Verifies the implementation doesn't go the whole way to the end of all pages before emitting data.
      "should emit data as it is read" in {
        val pages = Map(
          PageId.FirstPage -> PaginatedResult(List(SourceRecord("1", "1.1")), Some(PageId("2"))),
          PageId("2") -> PaginatedResult(List(SourceRecord("2", "2.2")), Some(PageId("3"))),
          PageId("3") -> PaginatedResult(List(SourceRecord("3", "3.3")), None)
        )

        def readPage(pageId: PageId): Task[PaginatedResult] = {
          Task(pages(pageId)).delayResult(1.second)
        }

        val obs = ObservableAdventures.readFromPaginatedDatasource(readPage)

        var dataEmitted: SourceRecord = null

        obs.consumeWith(Consumer.foreach(emitted => dataEmitted = emitted)).runToFuture

        Thread.sleep(1500)

        dataEmitted must not(beNull)
      }
    }

    "run the reads and writes in parallel" in {
      val pages = (0 to 19).map { page =>
        val pageRecords = (0 to 4).map { record =>
          SourceRecord(s"${page}-${record}", "111")
        }.toList
        val nextPage = if (page >= 19) None else Some(PageId((page + 1).toString))
        PageId(page.toString) -> PaginatedResult(pageRecords, nextPage)
      }.toMap

      def readPage(pageId: PageId): Task[PaginatedResult] = {
        Task(pages(pageId)).delayExecution(1.second)
      }

      def esLoad(batch: Seq[TargetRecord]): Task[Unit] = {
        Task(()).delayExecution(500.milliseconds)
      }

      val job = ObservableAdventures.readTransformAndLoadAndExecute(readPage, esLoad)

      val start = System.currentTimeMillis()
      val recordsProcessed = Await.result(job.runToFuture, 1.minute)
      val duration = System.currentTimeMillis() - start

      println(s"Processing took ${duration}ms")

      duration must beLessThan(22.seconds.toMillis) // 1 second buffer for timing issues

      recordsProcessed must beEqualTo(100)
    }
  }

  def runLog[T](observable: Observable[T], timeout: FiniteDuration = 10.seconds): List[T] =
    Await.result(observable.consumeWith(collectingConsumer).runToFuture, timeout).reverse

  /**
    * Consumer which records all elements in a Vector.  Only for test purposes
    */
  private def collectingConsumer[T]: Consumer[T, List[T]] = Consumer.foldLeft(List.empty[T]) { (acc, elem) =>
    elem :: acc
  }
}
