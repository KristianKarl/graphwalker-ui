package org.graphwalker.ui.layout;

/*-
 * #%L
 * GraphWalker UI Application
 * %%
 * Copyright (C) 2005 - 2017 GraphWalker
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import org.graphwalker.ui.model.UIEdge;
import org.graphwalker.ui.model.UIElement;
import org.graphwalker.ui.model.UIGraph;
import org.graphwalker.ui.model.UIVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphWalkerLayout extends Layout {

  private static final Logger logger = LoggerFactory.getLogger(GraphWalkerLayout.class);

  UIGraph graph;
  Random rnd = new Random();
  HashMap<UIVertex, Rectangle> locatedRectangles = new HashMap();
  HashMap<Map<UIVertex, UIVertex>, Integer> edges = new HashMap();
  private double MINIMAL_DISTANCE = 20;
  private double CONTROL_PT_DISTANCE = 100;

  public GraphWalkerLayout() {
    this.graph = null;
  }

  public GraphWalkerLayout(UIGraph graph) {
    this.graph = graph;
  }

  @Override
  public void execute() {
    logger.debug("Running the GraphWalker layout");
    for (UIElement element : graph.getElements()) {
      if (element instanceof UIVertex) {
        placeVertex((UIVertex) element);
      }
    }
    for (UIVertex v : locatedRectangles.keySet()) {
      Rectangle r = locatedRectangles.get(v);
      v.relocate(r.getLayoutX() + 1e6,
                 r.getLayoutY() + 1e6);
    }

    doEdges();
  }

  @Override
  public void doEdges() {
    logger.debug("Running the GraphWalker edge layout");
    for (UIElement element : graph.getElements()) {
      if (element instanceof UIEdge) {
        placeEdges((UIEdge) element);
      }
    }
  }

  @Override
  public void doEdge(UIEdge edge) {
    placeEdges(edge);
  }

  private void placeEdges(UIEdge edge) {
    Integer instances = 1;
    Hashtable<UIVertex, UIVertex> e = new Hashtable();
    e.put(edge.getSource(), edge.getTarget());
    if (edges.containsKey(e)) {
      instances = edges.get(e) + 1;
      edges.put(e, instances);
    } else {
      edges.put(e, instances);
    }

    Point2D p3;
    Point2D p4;
    Point2D startPt = new Point2D(edge.getSource().getX().getValue(), edge.getSource().getY().getValue());
    Point2D endPt = new Point2D(edge.getTarget().getX().getValue(), edge.getTarget().getY().getValue());

    if (edge.getSource() == edge.getTarget()) {
      p3 = new Point2D(startPt.getX() + CONTROL_PT_DISTANCE * instances,
                       startPt.getY());
      p4 = new Point2D(startPt.getX(),
                       startPt.getY() + CONTROL_PT_DISTANCE * instances);
    } else {
      double angle = calcRotationAngleInDegrees(startPt, endPt);

      p3 = new Point2D(endPt.getX() + CONTROL_PT_DISTANCE * instances * Math.sin(angle + 90),
                       endPt.getY() + CONTROL_PT_DISTANCE * instances * Math.cos(angle + 90));
      p4 = new Point2D(startPt.getX() + CONTROL_PT_DISTANCE * instances * Math.sin(angle + 90),
                       startPt.getY() + CONTROL_PT_DISTANCE * instances * Math.cos(angle + 90));
    }

    edge.getCurve().setControlX1(p4.getX());
    edge.getCurve().setControlY1(p4.getY());
    edge.getCurve().setControlX2(p3.getX());
    edge.getCurve().setControlY2(p3.getY());
    edge.update();
  }

  private void placeVertex(UIVertex vertex) {
    if (locatedRectangles.containsKey(vertex)) {
      return;
    }
    Rectangle rectangle = new Rectangle(vertex.getLayoutX() - MINIMAL_DISTANCE,
                                        vertex.getLayoutY() - MINIMAL_DISTANCE,
                                        vertex.getLayoutBounds().getWidth() + MINIMAL_DISTANCE,
                                        vertex.getLayoutBounds().getHeight() + MINIMAL_DISTANCE);

    if (locatedRectangles.size() < 1) {
      Point2D pt = new Point2D(rnd.nextDouble() * graph.getRootPane().getWidth(), rnd.nextDouble() * graph.getRootPane().getHeight());
      rectangle.relocate(pt.getX(), pt.getY());
      locatedRectangles.put(vertex, rectangle);
      return;
    }

    for (UIVertex v : locatedRectangles.keySet()) {
      Rectangle r = locatedRectangles.get(v);
      do {
        Point2D pt = new Point2D(rnd.nextDouble() * graph.getRootPane().getWidth(), rnd.nextDouble() * graph.getRootPane().getHeight());
        rectangle.relocate(pt.getX(), pt.getY());
        logger.debug("Testing pt: " + pt);
      } while (rectangle.getBoundsInParent().intersects(r.getBoundsInParent()));
    }

    locatedRectangles.put(vertex, rectangle);
  }

  /**
   * Calculates the angle from centerPt to targetPt in degrees.
   * The return should range from [0,360), rotating CLOCKWISE,
   * 0 and 360 degrees represents NORTH,
   * 90 degrees represents EAST, etc...
   * <p>
   * Assumes all points are in the same coordinate space.  If they are not,
   * you will need to call SwingUtilities.convertPointToScreen or equivalent
   * on all arguments before passing them  to this function.
   *
   * @param centerPt Point we are rotating around.
   * @param targetPt Point we want to calcuate the angle to.
   * @return angle in degrees.  This is the angle from centerPt to targetPt.
   */
  public static double calcRotationAngleInDegrees(Point2D centerPt, Point2D targetPt) {
    // calculate the angle theta from the deltaY and deltaX values
    // (atan2 returns radians values from [-PI,PI])
    // 0 currently points EAST.
    // NOTE: By preserving Y and X param order to atan2,  we are expecting
    // a CLOCKWISE angle direction.
    double theta = Math.atan2(targetPt.getY() - centerPt.getY(), targetPt.getX() - centerPt.getX());

    // rotate the theta angle clockwise by 90 degrees
    // (this makes 0 point NORTH)
    // NOTE: adding to an angle rotates it clockwise.
    // subtracting would rotate it counter-clockwise
    //theta += Math.PI/2.0;

    // convert from radians to degrees
    // this will give you an angle from [0->270],[-180,0]
    double angle = Math.toDegrees(theta);

    // convert to positive range [0-360)
    // since we want to prevent negative angles, adjust them now.
    // we can assume that atan2 will not return a negative value
    // greater than one partial rotation
    if (angle < 0) {
      angle += 360;
    }

    return angle;
  }
}
