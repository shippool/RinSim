package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.Iterator;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.HeuristicPath;
import com.github.rinde.rinsim.geom.Point;

final class PlaneModelSnapshot extends AbstractModelSnapshot {

  private final Point min;
  private final Point max;

  public PlaneModelSnapshot(Point minimum, Point maximum,
      Unit<Length> planeDistanceUnit) {
    super(planeDistanceUnit);
    min = minimum;
    max = maximum;
  }

  @Override
  public HeuristicPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, Graphs.Heuristic heuristic) {
    final double distance = Point.distance(from, to);
    return new HeuristicPath(getShortestPathTo(from, to),
      distance, RoadModels.computeTravelTime(speed,
        Measure.valueOf(distance, distanceUnit), timeUnit));
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path) {
    final Iterator<Point> pathIt = path.iterator();
    checkArgument(pathIt.hasNext(),
      "cannot check distance of an empty path.");
    Point prev = pathIt.next();
    double distance = 0d;
    Point cur = null;
    while (pathIt.hasNext()) {
      cur = pathIt.next();
      distance += Point.distance(cur, prev);
      prev = cur;
    }
    return Measure.valueOf(distance, distanceUnit);
  }

  private List<Point> getShortestPathTo(Point from, Point to) {
    checkArgument(
      isPointInBoundary(from),
      "from must be within the predefined boundary of the plane, from is %s, "
        + "boundary: min %s, max %s.",
      to, min, max);
    checkArgument(
      isPointInBoundary(to),
      "to must be within the predefined boundary of the plane, to is %s,"
        + " boundary: min %s, max %s.",
      to, min, max);
    return asList(from, to);
  }

  /**
   * Checks whether the specified point is within the plane as defined by this
   * model.
   * @param p The point to check.
   * @return <code>true</code> if the points is within the boundary,
   *         <code>false</code> otherwise.
   */
  protected boolean isPointInBoundary(Point p) {
    return p.x >= min.x && p.x <= max.x && p.y >= min.y && p.y <= max.y;
  }
}
