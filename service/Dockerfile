FROM ubuntu:20.04

WORKDIR /service
EXPOSE 5000
EXPOSE 5001
EXPOSE 5001/udp

COPY . .
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    python3 -m pip install --upgrade pip  && \
    pip3 install pipenv && \
    pipenv install && \
    chmod +x ./scripts/build-iperf.sh && \
    ./scripts/build-iperf.sh && \
    chmod +x ./iperf.elf

CMD ["pipenv", "run", "python3", "manage.py", "runserver", "0.0.0.0:5000"]
