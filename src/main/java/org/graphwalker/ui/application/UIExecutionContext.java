package org.graphwalker.ui.application;

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

import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.graphwalker.core.generator.PathGenerator;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.core.model.Model;

/**
 * Created by krikar on 2017-07-09.
 */
public final class UIExecutionContext extends ExecutionContext {

  private static final Bindings bindings = new SimpleBindings();

  public UIExecutionContext() {
    super();
    getScriptEngine().put("global", bindings);
  }

  public UIExecutionContext(Model model, PathGenerator generator) {
    super(model, generator);
    getScriptEngine().put("global", bindings);
  }

  public UIExecutionContext(Model.RuntimeModel model, PathGenerator generator) {
    super(model, generator);
    getScriptEngine().put("global", bindings);
  }
}
