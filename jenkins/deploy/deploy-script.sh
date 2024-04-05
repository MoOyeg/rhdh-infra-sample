#!/bin/bash
#Script to create a Jenkins Instance and Jenkins application in namespace 1234-jenkins


export foldername=$(dirname $0)
#App
export SECRET_NAME="github-secret"
export SSHKEY_PATH="/root/.ssh/env_key1"
export MYSQL_USER="github-secret"
export MYSQL_USER="user"
export MYSQL_PASSWORD="pass"
export MYSQL_DB="testdb"
export MYSQL_HOST="mysql"
export NAMESPACE_DEV="apptest"
export NAMESPACE_PROD="appprod"
export APP_NAME="testflask"
export APP_CONFIG="./gunicorn/gunicorn.conf.py"
export APP_MODULE="runapp:app"
export MYSQL_DATABASE="testdb"
export SECRET_NAME="my-secret"
export REPO_SECRET_NAME="repo-secret"
export CODE_URL=https://github.com/MoOyeg/testFlask.git

#Jenkins
export JENKINS_NAMESPACE="1234-jenkins"

#Tekton
export TEKTON_NAMESPACE="1234-tekton"
export TEKTON_PVC_NAME=tekton-repo

function cleanup() {  
    oc delete project ${JENKINS_NAMESPACE}
    oc delete project ${NAMESPACE_DEV}
    oc delete project ${NAMESPACE_PROD}
}

if [ $# -eq 1 ]; then
    if [ $1 == "cleanup" ]; then
        cleanup
        exit 0
    fi
fi

oc new-project $JENKINS_NAMESPACE
oc new-project $NAMESPACE_DEV
oc new-project $NAMESPACE_PROD
oc process -f $foldername/template.yaml | oc apply -f - -n ${JENKINS_NAMESPACE}
oc policy add-role-to-user edit system:serviceaccount:$JENKINS_NAMESPACE:jenkins -n $NAMESPACE_DEV 
oc policy add-role-to-user edit system:serviceaccount:$JENKINS_NAMESPACE:jenkins -n $NAMESPACE_PROD
oc policy add-role-to-user edit system:serviceaccount:$JENKINS_NAMESPACE:default -n $NAMESPACE_DEV
oc policy add-role-to-user edit system:serviceaccount:$JENKINS_NAMESPACE:default -n $NAMESPACE_PROD
export JENKINS_BASE_IMAGE=registry.redhat.io/openshift4/ose-jenkins-agent-base:v4.10.0
export PYTHON_DOCKERFILE=$(cat $foldername/Dockerfile | envsubst )
oc new-build --strategy=docker -D="$PYTHON_DOCKERFILE" --name=python-jenkins -n $JENKINS_NAMESPACE
echo """
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: "$APP_NAME-pipeline"
  namespace: $JENKINS_NAMESPACE
spec:
  source:
    git:
      ref: master
      uri: 'https://github.com/MoOyeg/testFlask-Jenkins.git'
    type: Git
  strategy:
    type: "JenkinsPipeline"
    jenkinsPipelineStrategy:
      jenkinsfilePath: Jenkinsfile
""" | oc create -f -

oc set env bc/$APP_NAME-pipeline \
--env=JENKINS_NAMESPACE=$JENKINS_NAMESPACE \
--env=REPO="https://github.com/MoOyeg/testFlask.git" \
--env=DEV_PROJECT=$NAMESPACE_DEV --env=APP_NAME=$APP_NAME \
--env=BRANCH=release-2.0 \
--env=MYSQL_USER=$MYSQL_USER --env=MYSQL_PASSWORD=$MYSQL_PASSWORD \
--env=APP_CONFIG=$APP_CONFIG --env=APP_MODULE=$APP_MODULE \
--env=MYSQL_HOST=$MYSQL_HOST --env=MYSQL_DATABASE=$MYSQL_DATABASE --env=PROD_PROJECT=$NAMESPACE_PROD -n $JENKINS_NAMESPACE

echo -e "Will wait for Sample App Jenkins Agent Base Image to be Built, Can take up to 6 mins\n"
buildno=$(oc get bc/python-jenkins -n ${JENKINS_NAMESPACE} -o jsonpath='{.status.lastVersion}')
oc wait --for=jsonpath='{.status.phase}'="Complete" Build/python-jenkins-${buildno} \
--allow-missing-template-keys=true --timeout=420s -n ${JENKINS_NAMESPACE};
oc start-build $APP_NAME-pipeline -n ${JENKINS_NAMESPACE}
