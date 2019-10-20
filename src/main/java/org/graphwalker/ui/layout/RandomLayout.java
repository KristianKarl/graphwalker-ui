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

import java.util.Random;
import org.graphwalker.ui.model.UIEdge;
import org.graphwalker.ui.model.UIElement;
import org.graphwalker.ui.model.UIGraph;
import org.graphwalker.ui.model.UIVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomLayout extends Layout {

  private static final Logger logger = LoggerFactory.getLogger(RandomLayout.class);

  UIGraph graph;
  Random rnd = new Random();

  public RandomLayout(UIGraph graph) {
    this.graph = graph;
  }

  @Override
  public void execute() {
    logger.debug("Running random layout");
    for (UIElement element : graph.getElements()) {
      if (element instanceof UIVertex) {
        double x = rnd.nextDouble() * graph.getRootPane().getWidth();
        double y = rnd.nextDouble() * graph.getRootPane().getHeight();
        ((UIVertex) element).relocate(x, y);
      }
    }
  }

  @Override
  public void doEdges() {
  }

  @Override
  public void doEdge(UIEdge edge) {

  }
}
