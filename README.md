sonar-cloud-key = 962bde7e-6cb4-4bbf-8a53-f5b1616deaf1
project-key=1c6a274c-6721-477b-b100-4dd4cad0218c
project-name=tp-CI/CD


CI/CD TP1 - Docker
==================

### Database

    FROM postgres:11.6-alpine
    COPY docker-entrypoint-initdb.d/ docker-entrypoint-initdb.d/
    ENV POSTGRES_DB=db \
        POSTGRES_USER=usr \
        POSTGRES_PASSWORD=pwd

    docker network create tp1
    docker build -t tp1-database
    docker run -p 5432:5432 --name tp1-database --network tp1 -v /docker-tp/1/data:/var/lib/postgresql/data tp1-database
    docker run --network tp1 -p 8080:8080 adminer

> Why should we run the container with a flag -e to give the environment variables ?

Cela permet de ne pas avoir a changer d’image ou de devoir rebuild si jamais les variables changeraient deplus cela n’est pas très sécurisé.

> Why do we need a volume to be attached to our postgres container ?

Cela permet de conserver les données même quand le container est redémarré.

### Backend API

    FROM openjdk:16-alpine3.13
    COPY target .
    # set the startup command to execute the jar
    CMD ["java", "Main"]

    docker build -t tp1-api .
    docker run -p 8080:8080 --network tp1 --name tp1-api tp1-api

> Why do we need a multistage build ? And explain each steps of this dockerfile

Cela nous évite de télécharger les dépendances sur le docker qui fat tourner l’application et de réduire les commandes manuelles. De plus les jdks sont relativement lourd, ils sont juste installés pour la partie de build. Après les jres sont executés sur l’autre partie.

### HTTP

    # Dockerfile
    FROM httpd:2.4
    COPY index.html /usr/local/apache2/htdocs/
    COPY httpd.conf /usr/local/apache2/conf/
    EXPOSE 80

    # httpd.conf
    LoadModule proxy_module modules/mod_proxy.so
    LoadModule proxy_http_module modules/mod_proxy_http.so
    
    ServerName localhost
    <VirtualHost *:80>
        ProxyPreserveHost On
        ProxyPass / http://tp1-api:8080/
        ProxyPassReverse / http://tp1-api:8080/
    </VirtualHost>

    docker build -t http .
    docker run -p 80:80 --network tp1 --name http http
    docker cp http:/usr/local/apache2/conf/httpd.conf .

> Why do we need a reverse proxy ?

Un reverse proxy sert d’intermédiaire entre les clients et les ressources du serveur, il permet de gérer les connections/requêtes pour les rediriger et/ou les bloquer.

### Docker-compose

    version: '3.3'
    # Liste des conteneurs
    services:
      # API
      tp1-api:
        # Dossier de l'API
        build: 
          ./API/simple-api-test
        # Relier au réseau tp1
        networks:
          - tp1
        # Attend tp1-database avant de se lancer
        depends_on:
          - tp1-database
      # Database
      tp1-database:
        # Dossier de la database
        build:
          ./Database
        # Relier au réseau tp1
        networks:
          - tp1
      # Http
      httpd:
        # Dossier de l'http
        build:
          ./HTTP server
        # Binding du port 80 de la machine hôte vers le docker
        ports:
          - "80:80"
        # Relier au réseau tp1
        networks:
          - tp1
        # Attend tp1-tp1-api avant de se lancer
        depends_on:
          - tp1-api
    networks:
      # Création du réseau tp1
      tp1:

> Why is docker-compose so important ?

Docker-compose permet d’automatiser le lancement des conteneurs, deplus il permet de réduire les erreurs concernant la configuration des réseaux par exemple.

> Document docker-compose most important commands

    docker-compose up # Lancer le docker-compose
    docker-compose stop # Arreter les dockers liés au docker-compose
    docker-compose down # Supprimmer les conteneurs PublishPublish

### Publish

    docker tag tp1_tp1-api zennyth/api:1.0
    docker push zennyth/api:1.0
    ...

> Why do we put our images into an online repository ?

Cela permet de pouvoir pull les images depuis un poste distant, donc nos collègues pourraient pull les images


CI/CD TP2 - Gihub actions
=========================
```
    cd simple-api-main
    mvn clean verify
```
> Ok, what is it supposed to do ?

La commande permet de build le projet maven puis d’exécuter les tests d’intégrations

> What are testcontainers?

Ce sont des librairies java qui permettent de lancer des conteneurs pendant les tests

> Why did we put needs: build-and-test-backend on this job? Maybe try without this
> 
> and you will see !

Eviter de build les dockers si jamais les tests n’étaient pas validé
```
    name: CI devops 2022 CPE
    on:
      #to begin you want to launch this job in main and develop
      push:
        branches:
          # Pour la branche master
          - master
      pull_request:
    jobs:
      test-backend:
        runs-on: ubuntu-18.04
        steps:
          #checkout your github code using actions/checkout@v2.3.3
          - uses: actions/checkout@v2.3.3
          #do the same with another action (actions/setup-java@v2) that enable to setup jdk 11
          - name: Set up JDK 11
            uses: actions/setup-java@v2
            with:
              # On utilise la version 11 avec la distribution adopt
              java-version: '11'
              distribution: 'adopt'
          #finally build your app with the latest command
          - name: Build and test with Maven
            # build and test the java project
            run: mvn clean verify --file ./API/simple-api -DskipTests
      
      # define job to build and publish docker image
      build-and-push-docker-image:
        needs: test-backend
        # run only when code is compiling and tests are passing
        runs-on: ubuntu-latest
        # steps to perform in job
        steps:
          - name: Login to DockerHub
            run: docker login -u ${{ secrets.DOCKERHUB_USERNAME }} -p ${{ secrets.DOCKERHUB_PASSWORD }}
          - name: Checkout code
            uses: actions/checkout@v2
          - name: Build image and push backend
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./API/simple-api
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/simple-api:1.0
              push: ${{ github.ref == 'refs/heads/master' }}
          - name: Build image and push database
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./Database
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/my-database:1.0
              push: ${{ github.ref == 'refs/heads/master' }}
          - name: Build image and push httpd
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./HTTP server
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/http:1.0
              push: ${{ github.ref == 'refs/heads/master' }}
```
> Secured variables, why ?

Pour éviter les éventuelles fuites d’informations

> Why did we put needs: build-and-test-backend on this job? Maybe try without this
> 
> and you will see !

Pour éviter de build les images docker si notre api ne passe pas les tests, cela nous ferait perdre beaucoup de temps et d’argent

> For what purpose do we need to push docker images?

Garder une trace du code avec une version qui marche

#### Sonar

    name: CI devops 2022 CPE
    on:
      #to begin you want to launch this job in main and develop
      push:
        branches:
          # Pour la branche master
          - master
      pull_request:
    jobs:
      test-backend:
        runs-on: ubuntu-18.04
        steps:
          #checkout your github code using actions/checkout@v2.3.3
          - uses: actions/checkout@v2.3.3
          #do the same with another action (actions/setup-java@v2) that enable to setup jdk 11
          - name: Set up JDK 11
            uses: actions/setup-java@v2
            with:
              # On utilise la version 11 avec la distribution adopt
              java-version: '11'
              distribution: 'adopt'
          - name: Analyze with sonar
            env:
              GITHUB_TOKEN: ${{secrets.GITHUB_TOKEN}}
              SONAR_TOKEN: ${{secrets.SONAR_TOKEN}}
            run: mvn -B verify sonar:sonar -Dsonar.projectKey=1c6a274c-6721-477b-b100-4dd4cad0218c -Dsonar.organization=962bde7e-6cb4-4bbf-8a53-f5b1616deaf1 -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${{secrets.SONAR_TOKEN }} --file ./API/simple-api
          #finally build your app with the latest command
          - name: Build and test with Maven
            # build and test the java project
            run: mvn clean verify --file ./API/simple-api
      
      # define job to build and publish docker image
      build-and-push-docker-image:
        needs: test-backend
        # run only when code is compiling and tests are passing
        runs-on: ubuntu-latest
        # steps to perform in job
        steps:
          - name: Login to DockerHub
            run: docker login -u ${{ secrets.DOCKERHUB_USERNAME }} -p ${{ secrets.DOCKERHUB_PASSWORD }}
          - name: Checkout code
            uses: actions/checkout@v2
          - name: Build image and push backend
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./API/simple-api
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/simple-api:1.0
              push: ${{ github.ref == 'refs/heads/master' }}
          - name: Build image and push database
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./Database
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/my-database:1.0
              push: ${{ github.ref == 'refs/heads/master' }}
          - name: Build image and push httpd
            uses: docker/build-push-action@v2
            with:
              # relative path to the place where source code with Dockerfile is located
              context: ./HTTP server
              # Note: tags has to be all lower-case
              tags: ${{secrets.DOCKERHUB_USERNAME}}/http:1.0
              push: ${{ github.ref == 'refs/heads/master' }}


### Going further
```
name: CI devops 2022 CPE
on:
  #to begin you want to launch this job in main and develop
  push:
    branches: ['master', 'develop']
  pull_request:
jobs:
  test-backend:
    runs-on: ubuntu-18.04
    env:
      working-directory: ./API/simple-api
    steps:
      #checkout your github code using actions/checkout@v2.3.3
      - uses: actions/checkout@v2.3.3
      #do the same with another action (actions/setup-java@v2) that enable to setup jdk 11
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          # On utilise la version 11 avec la distribution adopt
          java-version: '11'
          distribution: 'adopt'
          cache: 'maven'
      - name: Cache local Maven repository
        uses: actions/cache@v2
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-
      - name: Analyze with sonar
        env:
          GITHUB_TOKEN: ${{secrets.GITHUB_TOKEN}}
          SONAR_TOKEN: ${{secrets.SONAR_TOKEN}}
        run: mvn -B verify sonar:sonar -Dsonar.projectKey=1c6a274c-6721-477b-b100-4dd4cad0218c -Dsonar.organization=962bde7e-6cb4-4bbf-8a53-f5b1616deaf1 -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${{secrets.SONAR_TOKEN }} --file ./API/simple-api
  
  call-workflow:
    needs: test-backend
    uses: Zennyth/4irc-CICD/.github/workflows/main.yaml@master 
    secrets:
      DOCKERHUB_USERNAME: ${{secrets.DOCKERHUB_USERNAME}}
      DOCKERHUB_PASSWORD: ${{secrets.DOCKERHUB_PASSWORD}}
```


CI/CD TP3 - Docker
==================

Intro
-----
```
    all:
      vars:
        # utilisateur courrant
        ansible_user: centos
        # ssh path
        ansible_ssh_private_key_file: ./id_rsa
      children:
        prod:
          # Serveur
          hosts: centos@mathis.figuet.takima.cloud

    # changer de distribution
    ansible all -i inventories/setup.yml -m setup -a "filter=ansible_distribution*"
    
    # remove apache
    ansible all -i inventories/setup.yml -m yum -a "name=httpd state=absent" --become
```

Playbooks
---------
```
    - hosts: all
      gather_facts: false
      become: yes
      # Run all roles
      roles:
        - docker
        - create-network
        - launch-database
        - launch-app
        - launch-front
        - launch-proxy

    # Exemple de role
    - name: Create App
      docker_container:
        name: tp1-api
        image: zennyth/simple-api:latest
        networks:
          - name: network
```