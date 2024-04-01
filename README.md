# RHDH Infra Samples

This repository provides sample files and instructions to play with Red Hat Developer Hub(Backstage) and different types of Infrastructure. 


## Sample 1 - Authentication
The below sample will 
1 Ceate a project in OCP
2 Install Red Hat SSO for OIDC authentication
3 Create a Red Hat SSO Instance Instance,Realm and 3 Users(admin,user1,user2)
4 Install Red Hat Developer Hub via Helm
5 Give admin user the admin role, user1 the catalog-admin role and user2 is a standard user.

### Requirements
    - OCP Cluster => 4.12
    - oc command line tool
    - Helm 3.2.0 or later is installed.
    - PersistentVolume provisioner support in the underlying infrastructure is available.
    - Tested with version 1.1.0 of  openshift-helm-charts/redhat-developer-hub
    - Dependecies of the Chart can change. Please review below for other dependencies.
    ```bash
    helm show readme --version 1.1.0 openshift-helm-charts/redhat-developer-hub
    ```


### Installation

Follow the steps below to install Keycloak and Red Hat Developer Hub:

### Steps
  - Set Variables  
      
      Set Namespace to create resources In
      ```bash
      export NAMESPACE=backstage-test
      ```

      Set the Basedomain for OCP routes
      ```bash
      export BASEDOMAIN=$(oc get ingresses.config.openshift.io/cluster -o jsonpath='{.spec.domain}')
      ```
      
      Set a secret for SSO Client
      ```bash
      export BACKSTAGE_CLIENT_SECRET=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
      ```

      Set an Auth Session Secret
      ```bash
      export AUTH_SESSION_SECRET=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
      ```

      Set Keycloak Base URL
      ```bash
      export KEYCLOAK_BASE_URL=https://keycloak-${NAMESPACE}.${BASEDOMAIN}
      ```

      Set Keycloak Realm Name
      ```bash
      export KEYCLOAK_REALM=backstage
      ```

  - Create deploy namespace
      ```bash
      oc kustomize ./namespace | envsubst | oc apply -f -
      ```

  - Install the Red Hat SSO Operator
      ```bash
      oc kustomize ./sso-operator/ | envsubst | oc apply -f -
      ```

  - Create the Red Hat SSO Instance,Realm,Client and User. This will create 3 users admin,user1,user2 all with a password set to the value "test".
      ```bash
      oc kustomize ./sso-manifests | envsubst | oc apply -f -
      ```

  - Create our Application Specific Configuration
    ```bash
    cat ./rhdh-manifests/keycloak/app-config-rhdh.yaml  | envsubst '${NAMESPACE}' | oc apply -n ${NAMESPACE} -f - 
    ```

    ```bash
    cat ./rhdh-manifests/keycloak/policy-configmap.yaml  | envsubst '${NAMESPACE}' | oc apply -n ${NAMESPACE} -f -
    ```

  - Wait for Keycloak Instance to be Ready(Can copy all)
    ```bash
    csv=$(oc get subscriptions.operators.coreos.com/rhsso-operator -n ${NAMESPACE} -o jsonpath='{.status.installedCSV}');

  
    oc wait --for=jsonpath='{.status.phase}'=Succeeded ClusterServiceVersion/$csv --allow-missing-template-keys=true --timeout=150s -n $NAMESPACE;
    
    until [ $(curl -k -s -o /dev/null -I -w "%{http_code}" ${KEYCLOAK_BASE_URL}/auth/realms/${KEYCLOAK_REALM}/.well-known/openid-configuration) -eq "200" ];do echo -e "Waiting for Keycloak instance endpoint to become ready at ${KEYCLOAK_BASE_URL}/auth/realms/${KEYCLOAK_REALM}/.well-known/openid-configuration \n" && sleep 10;done
  
    oc wait --for=jsonpath='{.status.ready}'=true --allow-missing-template-keys=true --timeout=120s Keycloak/backstage -n $NAMESPACE
    ```

  - Create our Developer Release via Helm
    ```bash
    helm show values --version 1.1.0 openshift-helm-charts/redhat-developer-hub > ./rhdh-manifests/base/values.yaml
    ```

    ```bash
    helm upgrade -i developer-hub \
    --version 1.1.0 \
    -f ./rhdh-manifests/base/values.yaml \
    -f ./rhdh-manifests/keycloak/values.yaml \
    openshift-helm-charts/redhat-developer-hub -n ${NAMESPACE}
    ```

### Clean Up 
  ```bash
  cat ./rhdh-manifests/keycloak/app-config-rhdh.yaml  | envsubst '${NAMESPACE}' | oc delete -n ${NAMESPACE} -f - ; \
  oc kustomize ./sso-manifests | envsubst | oc delete -f - ; \
  oc kustomize ./sso-operator/ | envsubst | oc delete -f - ; \
  oc kustomize ./namespace | envsubst | oc delete -f -
  ```