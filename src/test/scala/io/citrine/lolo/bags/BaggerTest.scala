package io.citrine.lolo.bags

import io.citrine.lolo.TestUtils
import io.citrine.lolo.trees.{ClassificationTreeLearner, RegressionTreeLearner}
import org.junit.Test
import org.scalatest.Assertions._

/**
  * Created by maxhutch on 11/29/16.
  */
@Test
class BaggerTest {

  /**
    * Test the fit performance of the regression bagger
    */
  @Test
  def testRegressionBagger(): Unit = {
    val csv = TestUtils.readCsv("large_example_with_cat.csv")
    val trainingData = csv.map(vec => (vec.init, vec.last.asInstanceOf[Double]))
    val DTLearner = new RegressionTreeLearner()
    val baggedLearner = new Bagger(DTLearner, numBags = trainingData.size / 2)
    val N = 0
    val start = System.nanoTime()
    val RFMeta = baggedLearner.train(trainingData)
    val RF = RFMeta.getModel()
    (0 until N).map(i => baggedLearner.train(trainingData))
    val duration = (System.nanoTime() - start) / 1.0e9

    println(s"Training large case took ${duration / (N + 1)} s")

    val results = RF.transform(trainingData.map(_._1))
    val means = results.getExpected()

    /* The first feature should be the most important */
    val importances = RFMeta.getFeatureImportance()
    assert(importances(0) == importances.max)
  }
  /**
    * Test the fit performance of the regression bagger
    */
  @Test
  def testClassificationBagger(): Unit = {
    val csv = TestUtils.readCsv("class_example.csv")
    val trainingData = csv.map(vec => (vec.init, vec.last))
    val DTLearner = new ClassificationTreeLearner()
    val baggedLearner = new Bagger(DTLearner, numBags = trainingData.size / 2)
    val N = 0
    val start = System.nanoTime()
    val RFMeta = baggedLearner.train(trainingData)
    val RF = RFMeta.getModel()
    (0 until N).map(i => baggedLearner.train(trainingData))
    val duration = (System.nanoTime() - start) / 1.0e9

    println(s"Training classification forest took ${duration / (N + 1)} s")

    val results = RF.transform(trainingData.map(_._1))
    val means = results.getExpected()

    /* The first feature should be the most important */
    val importances = RFMeta.getFeatureImportance()
    assert(importances(0) == importances.max)
  }

  /**
    * Test the scores on a smaller example, because computing them all can be expensive.
    *
    * In general, we don't even know that the self-score (score on a prediction on oneself) is maximal.  For example,
    * consider a training point that is sandwiched between two other points, i.e. y in | x     x y x    x |.  However,
    * this training data is on a 2D grid, so we know the corners of that grid need to have maximal self-scores.  Those
    * are at indices 0, 7, 56, and 63.
    */
  @Test
  def testScores(): Unit = {
    val csv = TestUtils.readCsv("double_example.csv")
    val trainingData = csv.map(vec => (vec.init, vec.last.asInstanceOf[Double]))
    val DTLearner = new RegressionTreeLearner()
    val baggedLearner = new Bagger(DTLearner, numBags = trainingData.size * 16) // use lots of trees to reduce noise
    val RF = baggedLearner.train(trainingData).getModel()

    /* Call transform on the training data */
    val results = RF.transform(trainingData.map(_._1))
    val scores = results.getScores()
    val corners = Seq(0, 7, 56, 63)
    assert(
      corners.forall(i => scores(i)(i) == scores(i).max),
      "One of the training corners didn't have the highest score"
    )
  }
}

/**
  * Companion driver
  */
object BaggerTest {
  /**
    * Test driver
    *
    * @param argv args
    */
  def main(argv: Array[String]): Unit = {
    // new BaggerTest().testRegressionBagger()
    new BaggerTest().testScores()
  }
}
