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
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import org.graphwalker.ui.model.UIEdge;
import org.graphwalker.ui.model.UIGraph;
import org.graphwalker.ui.model.UIVertex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MouseGestures {

  private static final Logger logger = LoggerFactory.getLogger(MouseGestures.class);
  final DragContext dragContext = new DragContext();
  UIGraph graph;

  public MouseGestures(UIGraph graph) {
    this.graph = graph;
  }

  public void makeDraggable(final Node node) {
    node.setOnMousePressed(onMousePressedEventHandler);
    node.setOnMouseDragged(onMouseDraggedEventHandler);
  }

  public void logHover(final Node node) {
    node.setOnMouseEntered(setOnMouseEntered);
    node.setOnMouseExited(setOnMouseExited);
  }

  EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {
    @Override
    public void handle(MouseEvent event) {
      logger.debug("Clicked");

      // If control modifier key is pressed, ignore
      if (event.isControlDown()) {
        return;
      }

      double scale = graph.getScale();
      Node node = (Node) event.getSource();
      dragContext.x = node.getBoundsInParent().getMinX() * scale - event.getScreenX();
      dragContext.y = node.getBoundsInParent().getMinY() * scale - event.getScreenY();
    }
  };

  EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
    @Override
    public void handle(MouseEvent event) {
      if (event.isShiftDown()) {
        return;
      }
      Node node = (Node) event.getSource();

      double offsetX = event.getScreenX() + dragContext.x;
      double offsetY = event.getScreenY() + dragContext.y;

      // adjust the offset in case we are zoomed
      double scale = graph.getScale();

      offsetX /= scale;
      offsetY /= scale;

      node.relocate(offsetX, offsetY);
      event.consume();
    }
  };

  EventHandler<MouseEvent> setOnMouseEntered = event -> {
    Node node = (Node) event.getSource();
    //node.setEffect(new DropShadow());

    if (!logger.isDebugEnabled()) {
      return;
    }
    JSONObject jsonPosition = new JSONObject();
    JSONObject jsonNode = new JSONObject();
    jsonNode.append("Position", jsonPosition);

    if (node instanceof UIVertex) {
      jsonNode.put("Type", "Vertex");
      jsonNode.put("Name", ((UIVertex) node).getVertex().getName());
      jsonNode.put("Id", ((UIVertex) node).getVertex().getId());
      jsonNode.put("x", ((UIVertex) node).getX().getValue());
      jsonNode.put("y", ((UIVertex) node).getY().getValue());
    } else if (node instanceof UIEdge) {
      jsonNode.put("Type", "Edge");
      jsonNode.put("Name", ((UIEdge) node).getEdge().getName());
      jsonNode.put("Id", ((UIEdge) node).getEdge().getId());
    } else {
      jsonNode.put("Type", "Unknown");
    }

    logger.debug("Entered: " + jsonNode.toString(2));
  };

  EventHandler<MouseEvent> setOnMouseExited = event -> {
    Node node = (Node) event.getSource();
    node.setEffect(null);
    if (node instanceof UIVertex) {
      logger.debug("Exited: " + ((UIVertex) node).getVertex().getName());
    } else if (node instanceof UIEdge) {
      logger.debug("Exited: " + ((UIEdge) node).getEdge().getName());
    }
  };

  class DragContext {

    double x;
    double y;
  }
}
