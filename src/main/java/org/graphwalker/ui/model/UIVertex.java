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

import static org.graphwalker.core.common.Objects.isNotNullOrEmpty;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.graphwalker.core.model.Vertex;
import org.graphwalker.ui.control.LabelComboBox;
import org.graphwalker.ui.control.LabelTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIVertex extends VBox implements UIElement {

  private static final Logger logger = LoggerFactory.getLogger(UIVertex.class);

  private Bounds bounds = new BoundingBox(0, 0, 50, 20);
  private Label labelName;
  private Label sharedStateName;
  private Vertex vertex;
  private DoubleProperty x;
  private DoubleProperty y;
  private UIGraph graph;
  private CheckMenuItem startElementMenu;
  private CheckMenuItem sharedStateMenu;
  private ComboBox<String> sharedStateLabels = new ComboBox<>();

  private int strokeWidthVisited = 1;
  private int strokeWidthNormal = 2;
  private int strokeWidthSelected = 4;
  private int strokeWidthHighLighted = 5;

  protected boolean selected = false;
  protected boolean visited = false;
  protected boolean highLighted = false;


  public UIVertex(UIGraph graph, Vertex vertex) {
    this.graph = graph;
    this.vertex = vertex;
    init();
  }

  public UIVertex(UIGraph graph, String name) {
    this.graph = graph;
    vertex = new Vertex().setName(name);
    init();
  }

  private void init() {
    Platform.runLater(() -> this.toFront());

    setOnMouseClicked(mouseEvent -> {
      logger.debug("UIVertex Clicked");
      if (!graph.getRubberLine().isDisabled()) {
        graph.endCreateEdge(UIVertex.this);
        mouseEvent.consume();
        return;
      }

      if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        if (mouseEvent.isShiftDown()) {
          graph.startCreateEdge((UIVertex) mouseEvent.getSource());
          mouseEvent.consume();
          return;
        }
        logger.debug("Select vertex: " + vertex.getName());
        selected(true);
        mouseEvent.consume();
      }
    });

    labelName = new Label(vertex.getName());
    labelName.setTextAlignment(TextAlignment.CENTER);
    labelName.setAlignment(Pos.CENTER);
    labelName.setOnMouseClicked(mouseEvent -> {
      logger.debug("Label clicked");
      if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        if (mouseEvent.getClickCount() == 2) {
          logger.debug("Vertex label is double clicked");
          getChildren().add(0, new LabelTextField(labelName));
        }
      }
    });
    labelName.textProperty().addListener((observable, oldValue, newValue) -> vertex.setName(newValue));

    getChildren().addAll(new StackPane(labelName));
    getStyleClass().add("node");
    setMinWidth(bounds.getWidth());
    setMinHeight(bounds.getHeight());

    x = new SimpleDoubleProperty();
    x.bind(layoutXProperty().add(bounds.getWidth() / 2));

    y = new SimpleDoubleProperty();
    y.bind(layoutYProperty().add(bounds.getHeight() / 2));

    if (vertex.hasProperty("x") && vertex.hasProperty("y")) {
      relocate((double) vertex.getProperty("x"), (double) vertex.getProperty("y"));
    }

    final ContextMenu contextMenu = new ContextMenu();

    startElementMenu = new CheckMenuItem("Start element");
    startElementMenu.selectedProperty().addListener((observable, oldValue, newValue) -> {
      logger.debug("Current start id changed for: " + labelName.getText() + ", " + newValue);
      if (newValue) {
        setStartElement(true);
        graph.getGraphWalker().setElementStartId(this);
      } else {
        setStartElement(false);
      }
    });

    sharedStateMenu = new CheckMenuItem("Shared state");
    if (isNotNullOrEmpty(getVertex().getSharedState())) {
      sharedStateMenu.setSelected(true);
      createSharedStateCombo();
    }

    sharedStateMenu.selectedProperty().addListener((observable, oldValue, newValue) -> {
      logger.debug("Shared state changed for: " + labelName.getText() + ", " + newValue);
      if (newValue) {
        createSharedStateCombo();
      } else {
        getChildren().remove(sharedStateName);
      }
    });

    contextMenu.getItems().addAll(startElementMenu, sharedStateMenu);

    setOnContextMenuRequested(event -> {
      contextMenu.show(UIVertex.this, event.getScreenX(), event.getScreenY());
      event.consume();
    });
  }

  private void createSharedStateCombo() {
    if (isNotNullOrEmpty(getVertex().getSharedState())) {
      sharedStateName = new Label(getVertex().getSharedState());
    } else {
      sharedStateName = new Label("New Shared State Name");
    }
    sharedStateName.setTextAlignment(TextAlignment.CENTER);
    sharedStateName.setAlignment(Pos.CENTER);
    sharedStateName.setOnMouseClicked(mouseEvent -> {
      logger.debug("Clicked");
      if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        if (mouseEvent.getClickCount() == 2) {
          logger.debug("Vertex label is double clicked");
          getChildren().add(1, new LabelComboBox(sharedStateName,
                                                 graph.getGraphWalker().getListOfSharedStateLabels()));
        }
      }
    });
    sharedStateName.textProperty().addListener((o, oldV, newV) -> vertex.setSharedState(newV));
    getChildren().add(1, sharedStateName);
  }

  @Override
  public void setStartElement(boolean setElement) {
    if (setElement) {
      setStyle("-fx-background-color: lightgreen;");
      startElementMenu.setSelected(true);
    } else {
      setStyle("-fx-background-color: lightblue;");
      if (startElementMenu.isSelected()) {
        startElementMenu.setSelected(false);
      }
    }
  }

  @Override
  public void highlight(boolean highLight) {
    this.highLighted = highLight;
    setElementStyle();
  }

  public void setElementStyle() {
    if (highLighted) {
      setStyle("-fx-border-color: lightblue;" +
               "-fx-border-width: " + strokeWidthSelected + ";");
      labelName.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
      return;
    }

    if (selected) {
      if (startElementMenu.isSelected()) {
        setStyle("-fx-background-color: lightgreen;" +
                 "-fx-border-width: " + strokeWidthSelected + ";");
      } else {
        setStyle("-fx-background-color: lightblue;" +
                 "-fx-border-width: " + strokeWidthSelected + ";");
      }
      labelName.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
      return;
    }

    if (visited) {
      if (startElementMenu.isSelected()) {
        setStyle("-fx-border-color: black;" +
                 "-fx-background-color: #CEF6CE;" +
                 "-fx-border-width: " + strokeWidthVisited + ";");
      } else {
        setStyle("-fx-border-color: black;" +
                 "-fx-background-color: lightgrey;" +
                 "-fx-border-width: " + strokeWidthVisited + ";");
      }
      labelName.setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
      return;
    }

    if (startElementMenu.isSelected()) {
      setStyle("-fx-border-color: black;" +
               "-fx-background-color: lightgreen;" +
               "-fx-border-width: " + strokeWidthNormal + ";");
    } else {
      setStyle("-fx-border-color: black;" +
               "-fx-background-color: lightblue;" +
               "-fx-border-width: " + strokeWidthNormal + ";");
    }
    labelName.setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
  }

  public Vertex getVertex() {
    return vertex;
  }

  public DoubleProperty getX() {
    return x;
  }

  public DoubleProperty getY() {
    return y;
  }

  @Override
  public void selected(boolean selected) {
    this.selected = selected;
    setElementStyle();
  }

  @Override
  public void visited(boolean visited) {
    this.visited = visited;
    setElementStyle();
  }

  @Override
  public boolean isVisited() {
    return visited;
  }

  @Override
  public boolean isSelected() {
    return selected;
  }

  @Override
  public String getElementId() {
    return getVertex().getId();
  }

  @Override
  public String getElementName() {
    return isNotNullOrEmpty(getVertex().getName()) ? getVertex().getName() : "";
  }
}
