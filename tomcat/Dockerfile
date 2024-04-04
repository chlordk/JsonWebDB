FROM tomcat:9.0.74

RUN rm -rf /usr/local/tomcat/webapps
RUN mv /usr/local/tomcat/webapps.dist /usr/local/tomcat/webapps

COPY config/conf/* /usr/local/tomcat/conf
COPY config/webapps/manager/META-INF/* /usr/local/tomcat/webapps/manager/META-INF