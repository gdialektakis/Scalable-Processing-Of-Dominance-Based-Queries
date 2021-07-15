package skyline

object domination extends Serializable {

  /* A point p dominates another point q, if it's equal or smaller than q in all dimensions
  and smaller in a least one dimension.
  Returns true if p dominates q.
   */
  def dominates(p: Array[Double], q: Array[Double]): Boolean = {
    val max_dim = p.length
    var dominates_1 = false
    var dominates_2 = true

    for (dim <- 0 until max_dim) {
      if (p(dim) < q(dim))
        dominates_1 = true
      if (q(dim) < p(dim)) {
        dominates_2 = false
      }
    }
    dominates_1 & dominates_2
  }
}
