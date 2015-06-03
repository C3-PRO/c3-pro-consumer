# C3-PRO-Consumer #

C3-PRO-Consumer is a system that consumes from an AWS SQS queue and stores the raw elements in an oracle DB and exports the data into an i2b2 instance (https://www.i2b2.org/). The elements in the queue are FHIR resources pushed trough C3-PRO-Server (https://bitbucket.org/ipinyol/c3pro-server/overview).

The system servers the following rest methods to start and stop the consumption of elements in the queue:

    HTTP/1.1 POST /c3pro-consumer/rest/actions/start
    HTTP/1.1 POST /c3pro-consumer/rest/actions/stop

# Configuration and Deployment #

## Prerequisites ##

The system uses the following external resources:

* **SQS queue**: A queue deployed in AWS to consume from. This queue must be configured and populated as described in https://bitbucket.org/ipinyol/c3pro-server/overview.
* **Oracle DB**: An oracle schema is needed to store the raw data from the SQS. Ideally, this schema should be located in the intranet of an organization.

The configuration of the DB and the access to SQS is explained in the below sections.

## Installing Maven, Java && JBoss AS7 ##

The system uses java 7 and we recommend to use JBoss AS7. To install the basic tools in a Debian-based Linux distribution:

    sudo apt-get clean
    sudo apt-get update
    sudo apt-get install openjdk-7-jdk
    sudo apt-get install unzip
    sudo apt-get install maven
    wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip
    sudo unzip jboss-as-7.1.1.Final.zip -d /usr/share/
    sudo chown -fR {{you_chosen_user}}:{{you_chosen_user}} /usr/share/jboss-as-7.1.1.Final/

## Oracle DB configuration ##

The system uses an oracle DB to store the raw information extracted form the queue. Here are the steps to configure the DB properly:

* Run the table creation script: *{{src/main/scripts/create_tables.sql}}*

* Deploy the provided oracle jdbc driver in jBoss:

```
#!shell
$HOME_C3PRO_CONSUMER/cp ojdbc14.jar $JBOSS_HOME/standalone/deployments
```

* Configure the data source by editing the file *$JBOSS_HOME/standalone/configuration/standalone.xml*. In the data source section place the following:

```
#!xml

<datasource jndi-name="java:jboss/datasources/c3proDS" pool-name="c3proDS" enabled="true" use-java-context="true">
    <connection-url>{{jdbc_connection_to_db}}</connection-url>
    <driver>ojdbc14.jar</driver>
    <security>
        <user-name>{{db_username}}</user-name>
        <password>{{db_password}}</password>
    </security>
</datasource>
```

## Building and deploying in DEV ##

Once the project is cloned or download, in the root of the project:

    mvn clean package
    mvn jboss-as:deploy

The previous instructions take the resource files located in *src/main/resources/dev* and place them as the resource files of the deployment. This requires JBoss on:

    $JBOSS_HOME/bin/standalone.sh

To stop JBoss:

    $JBOSS_HOME/bin/jboss-cli.sh --connect command=:shutdown


## Building in QA and PROD environment ##

In QA:

    mvn clean package -Pqa
    mvn jboss-as:deploy

In PROD:

    mvn clean package -Pprod
    mvn jboss-as:deploy

These commands take the resource files located in *src/main/resources/qa* or *src/main/resources/prod* respectively, and place them as the resource files of the deployment.

