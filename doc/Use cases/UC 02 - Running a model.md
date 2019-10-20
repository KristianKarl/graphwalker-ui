# Use Case 02: Running a model

### Description
As a user, I can interact with the app while it's  traversing a model.


## Basic course of events

### Preconditions

Use the latest version of GraphWalker UI:
```sh
git clone https://github.com/GraphWalker/graphwalker-project.git
mvn package -pl graphwalker-ui -am
cd graphwalker-ui
```

#### Events

1. The user starts the application from command line:
   ```sh
   java -jar target/graphwalker-ui-4.0.0-SNAPSHOT.jar --model src/test/resources/json/PetClinic.json
   ```
1. Load the model from file.
1. Click the `Run` button in the toolbar.
1. Click the `Pause` button in the toolbar.
1. Click the `Step forward` button in the toolbar.
1. Click the `Stop` button in the toolbar.
1. Change the value in the `Delay` slider.


#### Results
1. When the `Run` button is pressed, the traversing of the models starts, and the button changes to a `Pause` button.
1. The traversing is visualized to the user by highlighting the elements as they are visited.
1. In the `Execution log` window, each visited element is added to the list.
1. When the `Pause` button is pressed, the traversing of the models is paused, and the button changes to a `Run` button.
1. If the traversing reaches it's end, the stop conditions for al generators are fulfilled: 
   * the traversing halts.
   * The `Play` button is disabled.
   * The `Step forward` button is disabled.
1. If traversing is paused, and the `Run` button is pressed, the traversing of the models continues,  and the button changes to a `Pause` button.
1. The `Step forward` button is only enabled when the traversing is paused.
1. If the `Step forward` button is clicked, the models are traversed by one step, then paused again.
1. The `Stop` button is always enabled.
1. When the `Stop` button is pressed, the traversing stops, and the models are reset to their initial state.
1. The `Delay` slider changes the delay in milliseconds, which graphwalker uses to wait when each step is traversed:
    * The changes can be done during traversing of models.
     * The slider are individually for each model. 
