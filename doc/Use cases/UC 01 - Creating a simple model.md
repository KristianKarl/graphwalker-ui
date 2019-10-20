# Use Case 01: Creating a simple model

### Description
As a user, I want to create a simple and runnable GraphWalker model.


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
1. The user adds a couple of vertices (or nodes), by `CTRL + left` clicking with the mouse in the window called "New Model".
1. The user connects all vertices by `SHIFT + left` clicking once on the first vertex, then moving the mouse to the target vertex, and left click.
1. The user right-click on some element (vertex or edge), and set `Start element ` to true.
1. The user clicks the `Play` button

#### Results
* The application renders the vertices and edges as the users place them in the window.
* While traversing, the path will be logged in the `Execution output` window.
* While traversing, visited elements (edges and vertices) will be colored `light grey`.
* While traversing, the current element will be highlighted, by being colored `light blue`, and the font of the label of the element changing to `bold`. 


### Alternative path: Moving an element
#### Preconditions
All parts of `Basic course of events` has been executed. 

#### Events
1. The user moves a vertex in the model.

#### Results
* When a vertex is moved, all connected edges to that vertex moves with it.


## Alternative path: Removing elements
### Preconditions
All parts of `Basic course of events` has been executed. 

#### Events
1. The user removes a vertex from the model.
1. The user removes an edge from the model.

#### Results
* When a vertex is removed, all connected edges to that vertex are also removed.
* When an edge is removed, only that element is removed.
