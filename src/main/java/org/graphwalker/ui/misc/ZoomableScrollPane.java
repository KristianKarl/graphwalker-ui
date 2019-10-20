package org.graphwalker.ui.misc;

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

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import org.graphwalker.ui.model.UIVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZoomableScrollPane extends ScrollPane {

  private static final Logger logger = LoggerFactory.getLogger(ZoomableScrollPane.class);

  Group zoomGroup;
  Group contentGroup;
  Scale scaleTransform;
  Node content;
  double scaleValue = 1.0;
  double delta = 0.1;

  public ZoomableScrollPane(Node content) {
    this.content = content;
    contentGroup = new Group();
    zoomGroup = new Group();
    contentGroup.getChildren().add(zoomGroup);
    zoomGroup.getChildren().add(content);
    setContent(contentGroup);

    scaleTransform = new Scale(scaleValue, scaleValue, 0, 0);
    zoomGroup.getTransforms().add(scaleTransform);

    zoomGroup.setOnScroll(new ZoomHandler());

    setPannable(true);
    setVvalue(0.5);
    setHvalue(0.5);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    setVbarPolicy(ScrollBarPolicy.NEVER);
  }

  public double getScaleValue() {
    return scaleValue;
  }

  public void zoomToActual() {
    zoomTo(1.0);
  }

  public void zoomTo(double scaleValue) {
    logger.debug("Scale to: " + scaleValue);
    if (scaleValue > 0.01) {
      this.scaleValue = scaleValue;
    } else {
      this.scaleValue = 0.01;
    }

    double v = getVvalue();
    double h = getHvalue();
    logger.debug("v, h: " + v + ", " + h);
    scaleTransform.setX(this.scaleValue);
    scaleTransform.setY(this.scaleValue);
    setVvalue(v);
    setHvalue(h);
  }

  public void zoomActual() {
    scaleValue = 1;
    zoomTo(scaleValue);
  }

  public void zoomOut() {
    scaleValue -= delta;
    if (Double.compare(scaleValue, 0.1) < 0) {
      scaleValue = 0.1;
    }
    zoomTo(scaleValue);
  }

  public void zoomIn() {
    scaleValue += delta;
    if (Double.compare(scaleValue, 10) > 0) {
      scaleValue = 10;
    }
    zoomTo(scaleValue);

  }

  public void zoomToFit() {

    double left = 0;
    double right = 0;
    double upper = 0;
    double lower = 0;

    boolean isFirstVertexDone = false;
    for (Node node : ((Pane) content).getChildren()) {
      if (node instanceof UIVertex) {
        if (isFirstVertexDone) {
          if (node.getBoundsInParent().getMinX() < left) {
            left = node.getBoundsInParent().getMinX();
          }
          if (node.getBoundsInParent().getMaxX() > right) {
            right = node.getBoundsInParent().getMaxX();
          }
          if (node.getBoundsInParent().getMinY() < upper) {
            upper = node.getBoundsInParent().getMinY();
          }
          if (node.getBoundsInParent().getMaxY() > lower) {
            lower = node.getBoundsInParent().getMaxY();
          }
        } else {
          isFirstVertexDone = true;
          left = node.getBoundsInParent().getMinX();
          right = node.getBoundsInParent().getMaxX();
          upper = node.getBoundsInParent().getMinY();
          lower = node.getBoundsInParent().getMaxY();
        }
      }
    }
    double width = right - left;
    double height = lower - upper;

    if (width <= 0 || height <= 0) {
      logger.debug("No vertices, no scrolling nor scaling");
      return;
    }

    double scaleX = getViewportBounds().getWidth() / width;
    double scaleY = getViewportBounds().getHeight() / height;
    double scale = Math.min(scaleX, scaleY);

    double h = (left + width / 2) / 1e6;
    double v = (upper + height / 2) / 1e6;
    logger.debug("h, v: " + h + ", " + v);

    setVvalue(v);
    setHvalue(h);
    zoomTo(scale);
  }

  private class ZoomHandler implements EventHandler<ScrollEvent> {

    @Override
    public void handle(ScrollEvent scrollEvent) {
      if (scrollEvent.getDeltaY() < 0) {
        scaleValue -= delta;
      } else {
        scaleValue += delta;
      }

      zoomTo(scaleValue);
      scrollEvent.consume();
    }
  }
}
