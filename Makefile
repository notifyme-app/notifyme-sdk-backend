######################
#      Makefile      #
######################

N2S_SDK = n2step-sdk-backend
N2S_SDK_WS = $(N2S_SDK)/n2step-sdk-backend-ws

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
	mvn -f $(N2S_SDK)/pom.xml test

package:
	mvn -f $(N2S_SDK)/pom.xml clean package

updateproject:
	mvn -f $(N2S_SDK)/pom.xml package -DskipTests

updatedoc:
	mvn -f $(N2S_SDK)/pom.xml package -Dmaven.test.skip=true
	mvn springboot-swagger-3:springboot-swagger-3 -f $(N2S_SDK_WS)/pom.xml
	cp $(N2S_SDK_WS)/generated/swagger/swagger.yaml documentation/yaml/sdk.yaml

swagger:
	cd documentation; $(RUSTY_SWAGGER) --file ../$(N2S_SDK_WS)/generated/swagger/swagger.yaml

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
	cp $(N2S_SDK_WS)/target/n2step-sdk-backend-ws.jar ws-sdk/ws/bin/n2step-sdk-backend-ws-1.0.0.jar
	docker build -t N2S-docker ws-sdk/
	@printf '\033[33m DO NOT USE THIS IN PRODUCTION \033[0m \n'
	@printf "\033[32m docker run -p 8080:8080 -v $(PWD)/$(N2S_SDK_WS)/src/main/resources/logback.xml:/home/ws/conf/n2step-sdk-backend-ws-logback.xml -v $(PWD)/dpppt-backend-sdk/n2step-sdk-backend-ws/src/main/resources/application.properties:/home/ws/conf/n2step-sdk-backend-ws.properties N2S-docker \033[0m\n"

clean:
	mvn -f $(N2S_SDK)/pom.xml clean
	@rm -f $(N2S_SDK_WS)/N2S-ws.log*
	@rm -f documentation/*.log documentation/*.aux documentation/*.dvi documentation/*.ps documentation/*.blg documentation/*.bbl documentation/*.out documentation/*.bcf documentation/*.run.xml documentation/*.fdb_latexmk documentation/*.fls documentation/*.toc
