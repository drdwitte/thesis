# Environment and deploy file
# For use with bin/server_deploy, bin/server_package etc.
DEPLOY_HOSTS="192.168.1.4"

APP_USER=brecht
APP_GROUP=brecht
# optional SSH Key to login to deploy server
#SSH_KEY=/path/to/keyfile.pem
INSTALL_DIR=/home/spark/job-server
LOG_DIR=/var/log/job-server
PIDFILE=spark-jobserver.pid
JOBSERVER_MEMORY=1G
SPARK_VERSION=1.6.0
SPARK_HOME=/usr/local/spark
SPARK_CONF_DIR=$SPARK_HOME/conf
# Only needed for Mesos deploys
SPARK_EXECUTOR_URI=/home/spark/spark-1.6.0.tar.gz
# Only needed for YARN running outside of the cluster
# You will need to COPY these files from your cluster to the remote machine
# Normally these are kept on the cluster in /etc/hadoop/conf
# YARN_CONF_DIR=/pathToRemoteConf/conf
# HADOOP_CONF_DIR=/pathToRemoteConf/conf
#
# Also optional: extra JVM args for spark-submit
# export SPARK_SUBMIT_OPTS+="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5433"
SCALA_VERSION=2.11.7 # or 2.11.6
