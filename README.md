# INTERN-PROJECT-2020

MAJOR CLASSES:- 

1. Constants.java - Constants declarations.
2. DbmsLogKeeper.java - For maintaining the Log.
3. PlaylistUpdateHandler.java - Main Handler method for the Lambda Function.
4. TableUpdateVariables.java
5. POM.XML - Dependencies list of maven and build specifications

ABOUT THE UPLOADED DOCUMENTS : -

1. The document can directly be downloaded and opened using eclipse. After that, one can build it and generate the jar files.
The JAR file is the final compiled form of the project and can be run dirctly in a lambda function at aws with RequestHandle method given by dbmsHandler.PlaylistUpdateHandler::RequestHandle.
2. The Tables Need special Role defination allowing the complete access of lambda function to the DynamoDB Table.
3. The JAR file work on the following tables with table-name: playlist-records and user-info-playlist.
4. One needs to add a trigger to the table named playlist-records calling the lambda function with the JAR
5. The outcomes and logs can be seen as well. We have a log keeping utility class called DbmsLogKeeper.java
6. The Dependency POM.xml file contains list of all dependencies and build specifications. On building with eclipse, it will auto download all the dependencies.
7. The most important file which contains the main code is called the Handler.java
