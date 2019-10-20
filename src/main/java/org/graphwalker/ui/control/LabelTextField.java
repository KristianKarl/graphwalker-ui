package org.graphwalker.ui.control;

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

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by krikar on 2017-04-25.
 */
public class LabelTextField extends TextField {

  private static final Logger logger = LoggerFactory.getLogger(LabelTextField.class);

  public LabelTextField(Label labelName) {
    labelName.setVisible(false);
    setText(labelName.getText());

    addEventHandler(KeyEvent.KEY_RELEASED, event -> {
      event.consume();
    });

    addEventFilter(KeyEvent.KEY_RELEASED, keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ENTER) {
        logger.debug("Old text: " + labelName.getText());
        logger.debug("New text: " + getText());
        labelName.setText(getText());
        setFocused(false);
      } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
        logger.debug("Escaped pressed- Keeping old text");
        setFocused(false);
      }
    });

    focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
      if (newPropertyValue) {
        logger.debug("Text field on focus");
      } else {
        System.out.println("Text field out focus");
        labelName.setVisible(true);
        ((Pane) LabelTextField.this.getParent()).getChildren().remove(LabelTextField.this);
      }
    });
    Platform.runLater(() -> this.requestFocus());
  }
}

