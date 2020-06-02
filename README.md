# INTERN-PROJECT-2020


ABOUT THE UPLOADED DOCUMENTS : -

1. The document can directly be downloaded and opened using eclipse. After that, one can build it and generate the jar files.
The JAR file is the final compiled form of the project and can be run dirctly in a lambda function at aws with RequestHandle method given by dbmsHandler.Handle::RequestHandle.
2. The Tables Need special Role defination allowing the complete access of lambda function to the DynamoDB Table.
3. The JAR file work on the following tables with table-name: playlist-records and user-info-playlist.
4. One needs to add a trigger to the table named playlist-records calling the lambda function with the JAR
5. The outcomes and logs can be seen as well. We have a log keeping utility class called DbmsLogKeeper.java
6. The Dependency POM.xml file contains list of all dependencies and build specifications. On building with eclipse, it will auto download all the dependencies.
7. The most important file which contains the main code is called the Handler.java


STAR (SITUATION->TASK->ACTION->RESULT)

SITUATION Any track that the customer buys goes directly into the library of the client as per the current scenario. The situation here is to improvise on the current status by giving a ranked list of his playlists to the client and allow him to choose the playlist in which he/she wants the new track to be placed. The playlists should be ranked based on the likeliness of the given track going into them. This would make things easier for our clients.

TASK The task in hand is to create a system whereby the client gets a list of his created playlists ranked based on the likelihood of the chosen track going into it.

ACTION We will create a system which will list out all the playlists created by the client in an order based on the likelihood of the chosen track to go into it. To achieve this, we have chosen a set of features which is the major potential deciding factor for our client to choose a playlist for a given track. Next, we map the track features with the playlist features (Current state of features of the tracks in the playlist) and based on the results and other methods (specified later) we would perform the ranking operation.

RESULT We would be able to achieve the assigned task which in turn will potentially improvise the current mechanism and enhance client experience. This would be in line with our leadership principles of “Invent & Simplify”, “Customer Obsession” and our policy of “Insisting on the highest standards”.
