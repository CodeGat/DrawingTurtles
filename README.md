# DrawingTurtles
Drawn Ontology to Turtle (a concrete RDF syntax) converter.

Requires JDK/JRE that includes JavaFX. 

Simply run 'java -jar DrawingTurtles.jar' to open the application.
Click on the canvas to add a new graph node, and click between nodes to add properties. 
The type of the node (Global Literal, Instance Literal Placeholder or Class) is automatically asserted based on how it conforms to the Turtle specification. 
Prefixes can be added in the Prefixes menu, so you don't have to type out the full IRI. 
The graph is exportable as a .png, .tll file, or an instance-level .ttl file if you ingest a .csv file (and correlate the .csv headers with the graph nodes). 
