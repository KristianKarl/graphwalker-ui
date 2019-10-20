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

import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.junit.Test;

/**
 * Created by krikar on 2017-06-18.
 */
public class GraphWalkerLayoutTest {

  @Test
  public void execute() {
    Vertex v1 = new Vertex();
    Vertex v2 = new Vertex();
    Vertex v3 = new Vertex();
    Edge e1 = new Edge();
    Edge e2 = new Edge();
    Edge e3 = new Edge();
    Edge e4 = new Edge();
    Edge e5 = new Edge();
    Edge e6 = new Edge();
    Model model = new Model().addEdge(e1.setSourceVertex(v1).setTargetVertex(v2))
      .addEdge(e2.setSourceVertex(v1).setTargetVertex(v3))
      .addEdge(e3.setSourceVertex(v1).setTargetVertex(v1))
      .addEdge(e4.setSourceVertex(v2).setTargetVertex(v2))
      .addEdge(e5.setSourceVertex(v3).setTargetVertex(v3))
      .addEdge(e6.setSourceVertex(v3).setTargetVertex(v1));
  }
}
