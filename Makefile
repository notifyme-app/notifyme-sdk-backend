######################
#      Makefile      #
######################

PROJ_BASE_DIR = swisscovid-cn-sdk-backend
WS_MODULE_NAME = swisscovid-cn-sdk-backend-ws
WS_MODULE_DIR = $(PROJ_BASE_DIR)/$(WS_MODULE_NAME)
WS_VERSION = 2.0.0
DOCKER_IMG_NAME = swisscovid-cn-docker

FILE_NAME = documentation.tex

LATEX = xelatex
BIBER = biber
RUSTY_SWAGGER = rusty-swagger

all: clean all1
all1: clean updateproject updatedoc swagger la la2 la3
no: clean updateproject updatedoc swagger la la2
docker-build: updateproject docker
doc: updatedoc swagger la la2 la3
test: clean run-test
run-test:
	mvn -f $(PROJ_BASE_DIR)/pom.xml test

package:
	mvn -f $(PROJ_BASE_DIR)/pom.xml clean package

updateproject:
	mvn -f $(PROJ_BASE_DIR)/pom.xml package -DskipTests

# Swagger doesn't do well with protobufs - Generated documentation won't be very useful
updatedoc:
	mvn -f $(PROJ_BASE_DIR)/pom.xml package -Dmaven.test.skip=true
	mvn springboot-swagger-3:springboot-swagger-3 -f $(WS_MODULE_DIR)/pom.xml
	cp $(WS_MODULE_DIR)/swagger/swagger.yaml documentation/yaml/sdk.yaml

swagger:
	cd documentation; $(RUSTY_SWAGGER) --file ./yaml/sdk.yaml

la:
	cd documentation;$(LATEX) $(FILE_NAME)
bib:
	cd documentation;$(BIBER) $(FILE_NAME)
la2:
	cd documentation;$(LATEX) $(FILE_NAME)
la3:
	cd documentation;$(LATEX) $(FILE_NAME)
show:
	cd documentation; open $(FILE_NAME).pdf &

docker:
	cp $(WS_MODULE_DIR)/target/${WS_MODULE_NAME}*.jar swisscovid-cn-ws/ws/bin/${WS_MODULE_NAME}-${WS_VERSION}.jar
	docker build -t ${DOCKER_IMG_NAME} notifyme-ws/
	@printf '\033[33m DO NOT USE THIS IN PRODUCTION \033[0m \n'
	@printf "\033[32m docker run -p 8080:8080 -v $(PWD)/$(WS_MODULE_DIR)/src/main/resources/logback.xml:/home/ws/conf/${WS_MODULE_NAME}-logback.xml -v $(PWD)/$(WS_MODULE_DIR)/src/main/resources/application.properties:/home/ws/conf/${WS_MODULE_NAME}.properties ${DOCKER_IMG_NAME} \033[0m\n"

clean:
	mvn -f $(PROJ_BASE_DIR)/pom.xml clean
	@rm -f $(WS_MODULE_DIR)/swisscovid-cn-ws.log*
	@rm -f documentation/*.log documentation/*.aux documentation/*.dvi documentation/*.ps documentation/*.blg documentation/*.bbl documentation/*.out documentation/*.bcf documentation/*.run.xml documentation/*.fdb_latexmk documentation/*.fls documentation/*.toc
