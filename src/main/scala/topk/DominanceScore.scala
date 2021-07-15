package topk
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import skyline.domination

import scala.collection.mutable.ArrayBuffer


class DominanceScore(sc: SparkContext) extends Serializable {

    def calculateScore(skylineSet: ArrayBuffer[Array[Double]], data: RDD[Array[Double]]): ArrayBuffer[PointWithDomScore] = {

        // Prepare skyline for broadcast
        val skylineBroadcast = sc.broadcast(skylineSet)

        data.mapPartitions( partition => {
          val skyline = skylineBroadcast.value
          // every point of the partition is compared to each point of the skyline
          partition.map(point => skyline.map(skylinePoint => {
            var dom = 0
            if (domination.dominates(skylinePoint, point)) dom = 1
            PointWithDomScore(skylinePoint, dom)
          }))
        })
          .reduce((point1: ArrayBuffer[PointWithDomScore], point2: ArrayBuffer[PointWithDomScore]) => {
           (point1,point2).zipped.map((pair1,pair2) => PointWithDomScore(pair1.p, pair1.dominanceScore + pair2.dominanceScore))
        })
    }

}
