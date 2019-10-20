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

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by krikar on 2017-06-03.
 */
public class LogEntry {

  private static final Logger logger = LoggerFactory.getLogger(LogEntry.class);
  private final SimpleIntegerProperty ordinal;
  private final SimpleStringProperty id;
  private final SimpleStringProperty modelName;
  private final SimpleStringProperty elementName;
  private final SimpleStringProperty elementType;
  private final SimpleStringProperty data;
  private final Node elementNode;

  public LogEntry(Node elementNode, Integer ordinal, String id, String modelName, String elementType, String elementName, String data) {
    this.elementNode = elementNode;
    this.ordinal = new SimpleIntegerProperty(ordinal);
    this.id = new SimpleStringProperty(id);
    this.modelName = new SimpleStringProperty(modelName);
    this.elementName = new SimpleStringProperty(elementName);
    this.elementType = new SimpleStringProperty(elementType);
    this.data = new SimpleStringProperty(data);
  }

  public Integer getOrdinal() {
    return ordinal.get();
  }

  public String getId() {
    return id.get();
  }

  public String getModelName() {
    return modelName.get();
  }

  public String getElementName() {
    return elementName.get();
  }

  public String getElementType() {
    return elementType.get();
  }

  public String getData() {
    return data.get();
  }

  public Node getElementNode() {
    return elementNode;
  }
}
