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
import javafx.geometry.Point2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.ui.application.GraphWalker;
import org.graphwalker.ui.control.LabelTextField;
import org.graphwalker.ui.misc.ModelDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIEdge extends Pane implements UIElement {

  private static final Logger logger = LoggerFactory.getLogger(UIEdge.class);
  private static final double defaultArrowHeadSize = 10.0;

  private UIVertex source;
  private UIVertex target;

  private CubicCurve curve;
  private ArrowHead arrowHead;
  private Edge edge;
  private Label labelName;
  private Label guardLabel;
  private Label actionLabel;
  private GraphWalker graphWalker;
  private CheckMenuItem startElementMenu;

  private int strokeWidthVisited = 1;
  private int strokeWidthNormal = 2;
  private int strokeWidthSelected = 4;
  private int strokeWidthHighLighted = 5;

  private boolean selected = false;
  private boolean visited = false;
  private boolean highLighted = false;

  public UIEdge(GraphWalker graphWalker, UIVertex source, UIVertex target, Edge edge) {
    this.graphWalker = graphWalker;
    this.source = source;
    this.target = target;
    this.edge = edge;
    init();
  }

  private void init() {
    Platform.runLater(() -> this.toBack());

    setPickOnBounds(false);

    setOnMouseClicked(mouseEvent -> {
      if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        mouseEvent.consume();
        selected(true);
      }
      if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
        if (mouseEvent.getClickCount() == 2) {
          logger.debug("Edge label is double clicked");
          LabelTextField labelTextField = new LabelTextField(labelName);
          labelTextField.layoutXProperty().bind(labelName.layoutXProperty());
          labelTextField.layoutYProperty().bind(labelName.layoutYProperty());
          getChildren().add(0, labelTextField);
        }
      }
    });

    labelName = new Label(edge.getName());
    labelName.textProperty().addListener((observable, oldValue, newValue) -> {
      edge.setName(newValue);
    });

    String guardStr = "";
    if (edge.getGuard() != null && edge.getGuard().getScript() != null) {
      guardStr = edge.getGuard().getScript().toString();
    }
    guardLabel = new Label("Guard: " + guardStr);

    String actionStr = "";
    if (edge.getActions() != null) {
      for (Action action : edge.getActions()) {
        if (action.getScript() != null) {
          actionStr += action.getScript().toString();
        }
      }
    }
    actionLabel = new Label("Action: " + actionStr);

    curve = new CubicCurve();

    curve.setStroke(Color.BLACK);
    curve.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: null;");

    curve.startXProperty().bind(source.getX());
    curve.startYProperty().bind(source.getY());

    curve.endXProperty().bind(target.getX());
    curve.endYProperty().bind(target.getY());

    target.widthProperty().addListener((observable, oldValue, newValue) -> update());
    target.heightProperty().addListener((observable, oldValue, newValue) -> update());

    curve.startXProperty().addListener((observable, oldValue, newValue) -> update());
    curve.startYProperty().addListener((observable, oldValue, newValue) -> update());
    curve.endXProperty().addListener((observable, oldValue, newValue) -> update());
    curve.endYProperty().addListener((observable, oldValue, newValue) -> update());

    curve.controlX1Property().addListener((observable, oldValue, newValue) -> update());
    curve.controlY1Property().addListener((observable, oldValue, newValue) -> update());
    curve.controlX2Property().addListener((observable, oldValue, newValue) -> update());
    curve.controlY2Property().addListener((observable, oldValue, newValue) -> update());

    double[] arrowShape = new double[]{0, 0, 5, 10, -5, 10};
    arrowHead = new ArrowHead(curve, 0.1f, arrowShape);

    getChildren().addAll(curve, arrowHead, labelName, guardLabel, actionLabel);

    final ContextMenu contextMenu = new ContextMenu();

    startElementMenu = new CheckMenuItem("Set as start element");
    startElementMenu.selectedProperty().addListener((observable, oldValue, newValue) -> {
      logger.debug("Current start id changed for: " + labelName.getText() + ", " + newValue);
      if (newValue) {
        setStartElement(true);
        graphWalker.setElementStartId(this);
      } else {
        setStartElement(false);
      }
    });

    MenuItem dataMenu = new MenuItem("Edge properties...");
    dataMenu.setOnAction(e -> {
      if (new ModelDialogs().runEdge(getEdge()).isPresent()) {
        labelName.setText(getEdge().getName());
        guardLabel.setText("Guard: " + getEdge().getGuard().getScript());
        String str = "";
        if (edge.getActions() != null) {
          for (Action action : edge.getActions()) {
            if (action.getScript() != null) {
              str += action.getScript().toString();
            }
          }
        }
        actionLabel.setText("Action: " + str);
        update();
      }
    });

    contextMenu.getItems().addAll(startElementMenu, dataMenu);

    setOnContextMenuRequested(event -> {
      contextMenu.show(UIEdge.this, event.getScreenX(), event.getScreenY());
      event.consume();
    });

    setElementStyle();
  }

  public void update() {
    Point2D pt = arrowHead.eval(curve, .5f);
    labelName.setLayoutX(pt.getX());
    labelName.setLayoutY(pt.getY());

    double posY = pt.getY() + labelName.getHeight();
    if (!guardLabel.getText().equals("Guard: ")) {
      guardLabel.setVisible(true);
      guardLabel.setLayoutX(pt.getX());
      guardLabel.setLayoutY(posY);
      posY += guardLabel.getHeight();
    } else {
      guardLabel.setVisible(false);
    }
    if (!actionLabel.getText().equals("Action: ")) {
      actionLabel.setVisible(true);
      actionLabel.setLayoutX(pt.getX());
      actionLabel.setLayoutY(posY);
    } else {
      actionLabel.setVisible(false);
    }

    arrowHead.t = findT(1);
    arrowHead.update();
  }

  private float findT(float t) {
    Point2D pt = arrowHead.eval(curve, t);
    if (target.getBoundsInParent().contains(pt)) {
      t -= .001;
      if (t > 1e-4) {
        return findT(t);
      }
    }
    return t;
  }

  public UIVertex getSource() {
    return source;
  }

  public UIVertex getTarget() {
    return target;
  }

  public Edge getEdge() {
    return edge;
  }

  public CubicCurve getCurve() {
    return curve;
  }

  public void deselect() {
    selected = false;
    curve.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: null;");
    arrowHead.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: black;");
    labelName.setStyle("-fx-font-weight: normal;");
    guardLabel.setStyle("-fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
    actionLabel.setStyle("-fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
  }

  public boolean isSelected() {
    return selected;
  }

  @Override
  public boolean isVisited() {
    return false;
  }

  public static class ArrowHead extends Polygon {

    public float t;
    CubicCurve curve;
    Rotate rz;

    public ArrowHead(CubicCurve curve, float t, double... arg0) {
      super(arg0);
      this.curve = curve;
      this.t = t;
      init();
    }

    private void init() {
      setFill(Color.BLACK);
      rz = new Rotate();
      rz.setAxis(Rotate.Z_AXIS);
      getTransforms().addAll(rz);
      update();
    }

    public void update() {
      double size = Math.max(curve.getBoundsInLocal().getWidth(), curve.getBoundsInLocal().getHeight());
      double scale = size / 4d;

      Point2D ori = eval(curve, t);
      Point2D tan = evalDt(curve, t).normalize().multiply(scale);

      setTranslateX(ori.getX());
      setTranslateY(ori.getY());

      double angle = Math.atan2(tan.getY(), tan.getX());

      angle = Math.toDegrees(angle);

      // arrow origin is top => apply offset
      double offset = -90;
      if (t > 0.5) {
        offset = +90;
      }

      rz.setAngle(angle + offset);
    }

    /**
     * Evaluate the cubic curve at a parameter 0<=t<=1, returns a Point2D
     *
     * See: https://pomax.github.io/bezierinfo/#projections
     *
     * @param c the CubicCurve
     * @param t param between 0 and 1
     * @return a Point2D
     */
    public Point2D eval(CubicCurve c, float t) {
      Point2D p = new Point2D(Math.pow(1 - t, 3) * c.getStartX() +
                              3 * t * Math.pow(1 - t, 2) * c.getControlX1() +
                              3 * (1 - t) * t * t * c.getControlX2() +
                              Math.pow(t, 3) * c.getEndX(),
                              Math.pow(1 - t, 3) * c.getStartY() +
                              3 * t * Math.pow(1 - t, 2) * c.getControlY1() +
                              3 * (1 - t) * t * t * c.getControlY2() +
                              Math.pow(t, 3) * c.getEndY());
      return p;
    }

    /**
     * Evaluate the tangent of the cubic curve at a parameter 0<=t<=1, returns a Point2D
     *
     * @param c the CubicCurve
     * @param t param between 0 and 1
     * @return a Point2D
     */
    public Point2D evalDt(CubicCurve c, float t) {
      Point2D p = new Point2D(-3 * Math.pow(1 - t, 2) * c.getStartX() +
                              3 * (Math.pow(1 - t, 2) - 2 * t * (1 - t)) * c.getControlX1() +
                              3 * ((1 - t) * 2 * t - t * t) * c.getControlX2() +
                              3 * Math.pow(t, 2) * c.getEndX(),
                              -3 * Math.pow(1 - t, 2) * c.getStartY() +
                              3 * (Math.pow(1 - t, 2) - 2 * t * (1 - t)) * c.getControlY1() +
                              3 * ((1 - t) * 2 * t - t * t) * c.getControlY2() +
                              3 * Math.pow(t, 2) * c.getEndY());
      return p;
    }
  }

  public void setSource(UIVertex source) {
    this.source = source;
  }

  public void setTarget(UIVertex target) {
    this.target = target;
  }

  @Override
  public void setStartElement(boolean setElement) {
    startElementMenu.setSelected(setElement);
    setElementStyle();
  }

  private void setElementStyle() {
    if (highLighted) {
      curve.setStyle("-fx-stroke: lightblue; -fx-stroke-width: " + strokeWidthHighLighted + "; -fx-fill: null;");
      arrowHead.setStyle("-fx-stroke: lightblue; -fx-stroke-width: " + strokeWidthHighLighted + "; -fx-fill: lightblue;");
      labelName.setStyle("-fx-text-fill: lightblue; -fx-font-weight: bold;");
      guardLabel.setStyle("-fx-text-fill: lightblue; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
      actionLabel.setStyle("-fx-text-fill: lightblue; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
      return;
    }

    if (selected) {
      if (startElementMenu.isSelected()) {
        curve.setStyle("-fx-stroke: lightgreen; -fx-stroke-width: " + strokeWidthSelected + "; -fx-fill: null;");
        arrowHead.setStyle("-fx-stroke: lightgreen; -fx-stroke-width: " + strokeWidthSelected + "; -fx-fill: lightgreen;");
        labelName.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        guardLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
        actionLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
      } else {
        curve.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthSelected + "; -fx-fill: null;");
        arrowHead.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthSelected + "; -fx-fill: black;");
        labelName.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        guardLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
        actionLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;");
      }
      return;
    }

    if (visited) {
      if (startElementMenu.isSelected()) {
        curve.setStyle("-fx-stroke: #CEF6CE; -fx-stroke-width: " + strokeWidthVisited + "; -fx-fill: null;");
        arrowHead.setStyle("-fx-stroke: #CEF6CE; -fx-stroke-width: " + strokeWidthVisited + "; -fx-fill: #CEF6CE;");
        labelName.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal;");
        guardLabel.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
        actionLabel.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
      } else {
        curve.setStyle("-fx-stroke: lightgrey; -fx-stroke-width: " + strokeWidthVisited + "; -fx-fill: null;");
        arrowHead.setStyle("-fx-stroke: lightgrey; -fx-stroke-width: " + strokeWidthVisited + "; -fx-fill: lightgrey;");
        labelName.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal;");
        guardLabel.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
        actionLabel.setStyle("-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
      }
      return;
    }

    if (startElementMenu.isSelected()) {
      curve.setStyle("-fx-stroke: lightgreen; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: null;");
      arrowHead.setStyle("-fx-stroke: lightgreen; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: lightgreen;");
      labelName.setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
      guardLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
      actionLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
    } else {
      curve.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: null;");
      arrowHead.setStyle("-fx-stroke: black; -fx-stroke-width: " + strokeWidthNormal + "; -fx-fill: black;");
      labelName.setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
      guardLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
      actionLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;");
    }
  }

  @Override
  public void highlight(boolean highLight) {
    this.highLighted = highLight;
    setElementStyle();
  }

  @Override
  public void visited(boolean visit) {
    this.visited = visit;
    setElementStyle();
  }

  @Override
  public void selected(boolean select) {
    this.selected = select;
    setElementStyle();
  }

  @Override
  public String getElementId() {
    return getEdge().getId();
  }

  @Override
  public String getElementName() {
    return isNotNullOrEmpty(getEdge().getName()) ? getEdge().getName() : "";
  }

  public Label getLabelName() {
    return labelName;
  }
}
