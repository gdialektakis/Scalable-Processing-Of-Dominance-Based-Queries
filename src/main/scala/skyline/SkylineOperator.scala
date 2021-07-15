package skyline
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks

class SkylineOperator extends Serializable {

  def SFS_Algorithm(initialData: Iterator[Array[Double]]): Iterator[Array[Double]] = {
    val data = score(initialData).toArray
    // sort data in ascending order by their score=sum
    val sorted_data = data.sortBy(_._2).toIterator
    // find local skyline
    val skyline = computeLocalSkyline(sorted_data.map(x => x._1))
    skyline
  }

  def score(data: Iterator[Array[Double]]): Iterator[(Array[Double], Double)] = {
    data.map(p => (p, 0))
      .map(p => {
        var sum = 0.0
        for (i <- p._1.indices) {
          sum = sum + p._1(i)
        }
        (p._1, sum)
      })
  }

  def computeLocalSkyline(d: Iterator[Array[Double]]): Iterator[Array[Double]] = {
    var window = ArrayBuffer[Array[Double]]()
    val data = d.toArray

    if (data.isEmpty){return window.toIterator}

    window += data(0)
    for (i <- 1 until data.length) {
      var j = 0
      var discard = false

      val loop = new Breaks
      loop.breakable {
        while (j < window.length) {
          if (domination.dominates(data(i), window(j))) {
            window.remove(j)
            j -= 1
          }
          else if (domination.dominates(window(j), data(i))) {
            discard = true
            loop.break
          }
          j += 1
        }
      }
      if (!discard)
        window.append(data(i))
    }
    window.toIterator
  }

  // computes the final Skyline set and stores it in finalSkylineSet array
  def computeFinalSkyline(finalSkylineSet: ArrayBuffer[Array[Double]], localSkyline: ArrayBuffer[Array[Double]]): ArrayBuffer[Array[Double]] = {

    var firstPoint = false
    if (finalSkylineSet.isEmpty) {
      // the first point of the final Skyline Set is just the first point from the first partition of the Local Skyline
      finalSkylineSet += localSkyline(0)
      firstPoint = true
    }
    // denotes when a local Skyline point needs to be inserted in the final Skyline Set
    var belongsToSkyline = true
    var j = 0
    val loop = new Breaks

    loop.breakable {
      while (j < finalSkylineSet.length) {
        if (firstPoint) {
          /* If the final Skyline Set was previously empty
            and we are looking at the first point that was inserted into it,
            we need to break the loop and do not insert the point to the final Skyline Set again.
           */
          loop.break
        }
        if (domination.dominates(localSkyline(0), finalSkylineSet(j))) {
          /* The local Skyline point i dominates a point from the Final Skyline Set,
           so the point from the final Skyline Set needs to be removed
           */
          belongsToSkyline = true
          finalSkylineSet.remove(j)
          j -= 1
        } else if (domination.dominates(finalSkylineSet(j), localSkyline(0))) {
          /* A point from the final Skyline Set dominates the point i from the local Skyline
           so we break the loop to stop comparing with other points which is unnecessary
           */
          belongsToSkyline = false
          loop.break
        }
        j += 1
      }
    }
    if (belongsToSkyline & !firstPoint) {
      // add the point to the final Skyline Set
      finalSkylineSet.append(localSkyline(0))
    }
    finalSkylineSet
  }
}
