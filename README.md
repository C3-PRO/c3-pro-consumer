# C3-PRO-Consumer #

C3-PRO-Consumer is a system that consumes from an AWS SQS queue, stores the raw elements in an oracle DB and exports the data into an i2b2 instance (https://www.i2b2.org/). The elements in the queue are FHIR resources pushed trough C3-PRO-Server (https://bitbucket.org/ipinyol/c3pro-server/overview).

The system servers the following rest methods to start and stop the consumption of elements in the queue:

    HTTP/1.1 POST /c3pro-consumer/rest/actions/start
    HTTP/1.1 POST /c3pro-consumer/rest/actions/stop

# Configuration and Deployment #

## Prerequisites ##

The system uses the following external resources:

* **SQS queue**: A queue deployed in AWS to consume from. This queue must be configured and populated as described in https://bitbucket.org/ipinyol/c3pro-server/overview.
* **Oracle DB**: An oracle schema is needed to store the raw data from the SQS. Ideally, this schema should be located in the intranet of an organization.
* **FHIR DSTU-2** compliant system: To store the consumed resourced. In the current release we store the data in i2b2 through the newly created [i2b2 fhir cell](https://bitbucket.org/ihlchip/fhir-i2b2-cell).

## Installing Maven, Java && JBoss AS7 ##

The system uses java 7 and we recommend to use JBoss AS7, although other java-based web servers can be used, like tomcat7. To install the basic tools in a Debian-based Linux distribution:

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

* Deploy the provided oracle jdbc driver in jBoss or anywhere accessible through the project:

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

* **Note for production deployments**: It's not recommended to display raw DB credentials in the configuration files, even when the servers are protected. One possible way is to use security domains to wrap encrypted credentials. For instance:
  
```
#!xml

<datasource jndi-name="java:jboss/datasources/c3proDS" pool-name="c3proDS" enabled="true" use-java-context="true">
    <connection-url>{{jdbc_connection_to_db}}</connection-url>
    <driver>ojdbc14.jar</driver>
    <security>
        <security-domain>secure-c3pro-credentials</security-domain>
    </security>
</datasource>
```

and in the security domain section:

```
#!xml
<security-domain name="secure-c3pro-credentials" cache-type="default">
   <authentication>
      <login-module code="org.picketbox.datasource.security.SecureIdentityLoginModule" flag="required">
          <module-option name="username" value="{{db_username}}"/>
          <module-option name="password" value="{{ENCRYPTED PASSWORD}}"/>
       </login-module>
    </authentication>
</security-domain>
```

The encrypted password can be generated running **picketbox** security module as follows:

    java  org.picketbox.datasource.security.SecureIdentityLoginModule {{db_password}}

The output will be the encrypted password to place in the security domain element. Make sure that your CLASS_PATH includes the appropriate jar file. PICKET BOX is included by default in JBOSS AS7 distribution as a module. 
 
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

## Deploying on web server containers different than JBOSS##

Generate the war files for the desired environment

    mvn clean package
    mvn clean package -Pqa
    mvn clean package -Pprod

and copy the generated war located in **target/c3pro-consumer.war** to the corresponding deployment directory. In **tomcat7** the default directory is:

    /var/lib/tomcat7/webapps/

## AWS SDK credentials ##

The system uses the Java AWS SDK provided by Amazon. The SDK will be installed automatically since it is a maven dependency. However, it grabs the credentials to access the S3 bucket and SQS from a file that should be located here:

    $HOME/.aws/credentials

The content of the file should be something like:

    [sqsqueue]
    aws_access_key_id={{access_key_to_SQS_and_S3}}
    aws_secret_access_key={{secret}}

To obtain access keys and secrets from AWS, visit http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html. We suggest to create a user in AWS-IAM with only permissions to access SQS, and generate the access key and secret for this user.

## Generating and installing public-private keys ##

The information retrieved from the SQS is encrypted using a symmetric key. Such symmetric key is sent via metadata of the elements inserted in the queue, and it's also encrypted using a public key. Also, the ID of the public key is sent as metadata. The C3-PRO-Consumer uses the corresponding private key to decrypt the symmetric key and finally decrypt the fhir resource. 

To generate and install a new key pair follow the steps:

**(1) Generate a new UUID** 
This will be the new ID of the key

**(2) Inform the server about the new ID** 
In the corresponding S3 Bucket, upload a new text file containing the new UUID. See (https://bitbucket.org/ipinyol/c3pro-server/overview)

**(3) Generate the public-private keys**

Execute the following command:

    $C3PRO_CONSUMER_HOME/src/main/scripts/key-generator.sh

These files will be generated:

    public-c3pro.der
    private-c3pro.der

*public-c3pro.der* is the public key file and must be uploaded to the S3 bucket used by C3-PRO-Server. See https://bitbucket.org/ipinyol/c3pro-server/overview for details

*private-c3pro.der* is the private key file and, under any circumstance, can be shared or distributed. It should be backed up in a secure device and install it in the following directory:

    ~/.c3pro/{{new UUID}}/private-c3pro.der

If this private key is lost, it won't be possible to recuperate the messages in the queue.

## Configuration Parameters ##

There is one configuration parameters file for each environment (dev, qa and prod). They are located here:

    src/main/resources/dev/org/bch/c3pro/consumer/config/config.properties
    src/main/resources/qa/org/bch/c3pro/consumer/config/config.properties
    src/main/resources/prod/org/bch/c3pro/consumer/config/config.properties

### SQS configuration access ###

*Url connection to Amazon SQS queue*

    app.aws.sqs.url=https://sqs.us-west-2.amazonaws.com/875222989376/testQ

*name of the SQS*

    app.aws.sqs.name=testQ


*Amazon profile for the SQS connection*

    app.aws.sqs.profile=sqsqueue


*Amazon region where the SQS is deployed*

    app.aws.sqs.region=us-west-2


### Property names of the Queue message (should not be changed! or changed in tune with the Research Kit App) ###
They are the property names of the messages in the queue

*The property name that holds the private symmetric AES key*

    app.security.metadatakey=pkey

*The property name that holds the public key id used to encrypt the private symmetric AES key*

    app.security.metadatakeyid=pkey_id

### Encryption parameters (should not be changed! or changed in tune with the Research Kit App and the c3pro-server) ###

*The asymmetric full algorithm used to encrypt and decrypt the symetric random key

    app.security.privatekey.algorithm=RSA/ECB/OAEPWithSHA1AndMGF1Padding

*The asymmetric BASE algorithm used to encrypt and decrypt the symmetric random key*

    app.security.privatekey.basealgorithm=RSA

*The symmetric full algorithm used to encrypt and decrypt resources*

    app.security.secretkey.algorithm=AES/CBC/PKCS5Padding


*The symmetric BASE algorithm used to encrypt and decrypt resources*

    app.security.secretkey.basealgorithm=AES

*The key size in bytes of the random symmetric key*

    app.security.secretkey.size=16

*The private key file name*

    app.security.privatekey.filename=private-c3pro.der

*The private key base path name. The complete path where will be 'app.security.privatekey.basepath'/pkey_id/'app.security.privatekey.filename' where pkey_id is the key id*

    app.security.privatekey.basepath=/home/vagrant/.c3pro/


### End point and connection information of the running fhir compliant instance to store the resources. In this case, the i2b2 fhir cell ###

*The host name*

    app.host.fhir.i2b2=127.0.0.1

*The end points. this is changed in the new version*

    app.endpoint.fhir.i2b2.qa=/fhir-i2b2/fhir/QuestionnaireAnswers
    app.endpoint.fhir.i2b2.obs=/fhir-i2b2/fhir/Observation
    app.endpoint.fhir.i2b2.con=/fhir-i2b2/fhir/Contract
    app.endpoint.fhir.i2b2.pat=/fhir-i2b2/fhir/Patient


*The connection port*

    app.port.fhir.i2b2=9090

*The transport protocol*

    app.network.protocol.fhir.i2b2=http

### Integration test variables (optional)###

    app.c3pro.server.host=ec2-52-11-82-72.us-west-2.compute.amazonaws.com
    app.c3pro.server.port=8080
    app.c3pro.server.transport=http
    app.authfile.c3pro.server=[JBOSS_HOME]/standalone/configuration/credentials.c3pro
    app.c3pro.consumer.datasourcefile=[JBOSS_HOME]/standalone/configuration/jdbc.c3pro