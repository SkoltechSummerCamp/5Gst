FROM ubuntu:20.04

WORKDIR /SpeedtestService
EXPOSE 5000
EXPOSE 5001
EXPOSE 5001/udp

COPY . .
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    python3 -m pip install --upgrade pip  && \
    pip3 install pipenv && \
    pipenv install && \
    chmod +x iperf
CMD ["pipenv", "run", "gunicorn", "balancer.wsgi", "-b", ":5000"]
