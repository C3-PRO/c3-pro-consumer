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
* **I2B2 instance**: *TODO* (NOT IMPLEMENTED YET)

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

## AWS SDK credentials ##

The system uses the Java AWS SDK provided by Amazon. The SDK will be installed automatically since it is a maven dependency. However, it grabs the credentials to access the S3 bucket and SQS from a file that should be located here:

    $HOME/.aws/credentials

The content of the file should be something like:

    [sqsqueue]
    aws_access_key_id={{access_key_to_SQS_and_S3}}
    aws_secret_access_key={{secret}}

To obtain access keys and secrets from AWS, visit http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html. We suggest to create a user in AWS-IAM with only permissions to access SQS, and generate the access key and secret for this user.

## Generating and installing public-private keys ##

The information retrieved from the SQS is encrypted using a symmetric key. Such symmetric key is send via metadata of the elements and it's also encrypted using a public key. The C3-PRO-Consumer uses its private key to decrypt the symmetric key and finally decrypt the fhir resource. 

To generate a key pair, execute the following command:

    $C3PRO_CONSUMER_HOME/src/main/scripts/key-generator.sh

The following files will be generated:

    public-c3pro.der
    private-c3pro.der

*public-c3pro.der* is the public key file and must be uploaded to the S3 bucket used by C3-PRO-Server. See https://bitbucket.org/ipinyol/c3pro-server/overview for details

*private-c3pro.der* is the private key file and, under any circumstance, can be shared or distributed. It should be backed up in a secure device and install it in the following directory:

    ~/.c3pro/private-c3pro.der

If this private key is lost, it won't be possible to recuperate the messages in the queue.