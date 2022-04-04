/**
 * @author Hang Su <hangsu@gatech.edu>.
 */

package edu.gatech.cse6250.clustering

import org.apache.spark.rdd.RDD
import edu.gatech.cse6250.helper.{ CSVHelper, SparkHelper }
import org.apache.spark.sql.functions._

object Metrics {
  /**
   * Given input RDD with tuples of assigned cluster id by clustering,
   * and corresponding real class. Calculate the purity of clustering.
   * Purity is defined as
   * \fract{1}{N}\sum_K max_j |w_k \cap c_j|
   * where N is the number of samples, K is number of clusters and j
   * is index of class. w_k denotes the set of samples in k-th cluster
   * and c_j denotes set of samples of class j.
   *
   * @param clusterAssignmentAndLabel RDD in the tuple format
   *                                  (assigned_cluster_id, class)
   * @return
   */
  def purity(clusterAssignmentAndLabel: RDD[(Int, Int)]): Double = {
    /**
     * TODO: Remove the placeholder and implement your code here
     */
    val spark = SparkHelper.spark
    import spark.implicits._


    val N = clusterAssignmentAndLabel.count()
    val cluster_max = clusterAssignmentAndLabel.toDF("assigned", "class").groupBy("assigned", "class").count().groupBy("assigned").max().agg(sum("max(count)")).first.getAs[Long](0)
    (cluster_max*1.0 / N)


  }
}
