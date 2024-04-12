FROM tomcat:9.0.74

RUN mkdir /usr/local/tomcat/conf/security
COPY config/server.xml /usr/local/tomcat/conf
COPY security/demo.* /usr/local/tomcat/conf/security