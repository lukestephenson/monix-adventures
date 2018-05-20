package adventures.observable

import adventures.observable.model.{PageId, PaginatedResult, SourceRecord, TargetRecord}
import monix.eval.Task
import monix.reactive.Observable

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
    * Create an Observable which emits each element of the source list
    */
  def listToObservable(records: List[SourceRecord]): Observable[SourceRecord] = ???

  /**
    * Create an Observable from which all records can be read.
    *
    * The first page of data can be obtained using `PageId.FirstPage`, after which you should follow the nextPage
    * references in the PaginatedResult.
    *
    * Look at Observable.tailRecM
    */
  def readFromLegacyDatasource(readPage: PageId => Task[PaginatedResult]): Observable[SourceRecord] = ???

  /**
    * Transform all of the SourceRecords to TargetRecords.  If the price cannot be converted to a double,
    * then drop the Source element.
    *
    * @param sourceRecords
    * @return
    */
  def transform(sourceRecords: Observable[SourceRecord]): Observable[TargetRecord] = ???

  /**
    * Elastic search supports saving batches of 5 records.  This is a remote async call so the result is represented
    * by `Task`
    *
    * Returns the number of records which were saved to elastic search
    */
  def load(targetRecords: Observable[TargetRecord], elasticSearchLoad: Seq[TargetRecord] => Task[Unit]): Observable[Int] = ???

  /**
    * Consume the Observable
    *
    * The final result should be the number of records which were saved to ElasticSearch
    */
  def execute(loadedObservable: Observable[Int]): Task[Int] = ???
}
