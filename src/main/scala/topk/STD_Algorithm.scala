package topk
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import skyline.{SkylineOperator, domination}
import scala.collection.mutable.ArrayBuffer

class STD_Algorithm(sc: SparkContext, k: Int) extends Serializable {

  def compute(data: RDD[Array[Double]], skylineObj: SkylineOperator, dominanceScore: DominanceScore): ArrayBuffer[PointWithDomScore] = {

    var dataRDD = data

    // Compute the skyline of the dataRDD (data)
    val localSkylines = data.mapPartitions(skylineObj.SFS_Algorithm)
    val skylineSet: ArrayBuffer[Array[Double]] = ArrayBuffer[Array[Double]]()
    localSkylines.collect.foreach(localSkyline => skylineObj.computeFinalSkyline(skylineSet, ArrayBuffer(localSkyline)))

    // Compute the domination score of each point in the skyline
    val skylineWithDomScore: ArrayBuffer[PointWithDomScore] = dominanceScore.calculateScore(skylineSet, dataRDD)

    // Store skyline points in an Array and sort them by their domination score
    var sortedArray: ArrayBuffer[PointWithDomScore] = ArrayBuffer[PointWithDomScore]()
    sortedArray = skylineWithDomScore.sortWith(_.dominanceScore > _.dominanceScore)

    val top_k_Points: ArrayBuffer[PointWithDomScore] = new ArrayBuffer[PointWithDomScore]()

      for(_ <- 0 until math.min(k, sortedArray.length)) {
        // Pick up the point with the highest domination score
        val topK: PointWithDomScore = sortedArray.remove(0)
        // Append it to to ArrayBuffer which contains to top-k Points
        top_k_Points.append(topK)
        // Remove it from the dataRDD
        dataRDD = dataRDD.filter(point => !(topK.p sameElements point))

        // Compute the exclusive domination region of the current top-k point
        // Prepare the skyline and current Top-K Point for broadcast
        val curTop_kPointBC = sc.broadcast(topK)
        val curSkylineBC = sc.broadcast(sortedArray)

        val excl_DominationRegion = dataRDD
          // Find all points dominated by current top-k point
          .filter(point => domination.dominates(curTop_kPointBC.value.p, point))
          // And not dominated by any other point of the skyline
          .filter(point => !curSkylineBC.value.exists(other_point => domination.dominates(other_point.p, point)))

        // Find skyline of the exclusive domination region of current top-k point

        // First, calculate the skyline of each partition
        val localSkylinesOfDomRegion = excl_DominationRegion.mapPartitions(skylineObj.SFS_Algorithm)
        // Secondly, aggregate them into a final skyline set
        val skylineOfDomRegion: ArrayBuffer[Array[Double]] = ArrayBuffer[Array[Double]]()
        localSkylinesOfDomRegion.collect.foreach(localSkyline => skylineObj.computeFinalSkyline(skylineOfDomRegion, ArrayBuffer(localSkyline)))
        if (skylineOfDomRegion.nonEmpty) {
          // Compute the dominance score of each point in the exclusive domination region
          val exclusiveSkylineWithDomScore: ArrayBuffer[PointWithDomScore] = dominanceScore.calculateScore(skylineOfDomRegion, dataRDD)

          // Append the skyline points of the exclusive domination region to the sortedArray and sort it
          sortedArray ++= exclusiveSkylineWithDomScore
          sortedArray = sortedArray.sortWith(_.dominanceScore > _.dominanceScore)
        }
      }
    top_k_Points
  }
}