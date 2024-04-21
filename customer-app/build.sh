#!/bin/bash -x
foldername=$(dirname $0)
APP_NAMESPACE="customer-app"

oc new-project $APP_NAMESPACE

#Build Binary
cd $foldername
# ./mvnw package -Dnative -Dmaven.test.skip=true -Dquarkus.native.container-build=true

# oc new-build --binary=true --name=customer-app -n $APP_NAMESPACE
# oc start-build customer-app --from-dir=. --follow -n $APP_NAMESPACE
oc kustomize ./deploy/ | oc apply -f - -n $APP_NAMESPACE