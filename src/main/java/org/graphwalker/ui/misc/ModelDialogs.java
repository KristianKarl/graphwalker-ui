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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.graphwalker.core.model.Action;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Guard;
import org.graphwalker.dsl.antlr.generator.GeneratorFactory;
import org.graphwalker.ui.model.UIGraph;

/**
 * Created by krikar on 2017-06-05.
 */
public class ModelDialogs {

  public Optional<Object> runEdge(Edge edge) {
    Dialog dialog = new Dialog();
    if (edge.getName() != null) {
      dialog.setTitle("Edge: " + edge.getName());
    } else {
      dialog.setTitle("Edge properties");
    }
    dialog.setResizable(true);

    String guardStr = "";
    if (edge.getGuard() != null && edge.getGuard().getScript() != null) {
      guardStr = edge.getGuard().getScript().toString();
    }

    String actionStr = "";
    if (edge.getActions() != null) {
      for (Action action : edge.getActions()) {
        if (action.getScript() != null) {
          actionStr += action.getScript().toString();
        }
      }
    }

    Label nameLabel = new Label("Name: ");
    Label guardLabel = new Label("Guard: ");
    Label actionLabel = new Label("Action: ");
    TextField nameField = new TextField(edge.getName());
    TextField guardField = new TextField(guardStr);
    TextField actionField = new TextField(actionStr);

    GridPane grid = new GridPane();
    grid.add(nameLabel, 1, 1);
    grid.add(nameField, 2, 1);
    grid.add(guardLabel, 1, 2);
    grid.add(guardField, 2, 2);
    grid.add(actionLabel, 1, 3);
    grid.add(actionField, 2, 3);
    dialog.getDialogPane().setContent(grid);

    ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(buttonTypeCancel, buttonTypeOk);

    dialog.setResultConverter(b -> {
      if (b == buttonTypeOk) {
        edge.setName(nameField.getText());
        edge.setGuard(new Guard(guardField.getText()));
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(actionField.getText()));
        edge.setActions(actions);
        return new Object();
      }
      return null;
    });

    return dialog.showAndWait();
  }

  public Optional<Object> runModel(UIGraph graph) {
    Dialog<Object> dialog = new Dialog<>();
    dialog.setTitle("Model: " + graph.getModel().getName());
    dialog.setResizable(true);

    String actionStr = "";
    if (graph.getModel().getActions() != null) {
      for (Action action : graph.getModel().getActions()) {
        if (action.getScript() != null) {
          actionStr += action.getScript().toString();
        }
      }
    }

    Label nameLabel = new Label("Name: ");
    Label generatorLabel = new Label("Generator: ");
    Label actionLabel = new Label("Action: ");
    TextField nameField = new TextField(graph.getModel().getName());
    TextField generatorField = new TextField(graph.getGenerator());
    TextField actionField = new TextField(actionStr);

    GridPane grid = new GridPane();
    grid.add(nameLabel, 1, 1);
    grid.add(nameField, 2, 1);
    grid.add(generatorLabel, 1, 2);
    grid.add(generatorField, 2, 2);
    grid.add(actionLabel, 1, 3);
    grid.add(actionField, 2, 3);
    dialog.getDialogPane().setContent(grid);

    ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(buttonTypeCancel, buttonTypeOk);

    final Button okButton = (Button) dialog.getDialogPane().lookupButton(buttonTypeOk);
    //okButton.setDisable(true);
    generatorField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (isValidGenerator(newValue.trim())) {
        okButton.setDisable(false);
        generatorField.setStyle("-fx-text-fill: black;");
      } else {
        okButton.setDisable(true);
        generatorField.setStyle("-fx-text-fill: red;");
      }
    });

    dialog.setResultConverter(b -> {
      if (b == buttonTypeOk) {
        graph.getModel().setName(nameField.getText());
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(actionField.getText()));
        graph.getModel().setActions(actions);
        graph.setGenerator(generatorField.getText());
        return new Object();
      }
      return null;
    });

    return dialog.showAndWait();
  }

  private boolean isValidGenerator(String generator) {
    try {
      GeneratorFactory.parse(generator);
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
