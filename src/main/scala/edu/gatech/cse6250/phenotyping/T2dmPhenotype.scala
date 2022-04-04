package edu.gatech.cse6250.phenotyping

import edu.gatech.cse6250.model.{Diagnostic, LabResult, Medication}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions._
import org.spark_project.dmg.pmml.True
import java.text.SimpleDateFormat



/**
 * @author Hang Su <hangsu@gatech.edu>,
 * @author Sungtae An <stan84@gatech.edu>,
 */
object T2dmPhenotype {

  /** Hard code the criteria */
  val T1DM_DX = Set("250.01", "250.03", "250.11", "250.13", "250.21", "250.23", "250.31", "250.33", "250.41", "250.43",
    "250.51", "250.53", "250.61", "250.63", "250.71", "250.73", "250.81", "250.83", "250.91", "250.93")

  val T2DM_DX = Set("250.3", "250.32", "250.2", "250.22", "250.9", "250.92", "250.8", "250.82", "250.7", "250.72", "250.6",
    "250.62", "250.5", "250.52", "250.4", "250.42", "250.00", "250.02")

  val T1DM_MED = Set("lantus", "insulin glargine", "insulin aspart", "insulin detemir", "insulin lente", "insulin nph", "insulin reg", "insulin,ultralente")

  val T2DM_MED = Set("chlorpropamide", "diabinese", "diabanase", "diabinase", "glipizide", "glucotrol", "glucotrol xl",
    "glucatrol ", "glyburide", "micronase", "glynase", "diabetamide", "diabeta", "glimepiride", "amaryl",
    "repaglinide", "prandin", "nateglinide", "metformin", "rosiglitazone", "pioglitazone", "acarbose",
    "miglitol", "sitagliptin", "exenatide", "tolazamide", "acetohexamide", "troglitazone", "tolbutamide",
    "avandia", "actos", "actos", "glipizide")

  def sqlDateParser(input: String, pattern: String = "yyyy-MM-dd'T'HH:mm:ssX"): java.sql.Date = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    new java.sql.Date(dateFormat.parse(input).getTime)
  }


  /**
   * Transform given data set to a RDD of patients and corresponding phenotype
   *
   * @param medication medication RDD
   * @param labResult  lab result RDD
   * @param diagnostic diagnostic code RDD
   * @return tuple in the format of (patient-ID, label). label = 1 if the patient is case, label = 2 if control, 3 otherwise
   */
  def transform(medication: RDD[Medication], labResult: RDD[LabResult], diagnostic: RDD[Diagnostic]): RDD[(String, Int)] = {
    /**
     * Remove the place holder and implement your code here.
     * Hard code the medication, lab, icd code etc. for phenotypes like example code below.
     * When testing your code, we expect your function to have no side effect,
     * i.e. do NOT read from file or write file
     *
     * You don't need to follow the example placeholder code below exactly, but do have the same return type.
     *
     * Hint: Consider case sensitivity when doing string comparisons.
     */
    val sc = medication.sparkContext
    /** Hard code the criteria */
    // val type1_dm_dx = Set("code1", "250.03")
    // val type1_dm_med = Set("med1", "insulin nph")
    // use the given criteria above like T1DM_DX, T2DM_DX, T1DM_MED, T2DM_MED and hard code DM_RELATED_DX criteria as well
    val DM_RELATED_DX = Set("250.*","256.4","V77.1","277.7","791.5","648.04","648.03","648.02","648.01","648","648",
      "648.84","648.83","648.82","648.81","790.29","790.2","790.22","790.21")
    val ABNORMAL_LAB_VALUES_CONTROL = Set("HbA1c", "Hemoglobin A1c", "Fasting Glucose", "Fasting blood glucose", "fasting plasma glucose",
    "Glucose", "glucose", "Glucose, Serum")

     /** Find CASE Patients */
    //// step 1: only keep patients who do not have a diagnosis in Type1DM_DX
    val removed_patients = diagnostic.filter(x => T1DM_DX.contains(x.code)).map(x=>x.patientID).distinct.collect()
    val case_diag_step1 = diagnostic.filter(x => !removed_patients.contains(x.patientID))
    //// step 2:
    val case_diag_step2 = case_diag_step1.filter(x=> T2DM_DX.contains(x.code))
    val case_patients_step2 = case_diag_step2.map(x=>x.patientID).distinct.collect()
    //// step 3:
    val case_diag_step2_grouped = case_diag_step2.map(x=>(x.patientID, x))
    val medication_grouped = medication.map(x=>(x.patientID, x))
    val case_medications = case_diag_step2_grouped.leftOuterJoin(medication_grouped).map(x=>(x._2._1.patientID, x._2._1.code, x._2._2.getOrElse(Medication("null", sqlDateParser("1900-01-01T00:00:00Z"), "null"))))
    val case_medications2 = case_medications.map( x=> (x._1, x._2, x._3.medicine, x._3.date))

    val further_ids = case_medications2.filter(x => T1DM_MED.contains(x._3.toLowerCase())).map(x => x._1).distinct.collect()
    val further_checks = case_medications2.filter(x => further_ids.contains(x._1))
    val case_1 = case_medications2.filter(x => !further_ids.contains(x._1))
    //// step 4:
    // check for type 2 DM medications
    val ids_step4 = further_checks.filter(x => T2DM_MED.contains(x._3.toLowerCase())).map(x=>x._1).distinct.collect()
    val output_step4 =  further_checks.filter(x=>ids_step4.contains(x._1))
    val case_2 = further_checks.filter(x => !ids_step4.contains(x._1))
    //// step 5:
    val t2_step5 = output_step4.filter(x=>T2DM_MED.contains(x._3.toLowerCase())).map {x =>
      (x._1, x._4.getTime)
    }
    val t2_min = t2_step5.reduceByKey(math.min).map(x=>(x._1, new java.sql.Date(x._2)))
    val t1_step5 = output_step4.filter(x=>T1DM_MED.contains(x._3.toLowerCase())).map {x =>
      (x._1, x._4.getTime)
    }
    val t1_min = t1_step5.reduceByKey(math.min).map(x=>(x._1, new java.sql.Date(x._2)))
    val ids_step5 = t2_min.join(t1_min).filter(x=>x._2._1.compareTo(x._2._2)<0).map(x=>x._1).distinct.collect()
    val case_3 = output_step4.filter(x=>ids_step5.contains(x._1))
    val casePatients = case_1.map(x=>(x._1, 1)).union(case_2.map(x=>(x._1, 1))).union(case_3.map(x=>(x._1, 1))).distinct()

    /** Find CONTROL Patients */
    //val controlPatients = sc.parallelize(Seq(("controlPatients-one", 2), ("controlPatients-two", 2), ("controlPatients-three", 2)))
    // we already have medication_grouped. Need to get lab values grouped and dx grouped
    val labResult_grouped = labResult.map(x=>(x.patientID, x))
    val diag_grouped = diagnostic.map(x=>(x.patientID, x))
    val all_patients = medication_grouped.leftOuterJoin(labResult_grouped).leftOuterJoin(diag_grouped)
    //// Step 1: Filter where glucose test exists
    val step1_ids = labResult.filter(x=>x.testName.toLowerCase().contains("glucose")).map(x=>x.patientID).distinct.collect()
    val step1_output_control = labResult.filter(x => step1_ids.contains(x.patientID))
    //// Step 2: Check for abnormal lab values
    val step2_ids_control = step1_output_control.map{x =>
      var abnormal = 0
      if (x.testName.toLowerCase() == "hba1c" && x.value>=6) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "hemoglobin a1c" && x.value >= 6) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "fasting glucose" && x.value >= 110) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "fasting blood glucose" && x.value >= 110) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "fasting plasma glucose" && x.value >= 110) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "glucose" && x.value > 110) {
        abnormal = 1
      } else if (x.testName.toLowerCase() == "glucose, serum" && x.value > 110) {
        abnormal = 1
      } else {
        abnormal = 0
      }
      (x.patientID, abnormal)
    }.filter(x=>x._2==1).map(x=>x._1).distinct().collect()
    val step2_output_control = step1_output_control.filter(x => !step2_ids_control.contains(x.patientID)).map(x=>x.patientID).distinct().collect()
    //// step 3: check for diabetes related dx's
    val step3_ids_control = diagnostic.filter { x =>
      DM_RELATED_DX.contains(x.code) || x.code.contains("250.")
    }.map(x => x.patientID).distinct().collect()
    val step3_output_control = diagnostic.filter{x =>
      (step2_output_control.contains(x.patientID)) && !(step3_ids_control.contains(x.patientID))
    }
    val controlPatients = step3_output_control.map(x => (x.patientID, 2)).distinct()

    /** Find OTHER Patients */
    val case_ids = casePatients.map(x=>x._1).collect()
    val control_ids = controlPatients.map(x=>x._1).collect()
    val unknown_ids = diagnostic.filter{x =>
      !(case_ids.contains(x.patientID)) && !(control_ids.contains(x.patientID))
    }.distinct()
    val others = unknown_ids.map(x => (x.patientID, 3)).distinct()

    /** Once you find patients for each group, make them as a single RDD[(String, Int)] */
    val phenotypeLabel = sc.union(casePatients, controlPatients, others).distinct()

    /** Return */
    phenotypeLabel
  }
}