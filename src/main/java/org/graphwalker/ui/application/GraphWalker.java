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

import static org.graphwalker.core.common.Objects.isNotNullOrEmpty;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.script.ScriptEngine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.graphwalker.core.event.EventType;
import org.graphwalker.core.event.Observer;
import org.graphwalker.core.machine.Context;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.core.machine.Machine;
import org.graphwalker.core.machine.SimpleMachine;
import org.graphwalker.core.model.Edge;
import org.graphwalker.core.model.Element;
import org.graphwalker.core.model.Vertex;
import org.graphwalker.dsl.antlr.DslException;
import org.graphwalker.dsl.antlr.generator.GeneratorFactory;
import org.graphwalker.io.factory.ContextFactory;
import org.graphwalker.io.factory.ContextFactoryException;
import org.graphwalker.io.factory.ContextFactoryScanner;
import org.graphwalker.io.factory.dot.DotContextFactory;
import org.graphwalker.io.factory.json.JsonContextFactory;
import org.graphwalker.io.factory.json.JsonModel;
import org.graphwalker.io.factory.yed.YEdContextFactory;
import org.graphwalker.ui.Options;
import org.graphwalker.ui.layout.GraphWalkerLayout;
import org.graphwalker.ui.misc.LogEntry;
import org.graphwalker.ui.misc.UnsupportedFileFormat;
import org.graphwalker.ui.misc.ZoomableScrollPane;
import org.graphwalker.ui.model.UIEdge;
import org.graphwalker.ui.model.UIElement;
import org.graphwalker.ui.model.UIGraph;
import org.graphwalker.ui.model.UIStartVertex;
import org.graphwalker.ui.model.UIVertex;
import org.graphwalker.ui.util.LoggerUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphWalker extends Application implements Observer {

  private static final Logger logger = LoggerFactory.getLogger(GraphWalker.class);

  final ObjectProperty<Cursor> CURSOR_DEFAULT = new SimpleObjectProperty<>(Cursor.DEFAULT);
  final ObjectProperty<Cursor> CURSOR_WAIT = new SimpleObjectProperty<>(Cursor.WAIT);
  private final ExecutorService pool = Executors.newFixedThreadPool(4);

  private static List<String> modelFiles = new ArrayList<>();
  VBox topVBox = new VBox();
  String currentDir = null;
  File currentFile = null;
  Scene scene = null;
  Stage primaryStage = null;
  TabPane tabPane;
  Integer ordinal = null;
  TableView<LogEntry> executionWindow = null;
  TextFlow messagesWindow = null;
  TabPane outputTabPane = null;
  Future executingGraphTask = null;
  Object executionLock = new Object();
  Timer executionSpeedTimer = null;
  SimpleDoubleProperty executionDelay;
  Button playPause;
  Button stepFwd;
  Button stop;
  Slider slider;
  SimpleDoubleProperty speed;

  private List<UIGraph> graphs = new ArrayList<>();
  private Set<String> listOfSharedStateLabels = new HashSet<>();
  private int currentGraphIndex;
  private String startElementId;
  private boolean isSliderValueChanged = false;

  enum GraphWalkerRunningState {
    stopped,
    paused,
    running
  }

  GraphWalkerRunningState graphWalkerRunningState = GraphWalkerRunningState.stopped;

  private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

  @Override
  public void start(Stage primaryStage) throws IOException {
    this.primaryStage = primaryStage;

    BorderPane root = new BorderPane();
    scene = new Scene(root, 600, 500);
    scene.getStylesheets().add(GraphWalker.class.getResource("/graphwalker.css").toExternalForm());
    scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
      if (keyEvent.getCode() == KeyCode.L) {
        logger.debug("Key L is pressed");
        new GraphWalkerLayout(getCurrentGraph()).execute();
        getCurrentGraph().fitGraphInWindow();
      } else if (keyEvent.getCode() == KeyCode.E) {
        logger.debug("Key E is pressed");
        new GraphWalkerLayout(getCurrentGraph()).doEdges();
      } else if (keyEvent.getCode() == KeyCode.DELETE) {
        logger.debug("Key DELETE is pressed");

        Set<UIElement> elementsToBeRemoved = new HashSet<>();
        for (UIElement vertex : getCurrentGraph().getElements()) {
          if (vertex.isSelected() && vertex instanceof UIVertex) {
            for (UIElement edge : getCurrentGraph().getElements()) {
              if (edge instanceof UIEdge &&
                  ((UIEdge) edge).getSource().getVertex().getId().equals(vertex.getElementId())) {
                elementsToBeRemoved.add(edge);
              }
              if (edge instanceof UIEdge &&
                  ((UIEdge) edge).getTarget().getVertex().getId().equals(vertex.getElementId())) {
                elementsToBeRemoved.add(edge);
              }
            }
            elementsToBeRemoved.add(vertex);
            getCurrentGraph().getModel().deleteVertex(((UIVertex) vertex).getVertex());
          }
        }

        for (UIElement edge : getCurrentGraph().getElements()) {
          if (edge.isSelected() && edge instanceof UIEdge) {
            elementsToBeRemoved.add(edge);
            getCurrentGraph().getModel().deleteEdge(((UIEdge) edge).getEdge());
          }
        }

        getCurrentGraph().getElements().removeAll(elementsToBeRemoved);
        getCurrentGraph().getContentPane().getChildren().removeAll(elementsToBeRemoved);
      }
    });

    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(t -> {
      Platform.exit();
      System.exit(0);
    });
    primaryStage.show();

    topVBox.getChildren().add(addMenus());
    topVBox.getChildren().add(addToolbar());

    tabPane = new TabPane();
    tabPane.getSelectionModel().selectedItemProperty().addListener(
      (ov, oldTab, newTab) -> {
        currentGraphIndex = tabPane.getSelectionModel().getSelectedIndex();
        if (currentGraphIndex < 0) {
          return;
        }
        executionDelay.bind(slider.valueProperty());
      }
    );

    executionWindow = new TableView<LogEntry>();
    executionWindow.setItems(logEntries);
    executionWindow.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
      if (oldSelection != null) {
        if (oldSelection.getElementNode() instanceof UIElement) {
          ((UIElement) oldSelection.getElementNode()).highlight(false);
        }
      }
      if (newSelection != null) {
        highLightElement(newSelection.getId());
      }
    });

    ordinal = new Integer(0);
    TableColumn ordinalCol = new TableColumn("#");
    ordinalCol.setCellValueFactory(new PropertyValueFactory<LogEntry, Integer>("ordinal"));
    ordinalCol.setSortable(false);
    ordinalCol.setPrefWidth(40);

    TableColumn modelNameCol = new TableColumn("Model Name");
    modelNameCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("modelName"));
    modelNameCol.setSortable(false);
    modelNameCol.setPrefWidth(200);

    TableColumn elementNameCol = new TableColumn("Element Name");
    elementNameCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("elementName"));
    elementNameCol.setSortable(false);
    elementNameCol.setPrefWidth(200);

    TableColumn dataCol = new TableColumn("Data");
    dataCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("data"));
    dataCol.setSortable(false);
    dataCol.setPrefWidth(200);

    executionWindow.getColumns().addAll(ordinalCol, modelNameCol, elementNameCol, dataCol);

    messagesWindow = new TextFlow();

    Tab executionOutput = new Tab("Execution output");
    executionOutput.setClosable(false);
    executionOutput.setContent(executionWindow);

    Tab messagesOutput = new Tab("Messages");
    messagesOutput.setClosable(false);
    messagesOutput.setContent(messagesWindow);

    outputTabPane = new TabPane();
    outputTabPane.getTabs().addAll(executionOutput, messagesOutput);

    SplitPane splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.getItems().addAll(tabPane, outputTabPane);

    root.setTop(topVBox);
    root.setCenter(splitPane);

    executionDelay = new SimpleDoubleProperty();
    executionDelay.addListener((observable, oldValue, newValue) -> {
      if (graphWalkerRunningState == GraphWalkerRunningState.running) {
        logger.debug("New delay value: " + newValue.longValue());
        isSliderValueChanged = true;
      }
    });
    executionDelay.bind(slider.valueProperty());

    if (modelFiles != null) {
      for (String modelFile : modelFiles) {
        File file = new File(modelFile);
        loadFile(file);
        currentFile = file;
      }
    }
  }

  private Tab createTab(UIGraph graph) {
    logger.debug("Creating graph: " + graph.getModel().getName());
    Tab tab = new Tab();
    setTabLabel(graph, tab);

    tab.setOnClosed((Event t) -> {
      int removeGraphIndex = graphs.indexOf(graph);
      graphs.remove(graph);
      currentGraphIndex = removeGraphIndex - 1;
    });

    tab.setContent(graph.getRootPane());
    tabPane.getTabs().add(tab);
    return tab;
  }

  public void setTabLabel(UIGraph graph, Tab tab) {
    Label label = new Label(graph.getModel().getName());
    tab.setGraphic(label);

    TextField textField = new TextField();
    label.setOnMouseClicked(event -> {
      logger.debug("Clicked");
      if (event.getClickCount() == 2) {
        textField.setText(label.getText());
        tab.setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
      }
    });

    textField.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
      event.consume();
    });

    textField.setOnAction(event -> {
      label.setText(textField.getText());
      tab.setGraphic(label);
    });

    textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        label.setText(textField.getText());
        graph.getModel().setName(textField.getText());
        tab.setGraphic(label);
      }
    });
  }

  private ToolBar addToolbar() {
    ToolBar toolBar = new ToolBar();
    Button layout = new Button();
    Button fitLayout = new Button();
    playPause = new Button();
    stepFwd = new Button();
    stop = new Button();
    slider = new Slider();

    layout.setId("layout");
    fitLayout.setId("fitLayout");
    playPause.setId("playPause");
    stepFwd.setId("stepFwd");
    stop.setId("stop");

    layout.setGraphic(new ImageView("/img/layout.png"));
    fitLayout.setGraphic(new ImageView("/img/fit.png"));
    playPause.setGraphic(new ImageView("/img/play.png"));
    stepFwd.setGraphic(new ImageView("/img/stepFwd.png"));
    stop.setGraphic(new ImageView("/img/stop.png"));
    stop.setDisable(true);

    layout.setOnAction(event -> {
      if (currentGraphIndex >= 0) {
        new GraphWalkerLayout(getCurrentGraph()).execute();
        getCurrentGraph().fitGraphInWindow();
      }
    });

    fitLayout.setOnAction(event -> {
      if (currentGraphIndex < 0) {
        return;
      }
      getCurrentGraph().fitGraphInWindow();
    });

    playPause.setOnAction(event -> {
      if (currentGraphIndex < 0) {
        return;
      }
      switch (graphWalkerRunningState) {
        case stopped:
          resettingGraphs();
          stop.setDisable(false);
          stepFwd.setDisable(true);
          playPause.setGraphic(new ImageView("/img/pause.png"));
          startExecutionTimerContinuously((long) slider.getValue());
          runGraphWalker();
          graphWalkerRunningState = GraphWalkerRunningState.running;
          break;

        case paused:
          stop.setDisable(false);
          stepFwd.setDisable(true);
          playPause.setGraphic(new ImageView("/img/pause.png"));
          startExecutionTimerContinuously((long) slider.getValue());
          graphWalkerRunningState = GraphWalkerRunningState.running;
          break;

        case running:
          stopExecutionTimer();
          stop.setDisable(false);
          stepFwd.setDisable(false);
          playPause.setGraphic(new ImageView("/img/play.png"));
          graphWalkerRunningState = GraphWalkerRunningState.paused;
          break;

        default:
          break;
      }
    });

    stepFwd.setOnAction(event -> {
      graphWalkerRunningState = GraphWalkerRunningState.paused;
      if (currentGraphIndex < 0) {
        return;
      }
      switch (graphWalkerRunningState) {
        case stopped:
          resettingGraphs();
          stop.setDisable(false);
          stepFwd.setDisable(false);
          playPause.setGraphic(new ImageView("/img/play.png"));
          startExecutionTimerOnce();
          runGraphWalker();
          break;

        case paused:
          stop.setDisable(false);
          stepFwd.setDisable(false);
          playPause.setGraphic(new ImageView("/img/pause.png"));
          startExecutionTimerOnce();
          break;

        case running:
          break;

        default:
          break;
      }
    });

    stop.setOnAction(event -> {
      graphWalkerRunningState = GraphWalkerRunningState.stopped;
      if (currentGraphIndex < 0) {
        return;
      }
      switch (graphWalkerRunningState) {
        case stopped:
        case paused:
        case running:
          stopExecutionTimer();
          stepFwd.setDisable(false);
          stop.setDisable(true);
          executingGraphTask.cancel(true);
          playPause.setGraphic(new ImageView("/img/play.png"));
          resettingGraphs();
          break;

        default:
          break;
      }
    });

    slider.setMin(0);
    slider.setMax(1000);
    slider.setValue(500);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(200);
    slider.setMinorTickCount(100);
    slider.setBlockIncrement(100);
    slider.setTooltip(new Tooltip("Execution step delay in ms"));

    speed = new SimpleDoubleProperty();
    speed.bind(slider.valueProperty());

    Separator separator = new Separator();
    separator.setOrientation(Orientation.VERTICAL);

    toolBar.getItems().addAll(layout, fitLayout, separator, playPause, stepFwd, stop, slider);
    return toolBar;
  }

  private void runGraphWalker() {
    executionWindow.getItems().clear();
    messagesWindow.getChildren().clear();
    if (currentGraphIndex < 0) {
      return;
    }
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        logger.debug("Running the graph!");

        List<Context> contexts = new ArrayList<>();
        for (UIGraph graph : getGraphs()) {
          graph.getModel().getEdges().clear();
          graph.getModel().getVertices().clear();
          for (UIElement element : graph.getElements()) {
            if (element instanceof UIVertex) {
              graph.getModel().addVertex(((UIVertex) element).getVertex());
            }
            if (element instanceof UIEdge) {
              graph.getModel().addEdge(((UIEdge) element).getEdge());
            }
          }
          ExecutionContext context = new UIExecutionContext(graph.getModel().build(),
                                                            GeneratorFactory.parse(graph.getGenerator()));
          Element startElement = getElement(graph, startElementId);
          if (startElement != null) {
            context.setNextElement(startElement);
          }
          contexts.add(context);
        }

        Machine machine = new SimpleMachine(contexts);
        machine.addObserver(GraphWalker.this);
        while (machine.hasNextStep()) {
          machine.getNextStep();
        }
        return null;
      }
    };
    task.setOnFailed(handle -> {
      Throwable throwable = task.getException();
      addMessage(throwable.getMessage());
      throwable.printStackTrace();
      stopExecutionTimer();
      graphWalkerRunningState = GraphWalkerRunningState.stopped;
      stepFwd.setDisable(false);
      playPause.setGraphic(new ImageView("/img/play.png"));
    });
    task.setOnSucceeded(handle -> {
      logger.debug("Done running the graph");
      stopExecutionTimer();
      graphWalkerRunningState = GraphWalkerRunningState.stopped;
      stepFwd.setDisable(false);
      playPause.setGraphic(new ImageView("/img/play.png"));
    });
    executingGraphTask = pool.submit(task);
  }

  private void stopExecutionTimer() {
    logger.debug("Stopping execution timer");
    if (executionSpeedTimer != null) {
      executionSpeedTimer.cancel();
      executionSpeedTimer = null;
    }
  }

  private void startExecutionTimerContinuously(long delay) {
    if (delay < 1) {
      delay = 1;
    }
    logger.debug("Start continuously running execution timer");
    executionSpeedTimer = new Timer();
    executionSpeedTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        synchronized (executionLock) {
          executionLock.notify();
        }
      }
    }, 0, delay);
  }

  private void startExecutionTimerOnce() {
    executionSpeedTimer = new Timer();
    executionSpeedTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        synchronized (executionLock) {
          logger.debug(Thread.currentThread().getName() + " notifier runGuardAndAction");
          executionLock.notify();
        }
      }
    }, 0);
  }

  private Element getElement(UIGraph graph, String elementId) {
    for (Vertex vertex : graph.getModel().getVertices()) {
      if (vertex.getId() != null && vertex.getId().equals(elementId)) {
        return vertex.build();
      }
    }
    for (Edge edge : graph.getModel().getEdges()) {
      if (edge.getId().equals(elementId)) {
        return edge.build();
      }
    }
    return null;
  }

  private void resettingGraphs() {
    ordinal = 0;
    executionWindow.getItems().clear();
    messagesWindow.getChildren().clear();
    for (UIGraph graph : getGraphs()) {
      for (UIElement element : graph.getElements()) {
        if (element instanceof UIVertex) {
          if (isNotNullOrEmpty(((UIVertex) element).getVertex().getName()) &&
              !((UIVertex) element).getVertex().getName().equals("Start")) {
            element.selected(false);
            element.visited(false);
            element.highlight(false);
            if (isNotNullOrEmpty(startElementId) && startElementId.equals(element.getElementId())) {
              element.setStartElement(true);
            }
          }
        }
      }
      for (UIElement element : graph.getElements()) {
        if (element instanceof UIEdge) {
          element.selected(false);
          element.visited(false);
          element.highlight(false);
          if (isNotNullOrEmpty(startElementId) && startElementId.equals(element.getElementId())) {
            element.setStartElement(true);
          }
        }
      }
    }
  }

  private MenuBar addMenus() {
    MenuBar menuBar = new MenuBar();
    menuBar.getMenus().addAll(addFileMenu(menuBar), addModelMenu(menuBar));
    return menuBar;
  }

  private Menu addModelMenu(MenuBar menuBar) {
    Menu menu = new Menu("Model");
    menu.setId("modelMenu");

    MenuItem addModelItem = new MenuItem("Add model");
    addModelItem.setId("addModel");

    addModelItem.setOnAction((ActionEvent Event) -> {
      UIGraph graph = new UIGraph(this);
      graph.getModel().setName("New Model");
      graphs.add(graph);
      createTab(graph);
      currentGraphIndex = graphs.indexOf(graph);
      tabPane.getSelectionModel().select(currentGraphIndex);
    });

    menu.getItems().addAll(addModelItem);

    return menu;
  }

  private Menu addFileMenu(MenuBar menuBar) {
    Menu menu = new Menu("File");
    menu.setId("fileMenu");

    MenuItem newItem = new MenuItem("New");
    newItem.setId("fileNew");

    MenuItem openItem = new MenuItem("Open...");
    openItem.setId("fileOpen");

    MenuItem saveItem = new MenuItem("Save");
    saveItem.setId("saveOpen");

    MenuItem saveAsItem = new MenuItem("Save as...");
    saveAsItem.setId("saveAsOpen");

    MenuItem exitItem = new MenuItem("Exit");
    exitItem.setId("fileExit");

    menu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, exitItem);

    newItem.setOnAction((ActionEvent Event) -> {
      clearAll();
    });

    openItem.setOnAction((ActionEvent Event) -> {
      if (currentDir == null) {
        currentDir = System.getProperty("user.dir") + File.separator;
      }
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Open from file");
      fileChooser.setInitialDirectory(new File(currentDir));
      fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Graphwalker Files", "*.gw3", "*.json"),
        new FileChooser.ExtensionFilter("Graphml Files", "*.graphml"),
        new FileChooser.ExtensionFilter("Grapviz Dot Files", "*.dot"),
        new FileChooser.ExtensionFilter("All Files", "*.*"));
      List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
      if (selectedFiles != null && selectedFiles.size() > 0) {
        currentDir = selectedFiles.get(0).getParent();
        logger.debug("Number of files to open: " + selectedFiles.size());

        for (File file : selectedFiles) {
          loadFile(file);
          currentFile = file;
        }
      }
    });

    saveItem.setOnAction((ActionEvent Event) -> {
      if (currentFile != null && currentFile.canWrite()) {
        saveFile(currentFile);
      } else {
        askForFileToSave();
      }
    });

    saveAsItem.setOnAction((ActionEvent Event) -> {
      askForFileToSave();
    });

    exitItem.setOnAction(Event -> System.exit(0));

    return menu;
  }

  private void askForFileToSave() {
    if (currentDir == null) {
      currentDir = System.getProperty("user.dir") + File.separator;
    }
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save to file");
    fileChooser.setInitialDirectory(new File(currentDir));
    currentFile = fileChooser.showSaveDialog(primaryStage);
    if (currentFile != null) {
      currentDir = currentFile.getParent();
      saveFile(currentFile);
    }
  }

  private void loadFile(File modelFile) {
    scene.setCursor(Cursor.WAIT);
    List<UIGraph> graphsLoadedByFile = new ArrayList<>();

    Task<Void> task = new Task<Void>() {

      @Override
      protected Void call() throws Exception {
        logger.debug("Open from file: " + modelFile.getPath());
        ContextFactory factory;

        try {
          /**
           * Faster load times than using the FactoryScanner
           */
          if (new YEdContextFactory().accept(modelFile.toPath())) {
            factory = new YEdContextFactory();
          } else if (new JsonContextFactory().accept(modelFile.toPath())) {
            factory = new JsonContextFactory();
          } else if (new DotContextFactory().accept(modelFile.toPath())) {
            factory = new DotContextFactory();
          } else {
            throw new UnsupportedFileFormat(modelFile.getPath());
          }

          List<Context> localContexts = factory.create(modelFile.toPath());
          for (Context context : localContexts) {
            JsonModel jsonModel = new JsonModel();
            jsonModel.setModel(context.getModel());
            UIGraph graph = new UIGraph(GraphWalker.this);
            graph.setModel(jsonModel.getModel());
            if (context.getPathGenerator() != null) {
              Platform.runLater(() -> graph.setGenerator(context.getPathGenerator().toString()));
            }
            graphs.add(graph);
            graphsLoadedByFile.add(graph);
            if (context.getNextElement() != null) {
              setElementStartId(context.getNextElement().getId());
            }

          }
        } catch (DslException e) {
          addMessage(e.getMessage());
          e.printStackTrace();
        } catch (IOException e) {
          addMessage(e.getMessage());
          e.printStackTrace();
        } catch (UnsupportedFileFormat unsupportedFileFormat) {
          addMessage(unsupportedFileFormat.getMessage());
          unsupportedFileFormat.printStackTrace();
        } catch (ContextFactoryException e) {
          addMessage(e.getMessage());
          e.printStackTrace();
        }
        return null;
      }
    };

    task.setOnFailed(handle -> {
      scene.setCursor(Cursor.DEFAULT);
      Throwable throwable = task.getException();
      addMessage(throwable.getMessage());
      throwable.printStackTrace();
    });

    task.setOnSucceeded(event -> {
      for (UIGraph graph : graphsLoadedByFile) {
        createTab(graph);
        addGraphComponents(graph);
        new GraphWalkerLayout(graph).doEdges();
        graph.fitGraphInWindow();
        graph.getScrollPane().viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
          @Override
          public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
            logger.debug("New bound: " + newValue);
            graph.fitGraphInWindow();
            Platform.runLater(() -> graph.getScrollPane().requestLayout());
            graph.getScrollPane().viewportBoundsProperty().removeListener(this);
          }
        });
      }
      graphsLoadedByFile.clear();
      scene.setCursor(Cursor.DEFAULT);
    });

    Future<?> future = pool.submit(task);
    try {
      // Wait until loading of file is done.
      future.get();
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
    } catch (ExecutionException e) {
      logger.error(e.getMessage());
    }
  }

  private void addMessage(String message) {
    Text text = new Text(message);
    messagesWindow.getChildren().add(text);
    outputTabPane.getSelectionModel().select(1);
  }

  private void clearAll() {
    startElementId = null;
    tabPane.getTabs().clear();
    getGraphs().clear();
    listOfSharedStateLabels.clear();
    executionWindow.getItems().clear();
    messagesWindow.getChildren().clear();
  }

  private void saveFile(File modelFile) {
    scene.setCursor(Cursor.WAIT);
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        logger.debug("Save to file: " + modelFile.getPath());

        List<Context> contexts = new ArrayList<>();
        for (UIGraph graph : getGraphs()) {
          ExecutionContext context = new ExecutionContext() {
            @Override
            public ScriptEngine getScriptEngine() {
              return super.getScriptEngine();
            }
          };
          graph.getModel().getEdges().clear();
          graph.getModel().getVertices().clear();
          for (UIElement element : graph.getElements()) {
            if (element instanceof UIVertex) {
              graph.getModel().addVertex(((UIVertex) element).getVertex());
            } else if (element instanceof UIEdge) {
              graph.getModel().addEdge(((UIEdge) element).getEdge());
            }
          }

          context.setModel(graph.getModel().build());
          context.setPathGenerator(GeneratorFactory.parse(graph.getGenerator()));
          Element startElement = getElement(graph, startElementId);
          if (startElement != null) {
            context.setNextElement(startElement);
          }
          contexts.add(context);
        }
        ContextFactory outputFactory = ContextFactoryScanner.get(modelFile.toPath());

        try (PrintWriter out = new PrintWriter(modelFile)) {
          out.println(outputFactory.getAsString(contexts));
        }

        return null;
      }
    };

    task.setOnFailed(handle -> {
      scene.setCursor(Cursor.DEFAULT);
      Throwable throwable = task.getException();
      addMessage(throwable.getMessage());
      throwable.printStackTrace();
    });
    task.setOnSucceeded(event -> {
      scene.setCursor(Cursor.DEFAULT);
      logger.debug("Saving file to: " + currentFile.getPath() + ", successful");
    });
    pool.submit(task);
  }

  public void addGraphComponents(UIGraph graph) {
    logger.debug("Adding graphical components");

    double minX = 0f;
    double minY = 0f;

    for (Vertex vertex : graph.getModel().getVertices()) {
      UIVertex uiVertex = new UIVertex(graph, vertex);

      if (isNotNullOrEmpty(vertex.getSharedState())) {
        listOfSharedStateLabels.add(vertex.getSharedState());
      }

      graph.addVertex(uiVertex);
      if (isNotNullOrEmpty(startElementId) && startElementId.equals(vertex.getId())) {
        uiVertex.setStartElement(true);
      }

      if (uiVertex.getX().getValue() < minX) {
        minX = uiVertex.getX().getValue();
      }
      if (uiVertex.getY().getValue() < minY) {
        minY = uiVertex.getY().getValue();
      }
    }

    for (UIElement element : graph.getElements()) {
      if (element instanceof UIVertex) {
        ((UIVertex) element).relocate(((UIVertex) element).getX().getValue() - minX + 5e5,
                                      ((UIVertex) element).getY().getValue() - minY + 5e5);
      }
    }

    for (Edge edge : graph.getModel().getEdges()) {
      UIVertex sourceVertex;
      UIVertex targetVertex = findVertex(graph, edge.getTargetVertex());
      if (edge.getSourceVertex() == null) {
        sourceVertex = new UIStartVertex(graph);
        sourceVertex.relocate(targetVertex.getX().getValue(), targetVertex.getX().getValue() - 200);
        graph.addVertex(sourceVertex);
      } else {
        sourceVertex = findVertex(graph, edge.getSourceVertex());
      }
      UIEdge uiEdge = new UIEdge(this,
                                 sourceVertex,
                                 targetVertex,
                                 edge);
      if (isNotNullOrEmpty(startElementId) && startElementId.equals(edge.getId())) {
        uiEdge.setStartElement(true);
      }
      graph.addEdge(uiEdge);
    }
  }

  private UIVertex findVertex(UIGraph graph, Vertex sourceVertex) {
    for (UIElement element : graph.getElements()) {
      if (element instanceof UIVertex) {
        if (sourceVertex.getId().equals(element.getElementId())) {
          return (UIVertex) element;
        }
      }
    }
    logger.warn("Did not find vertex for: " + sourceVertex);
    return null;
  }

  public static void main(String[] args) {
    Options options = new Options();
    JCommander jc = new JCommander(options);
    jc.setProgramName("java -jar graphwalker.jar");
    try {
      jc.parseWithoutValidation(args);
    } catch (Exception e) {
      // ignore
    }

    try {
      setLogLevel(options);

      if (options.help) {
        options = new Options();
        jc = new JCommander(options);
        jc.parse(args);
        jc.usage();
        System.exit(0);
      } else if (options.version) {
        System.out.println(printVersionInformation());
        System.exit(0);
      } else if (!options.modelFiles.isEmpty()) {
        modelFiles = options.modelFiles;
      }

      // Need to instantiate options again to avoid
      // ParameterException "Can only specify option --debug once."
      options = new Options();
      jc = new JCommander(options);
      jc.parse(args);
      launch(args);

    } catch (MissingCommandException e) {
      System.err.println(e.getMessage() + System.lineSeparator());
    } catch (ParameterException e) {
      System.err.println("An error occurred when running command: " + StringUtils.join(args, " "));
      System.err.println(e.getMessage() + System.lineSeparator());
      if (jc.getParsedCommand() != null) {
        jc.usage(jc.getParsedCommand());
      }
    } catch (Exception e) {
      System.err.println("An error occurred when running command: " + StringUtils.join(args, " "));
      System.err.println(e.getMessage() + System.lineSeparator());
      logger.error("An error occurred when running command: " + StringUtils.join(args, " "), e);
    }
  }

  private static void setLogLevel(Options options) {
    // OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    if (options.debug.equalsIgnoreCase("OFF")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.OFF);
    } else if (options.debug.equalsIgnoreCase("ERROR")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.ERROR);
    } else if (options.debug.equalsIgnoreCase("WARN")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.WARN);
    } else if (options.debug.equalsIgnoreCase("INFO")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.INFO);
    } else if (options.debug.equalsIgnoreCase("DEBUG")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.DEBUG);
    } else if (options.debug.equalsIgnoreCase("TRACE")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.TRACE);
    } else if (options.debug.equalsIgnoreCase("ALL")) {
      LoggerUtil.setLogLevel(LoggerUtil.Level.ALL);
    } else {
      throw new ParameterException("Incorrect argument to --debug");
    }
  }

  private static String printVersionInformation() {
    String version = "org.graphwalker version: " + getVersionString() + System.getProperty("line.separator");
    version += System.getProperty("line.separator");

    version += "org.graphwalker is open source software licensed under MIT license" + System.getProperty("line.separator");
    version += "The software (and it's source) can be downloaded from http://graphwalker.org" + System.getProperty("line.separator");
    version +=
      "For a complete list of this package software dependencies, see http://graphwalker.org/archive/site/graphwalker-cli/dependencies.html" + System
        .getProperty("line.separator");

    return version;
  }

  private static String getVersionString() {
    Properties properties = new Properties();
    InputStream inputStream = GraphWalker.class.getResourceAsStream("/version.properties");
    if (null != inputStream) {
      try {
        properties.load(inputStream);
      } catch (IOException e) {
        logger.error("An error occurred when trying to get the version string", e);
        return "unknown";
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }
    return properties.getProperty("graphwalker.version");
  }

  public void highLightElement(String id) {
    logger.debug("Higlight: " + id);
    for (UIGraph graph : getGraphs()) {
      for (UIElement element : graph.getElements()) {
        if (id.equals(element.getElementId())) {
          currentGraphIndex = graphs.indexOf(graph);
          executionDelay.bind(slider.valueProperty());
          tabPane.getSelectionModel().select(currentGraphIndex);
          element.highlight(true);
          centerNodeInScrollPane(graph.getScrollPane(), (Node) element);
          return;
        }
      }
    }
  }

  /**
   * This method is called by a worker thread. So calls to the UI thread needs to be wrapped
   * in Platform.runLater calls
   */
  @Override
  public void update(Machine machine, Element element, EventType type) {
    logger.debug("Received an update from a GraphWalker machine");
    logger.debug("  Current model: " + machine.getCurrentContext().getModel().getName());
    logger.debug("   Element name: " + (isNotNullOrEmpty(element.getName()) ? element.getName() : ""));
    logger.debug("     Event type: " + type.name());
    if (graphWalkerRunningState == GraphWalkerRunningState.stopped) {
      return;
    }
    if (type == EventType.BEFORE_ELEMENT) {
      for (UIGraph graph : getGraphs()) {
        for (UIElement uiElement : graph.getElements()) {
          if (element.getId().equals(uiElement.getElementId())) {
            currentGraphIndex = graphs.indexOf(graph);
            executionDelay.bind(slider.valueProperty());
            tabPane.getSelectionModel().select(currentGraphIndex);

            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> key : machine.getCurrentContext().getKeys().entrySet()) {
              jsonObject.put(key.getKey(), key.getValue());
            }

            logEntries.add(new LogEntry((Node) uiElement,
                                        ++ordinal,
                                        uiElement.getElementId(),
                                        graph.getModel().getName(),
                                        "Vertex",
                                        uiElement.getElementName(),
                                        jsonObject.toString()));
            Platform.runLater(() -> {
              uiElement.highlight(true);
              executionWindow.scrollTo(logEntries.size() - 1);
              outputTabPane.getSelectionModel().select(0);
            });
          }
        }
      }
      synchronized (executionLock) {
        logger.debug(Thread.currentThread().getName() + " waiting to get notified");
        try {
          executionLock.wait();
          if (isSliderValueChanged) {
            stopExecutionTimer();
            startExecutionTimerContinuously((long) slider.getValue());
            isSliderValueChanged = false;
          }
        } catch (InterruptedException e) {
          logger.debug(Thread.currentThread().getName() + " interrupted");
          return;
        }
        logger.debug(Thread.currentThread().getName() + " got notified");
      }
    } else if (type == EventType.AFTER_ELEMENT) {
      for (UIGraph graph : getGraphs()) {
        for (UIElement uiElement : graph.getElements()) {
          if (element.getId().equals(uiElement.getElementId())) {
            Platform.runLater(() -> {
              currentGraphIndex = graphs.indexOf(graph);
              executionDelay.bind(slider.valueProperty());
              tabPane.getSelectionModel().select(currentGraphIndex);
              uiElement.highlight(false);
              uiElement.visited(true);
            });
          }
        }
      }
    }
  }

  public void setElementStartId(Node currentElement) {
    for (UIGraph graph : getGraphs()) {
      for (Node node : graph.getContentPane().getChildren()) {
        if (node == currentElement) {
          startElementId = ((UIElement) node).getElementId();
          continue;
        }
        ((UIElement) node).setStartElement(false);
      }
    }
  }

  public void centerNodeInScrollPane(ZoomableScrollPane scrollPane, Node node) {
    double x = 0;
    double y = 0;

    if (node instanceof UIVertex) {
      x = node.getBoundsInParent().getMinX();
      y = node.getBoundsInParent().getMinY();
    } else if (node instanceof UIEdge) {
      Node label = ((UIEdge) node).getLabelName();
      x = label.getBoundsInParent().getMinX();
      y = label.getBoundsInParent().getMinY();
    }

    scrollPane.setVvalue(y / 1e6);
    scrollPane.setHvalue(x / 1e6);
  }

  public void setElementStartId(String id) {
    startElementId = id;
    for (UIGraph graph : getGraphs()) {
      for (Node node : graph.getContentPane().getChildren()) {
        if (((UIElement) node).getElementId().equals(startElementId)) {
          ((UIElement) node).setStartElement(true);
        } else {
          ((UIElement) node).setStartElement(false);
        }
      }
    }
  }

  public List<UIGraph> getGraphs() {
    return graphs;
  }

  public UIGraph getCurrentGraph() {
    return graphs.get(currentGraphIndex);
  }

  public Set<String> getListOfSharedStateLabels() {
    return listOfSharedStateLabels;
  }

  public void setListOfSharedStateLabels(Set<String> listOfSharedStateLabels) {
    this.listOfSharedStateLabels = listOfSharedStateLabels;
  }

  public Scene getScene() {
    return scene;
  }

  public TableView<LogEntry> getExecutionWindow() {
    return executionWindow;
  }

  public TabPane getTabPane() {
    return tabPane;
  }
}
