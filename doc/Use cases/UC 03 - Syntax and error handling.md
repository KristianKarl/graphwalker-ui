# Use Case 03: Syntax and error handling

### Description
As a user, I get error information while I create and run a model.


## Basic course of events

### Preconditions

Use the latest version of GraphWalker UI:
```sh
git clone https://github.com/GraphWalker/graphwalker-project.git
mvn package -pl graphwalker-ui -am
cd graphwalker-ui
java -jar target/graphwalker-ui-4.0.0-SNAPSHOT.jar
```

#### Events

1. The user starts with an empty model.
1. Click the `Run` button in the toolbar.
1. Add a vertex `A`.
1. Click the `Run` button in the toolbar.
1. Set the vertex `A` as the `Start element`.
1. Click the `Run` button in the toolbar.
1. Add a an additional vertex `B`.
1. Click the `Run` button in the toolbar.
1. Connect vertex `B` with `A`. 
1. Click the `Run` button in the toolbar.
1. Connect vertex `A` with `B`. 
1. Click the `Run` button in the toolbar.


#### Results
1. 