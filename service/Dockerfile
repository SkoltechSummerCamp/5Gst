FROM ubuntu:20.04
RUN apt-get update
RUN apt-get install -y python3 python3-pip

WORKDIR /SpeedtestService
EXPOSE 5000
EXPOSE 5001
EXPOSE 5001/udp

COPY requirements.txt requirements.txt
RUN pip3 install -r requirements.txt
COPY iperf iperf
COPY server.py server.py
COPY iperf_wrapper.py iperf_wrapper.py
RUN chmod +x iperf
CMD python3 -u server.py
