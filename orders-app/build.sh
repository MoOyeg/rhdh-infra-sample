#!/bin/bash -x
foldername=$(dirname $0)
APP_NAMESPACE="order-app"

oc new-project $APP_NAMESPACE

#Build Binary
cd $foldername
# ./mvnw package -Dnative -Dmaven.test.skip=true -Dquarkus.native.container-build=true

# oc new-build --binary=true --name=order-app -n $APP_NAMESPACE
# oc start-build order-app --from-dir=. --follow -n $APP_NAMESPACE
oc kustomize ./deploy/ | oc apply -f - -n $APP_NAMESPACE