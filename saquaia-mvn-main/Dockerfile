FROM ubuntu:latest

WORKDIR /saquaia

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        maven \
        openjdk-11-jdk \
        zip \
    && rm -rf /var/lib/apt/lists/*
	
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 \
        python3-pip

RUN pip install -U --no-cache-dir \
    matplotlib==3.5.1 \
    numpy==1.22.3
	
COPY . .

WORKDIR /saquaia/code

RUN mvn package

CMD ["java", "-Xmx16g", "-jar", "target/saquaia-jar-with-dependencies.jar", "-e", "-f", "benchmarks_PPonly.json"]