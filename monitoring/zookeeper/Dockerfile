FROM zookeeper:3.9

COPY monitoring/zookeeper/jmx_prometheus_javaagent-0.20.0.jar /opt/jmx_prometheus_javaagent.jar
COPY monitoring/zookeeper/zookeeper-jmx-config.yml /opt/zookeeper-jmx-config.yml

ENV JVMFLAGS="-javaagent:/opt/jmx_prometheus_javaagent.jar=9404:/opt/zookeeper-jmx-config.yml"
