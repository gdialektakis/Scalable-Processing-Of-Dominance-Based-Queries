import java.io.FileWriter
import org.apache.spark.{SparkConf, SparkContext}
import skyline.SkylineOperator
import topk.{DominanceScore, PointWithDomScore, STD_Algorithm}
import scala.collection.mutable.ArrayBuffer


object Driver {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf()
      .setAppName("Dominance-Based Queries")
      .setMaster("local[*]")
    val sc = new SparkContext(conf)

    val inputFile = "./anticorrelated_4.csv"
    val data = sc.textFile(inputFile)
      .repartition(8)
      .map(_.split(","))
      .map(p => p.map(_.toDouble))

    print("Welcome! \n")
    println("Which task do you want to perform? ")

    println("Type: ")
    println("1 for Skyline Query")
    println("2 for Top-k Dominating Points")
    println("3 for Top-k Dominating Points from Skyline")

    val task = 3

    val start = System.nanoTime()
    val skylineObj = new SkylineOperator()
    task match {

      case 1 =>
        // compute skyline of each partition
        val localSkylines = data.mapPartitions(skylineObj.SFS_Algorithm)
        val finalSkylineSet: ArrayBuffer[Array[Double]] = ArrayBuffer[Array[Double]]()
        // compute final skyline set
        localSkylines.collect.foreach(localSkyline => skylineObj.computeFinalSkyline(finalSkylineSet, ArrayBuffer(localSkyline)))

        // write result to file
        val w = new FileWriter("output/skyline_result.txt")
        w.write(f"------------ Skyline Points are: ------------ \n")
        finalSkylineSet.foreach(point =>
          w.write("Point " + point.mkString("{",",","}") + "\n"))

        w.close()

      case 2 =>  // Compute the top-k points of the dataset

        val dominance: DominanceScore = new DominanceScore(sc)
        val k = 10
        val std = new STD_Algorithm(sc, k)
        var top_k_Points: ArrayBuffer[PointWithDomScore] = std.compute(data, skylineObj, dominance)
        top_k_Points = top_k_Points.sortWith(_.dominanceScore > _.dominanceScore)

        // write result to file
        val w = new FileWriter("output/top-k_result.txt")
        w.write(f"------------ Top-$k%d Points are: ------------ \n")
        top_k_Points.foreach(point =>
          w.write("Point " + point.p.mkString("{",",","}") + " with dominance score: " +  point.dominanceScore + "\n"))

        w.close()

      case 3 => // Compute the top-k points that belong to the skyline

        val k = 10
        val localSkylines = data.mapPartitions(skylineObj.SFS_Algorithm)
        val finalSkylineSet: ArrayBuffer[Array[Double]] = ArrayBuffer[Array[Double]]()
        // compute final skyline set
        localSkylines.collect.foreach(localSkyline => skylineObj.computeFinalSkyline(finalSkylineSet, ArrayBuffer(localSkyline)))

        val dominanceScore = new DominanceScore(sc)
        var top_k_SkylinePoints: ArrayBuffer[PointWithDomScore] = dominanceScore.calculateScore(finalSkylineSet, data)
        top_k_SkylinePoints = top_k_SkylinePoints.sortWith(_.dominanceScore > _.dominanceScore)
        // Select first k points
        top_k_SkylinePoints = top_k_SkylinePoints.take(k)

        // write result to file
        val w = new FileWriter("output/top-k_skyline_result.txt")
        w.write(f"------------ Top-$k%d Points of Skyline are: ------------ \n")
        top_k_SkylinePoints.foreach(point =>
          w.write("Point " + point.p.mkString("{",",","}") + " with dominance score: " +  point.dominanceScore + "\n"))

        w.close()
    }
    println("Total Execution Time: " + (System.nanoTime - start) / 1e9d + "seconds")
  }
}
