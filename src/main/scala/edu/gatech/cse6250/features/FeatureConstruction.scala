package edu.gatech.cse6250.features

import edu.gatech.cse6250.model.{ Diagnostic, LabResult, Medication }
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{ Vector, Vectors }
import org.apache.spark.rdd.RDD

/**
 * @author Hang Su
 */
object FeatureConstruction {

  /**
   * ((patient-id, feature-name), feature-value)
   */
  type FeatureTuple = ((String, String), Double)

  /**
   * Aggregate feature tuples from diagnostic with COUNT aggregation,
   *
   * @param diagnostic RDD of diagnostic
   * @return RDD of feature tuples
   */
  def constructDiagnosticFeatureTuple(diagnostic: RDD[Diagnostic]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val diagnostic_grouped = diagnostic.map(x => ((x.patientID, x.code), x.date)).groupByKey()
    val diag_counts = diagnostic_grouped.map(x => (x._1,x._2.size.toDouble))
    diag_counts
  }

  /**
   * Aggregate feature tuples from medication with COUNT aggregation,
   *
   * @param medication RDD of medication
   * @return RDD of feature tuples
   */
  def constructMedicationFeatureTuple(medication: RDD[Medication]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val med_grouped = medication.map(x => ((x.patientID, x.medicine.toLowerCase()), x.date)).groupByKey()
    val med_counts = med_grouped.map(x => (x._1, x._2.size.toDouble))
    med_counts
  }

  /**
   * Aggregate feature tuples from lab result, using AVERAGE aggregation
   *
   * @param labResult RDD of lab result
   * @return RDD of feature tuples
   */
  def constructLabFeatureTuple(labResult: RDD[LabResult]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val labResult_grouped = labResult.map(x => ((x.patientID, x.testName.toLowerCase()), x.value)).groupByKey()
    val lab_avg = labResult_grouped.mapValues(x=> x.sum / x.size)
    lab_avg
  }

  /**
   * Aggregate feature tuple from diagnostics with COUNT aggregation, but use code that is
   * available in the given set only and drop all others.
   *
   * @param diagnostic   RDD of diagnostics
   * @param candiateCode set of candidate code, filter diagnostics based on this set
   * @return RDD of feature tuples
   */
  def constructDiagnosticFeatureTuple(diagnostic: RDD[Diagnostic], candidateCode: Set[String]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val diag_filter = diagnostic.filter(x => candidateCode.contains(x.code.toLowerCase()))
    val diag_count = constructDiagnosticFeatureTuple(diag_filter)
    diag_count
  }

  /**
   * Aggregate feature tuples from medication with COUNT aggregation, use medications from
   * given set only and drop all others.
   *
   * @param medication          RDD of diagnostics
   * @param candidateMedication set of candidate medication
   * @return RDD of feature tuples
   */
  def constructMedicationFeatureTuple(medication: RDD[Medication], candidateMedication: Set[String]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val med_filter = medication.filter(x => candidateMedication.contains(x.medicine.toLowerCase()))
    val med_count = constructMedicationFeatureTuple(med_filter)
    med_count
  }

  /**
   * Aggregate feature tuples from lab result with AVERAGE aggregation, use lab from
   * given set of lab test names only and drop all others.
   *
   * @param labResult    RDD of lab result
   * @param candidateLab set of candidate lab test name
   * @return RDD of feature tuples
   */
  def constructLabFeatureTuple(labResult: RDD[LabResult], candidateLab: Set[String]): RDD[FeatureTuple] = {
    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    val lab_filter = labResult.filter(x => candidateLab.contains(x.testName.toLowerCase()))
    val lab_avg = constructLabFeatureTuple(lab_filter)
    lab_avg
  }

  /**
   * Given a feature tuples RDD, construct features in vector
   * format for each patient. feature name should be mapped
   * to some index and convert to sparse feature format.
   *
   * @param sc      SparkContext to run
   * @param feature RDD of input feature tuples
   * @return
   */
  def construct(sc: SparkContext, feature: RDD[FeatureTuple]): RDD[(String, Vector)] = {

    /** save for later usage */
    feature.cache()

    /** create a feature name to id map */
    val featureMap = feature.map(x=>x._1._2).distinct.collect().zipWithIndex.toMap

    /** transform input feature */
    val scFeatureMap = sc.broadcast(featureMap)
    val num_features = scFeatureMap.value.size

    val feature_transform = feature.map{x =>
      val pID = x._1._1
      val feature_name = x._1._2
      val feature_val = x._2
      (pID, (scFeatureMap.value(feature_name), feature_val))
    }.groupByKey()
    val result = feature_transform.map{case(pID, feat) =>
      val featureVector = Vectors.sparse(num_features, feat.toList)
      (pID, featureVector)
    }
    result

    /**
     * Functions maybe helpful:
     * collect
     * groupByKey
     */

    /**
     * TODO implement your own code here and remove existing
     * placeholder code
     */
    /** The feature vectors returned can be sparse or dense. It is advisable to use sparse */

  }
}

