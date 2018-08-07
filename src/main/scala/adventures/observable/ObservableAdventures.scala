package adventures.observable

import adventures.observable.model.{PageId, PaginatedResult, SourceRecord, TargetRecord}
import monix.eval.Task
import monix.reactive.Observable

import scala.util.{Failure, Success, Try}

/**
  * If elements from a list can be operated on synchronously as a List[A], then the equivalent data structure where
  * those elements can be operated asynchronously could be represented as a Observable[A].
  *
  * These exercises will introduce you to a common problem ETL pipeline.  The steps to complete this are.
  * 1. Read from a external paginated datasource (no need to worry about duplicate data, assume data will only come through
  * once).
  * 2. Transform that data (filtering out any invalid data)
  * 3. Insert that data into elasticsearch (which has an efficient API to insert in batches of 50)
  */
object ObservableAdventures {

  /**
    * For this exercise, think about how you would implement the following without Monix.
    *
    * Given an Iterable or record ids, how would you go about loading each of those records asynchronously?
    *
    * This exercise doesn't have to be implemented, but just think about what this would look like.  Would you model the
    * return type as:
    * - Iterable[TargetRecord] (what does this imply must happen?)
    * - Iterable[Future[TargetRecord]] (what is then responsible for handling the asynchronous nature?)
    *
    * What would you do if you needed back pressure (i.e. if something else consuming from the iterable slowed down, how
    * would this propagate?)
    */
  def listToObservable(records: List[SourceRecord]): Observable[SourceRecord] = {
    Observable.fromIterable(records)
  }

  /**
    * Transform all of the SourceRecords to TargetRecords.  If the price cannot be converted to a double,
    * then drop the Source element.
    *
    * @param sourceRecords
    * @return
    */
  def transform(sourceRecords: Observable[SourceRecord]): Observable[TargetRecord] = {
    sourceRecords.flatMap { source =>
      Try(source.price.toDouble) match {
        case Failure(_) => Observable.empty
        case Success(price) => Observable(TargetRecord(source.id, price))
      }
    }
  }

  /**
    * Elastic search supports saving batches of 5 records.  This is a remote async call so the result is represented
    * by `Task`
    *
    * Returns the number of records which were saved to elastic search
    */
  def load(targetRecords: Observable[TargetRecord], elasticSearchLoad: Seq[TargetRecord] => Task[Unit]): Observable[Int] = {
    targetRecords.bufferTumbling(5).mapAsync { batch =>
      elasticSearchLoad(batch).map(_ => batch.length)
    }
  }

  /**
    * Elastic search supports saving batches of 5 records.  This is a remote async call so the result is represented
    * by `Task`.  Note that the elasticSearchLoad may fail (in practice this is pretty rare).  Rather than the Observable terminating with an error,
    * try using the Task retry logic you created earlier.
    *
    * Returns the number of records which were saved to elastic search.
    */
  def loadWithRetry(targetRecords: Observable[TargetRecord], elasticSearchLoad: Seq[TargetRecord] => Task[Unit]): Observable[Int] = {
    load(targetRecords, elasticSearchLoad)
  }

  /**
    * Consume the Observable
    *
    * The final result should be the number of records which were saved to ElasticSearch.
    */
  def execute(loadedObservable: Observable[Int]): Task[Int] = {
    loadedObservable.sumL
  }

  /**
    * Create an Observable from which all records can be read.  Earlier we created "listToObservable", but what if the
    * source data comes from a paginated datasource.
    *
    * The first page of data can be obtained using `PageId.FirstPage`, after which you should follow the nextPage
    * references in the PaginatedResult.
    *
    * Look at Observable.tailRecM
    */
  def readFromPaginatedDatasource(readPage: PageId => Task[PaginatedResult]): Observable[SourceRecord] = {
    def scanPages(pageId: PageId): Observable[Either[PageId, SourceRecord]] = {
      Observable.fromTask(readPage(pageId)).flatMap { paginatedResult =>
        Observable.fromIterable(paginatedResult.results).map(Right(_)) ++
          continue(paginatedResult.nextPage)
      }
    }

    def continue(maybeNextPage: Option[PageId]): Observable[Either[PageId, SourceRecord]] = {
      maybeNextPage match {
        case Some(pageId) => Observable(Left(pageId))
        case None => Observable.empty
      }
    }

    Observable.tailRecM(PageId.FirstPage)(scanPages)
  }

}
