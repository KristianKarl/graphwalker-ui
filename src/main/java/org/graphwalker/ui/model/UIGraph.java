package org.graphwalker.ui.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.graphwalker.ui.application.GraphWalker;
import org.graphwalker.ui.layout.GraphWalkerLayout;
import org.graphwalker.ui.misc.ModelDialogs;
import org.graphwalker.ui.misc.MouseGestures;
import org.graphwalker.ui.misc.ZoomableScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIGraph {

  private static final Logger logger = LoggerFactory.getLogger(UIGraph.class);
  private static final String DEFAULT_GRAPH_GENERATOR = "random(edge_coverage(100))";

  private GraphWalker graphWalker;
  private StackPane root;
  private ZoomableScrollPane scrollPane;
  MouseGestures mouseGestures;
  List<UIElement> elements = new ArrayList<>();
  Model model = new Model();

  Line rubberLine;
  UIVertex startVertex;
  SimpleStringProperty modelName;
  SimpleStringProperty generator;

  /**
   * the pane wrapper is necessary or else the scrollpane would always align
   * the top-most and left-most child to the top and left eg when you drag the
   * top child down, the entire scrollpane would move down
   */
  Pane contentPane;

  public UIGraph(GraphWalker graphWalker) {

    this.graphWalker = graphWalker;
    root = new StackPane();

    rubberLine = new Line();
    rubberLine.setDisable(true);
    rubberLine.setMouseTransparent(true);

    mouseGestures = new MouseGestures(this);

    contentPane = new Pane();
    contentPane.setPrefWidth(1e6);
    contentPane.setPrefHeight(1e6);

    contentPane.setOnMouseClicked(mouseEvent -> {
      logger.debug("ContentPane clicked");
      if (!getRubberLine().isDisabled()) {
        return;
      }

      if (mouseEvent.getButton().equals(MouseButton.PRIMARY) &&
          mouseEvent.isControlDown()) {
        logger.debug("Left click and Control @: " + mouseEvent.getX() + ", " + mouseEvent.getY());
        logger.debug("                   scale: " + getScale());

        Vertex vertex = new Vertex().setName("New State").setId(UUID.randomUUID().toString());
        vertex.setProperty("x", mouseEvent.getX());
        vertex.setProperty("y", mouseEvent.getY());
        addVertex(new UIVertex(this, vertex));
      } else if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        logger.debug("Deselect all");
        for (UIElement element : elements) {
          element.selected(false);
        }
      } else if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
        logger.debug("Right mouse button clicked on the graph");
      }
    });

    contentPane.setOnMouseMoved(mouseEvent -> {
      if (!rubberLine.isDisabled()) {
        rubberLine.setEndX(mouseEvent.getX());
        rubberLine.setEndY(mouseEvent.getY());
      }
    });

    generator = new SimpleStringProperty(DEFAULT_GRAPH_GENERATOR);

    scrollPane = new ZoomableScrollPane(contentPane);
    root.getChildren().addAll(scrollPane);

    modelName = new SimpleStringProperty();

    final ContextMenu contextMenu = new ContextMenu();

    MenuItem dataMenu = new MenuItem("Model properties...");
    dataMenu.setOnAction(e -> {
      if (new ModelDialogs().runModel(this).isPresent()) {
        graphWalker.setTabLabel(this, graphWalker.getTabPane().getSelectionModel().getSelectedItem());
      }
    });

    contextMenu.getItems().add(dataMenu);

    contentPane.setOnContextMenuRequested(event -> {
      contextMenu.show(contentPane, event.getScreenX(), event.getScreenY());
      event.consume();
    });
  }

  public Pane getRootPane() {
    return this.root;
  }

  public double getScale() {
    return this.scrollPane.getScaleValue();
  }

  public void addVertex(UIVertex vertex) {
    logger.debug("Adding vertex: " + vertex);
    elements.add(vertex);
    contentPane.getChildren().add(vertex);
    mouseGestures.makeDraggable(vertex);
    mouseGestures.logHover(vertex);
  }

  public void addEdge(UIEdge edge) {
    elements.add(edge);
    contentPane.getChildren().add(edge);
    mouseGestures.logHover(edge);
  }

  public List<UIElement> getElements() {
    return elements;
  }

  public Model getModel() {
    return model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public void setGenerator(String generator) {
    this.generator.set(generator);
  }

  public String getGenerator() {
    return generator.getValue();
  }

  public void removeAllUIElements() {
    logger.debug("Removing all UI elements from graph");
    contentPane.getChildren().clear();
  }

  public void clear() {
    setGenerator("");
    elements.clear();
    model = null;
    removeAllUIElements();
  }

  public void fitGraphInWindow() {
    scrollPane.zoomToFit();
  }

  public void startCreateEdge(UIVertex startVertex) {
    this.startVertex = startVertex;
    rubberLine.startXProperty().bind(startVertex.getX());
    rubberLine.startYProperty().bind(startVertex.getY());
    rubberLine.setEndX(startVertex.getX().getValue());
    rubberLine.setEndY(startVertex.getY().getValue());
    rubberLine.setDisable(false);
    contentPane.getChildren().add(rubberLine);
  }

  public void endCreateEdge(UIVertex endVertex) {
    contentPane.getChildren().remove(rubberLine);
    rubberLine.setDisable(true);
    UIEdge edge =
      new UIEdge(graphWalker,
                 startVertex,
                 endVertex,
                 new Edge()
                   .setSourceVertex(startVertex.getVertex())
                   .setTargetVertex(endVertex.getVertex())
                   .setName("New Transition")
                   .setId(UUID.randomUUID().toString()));
    addEdge(edge);
    new GraphWalkerLayout(this).doEdge(edge);
  }

  public Line getRubberLine() {
    return rubberLine;
  }

  public Pane getContentPane() {
    return contentPane;
  }

  public GraphWalker getGraphWalker() {
    return graphWalker;
  }

  public ZoomableScrollPane getScrollPane() {
    return scrollPane;
  }
}
