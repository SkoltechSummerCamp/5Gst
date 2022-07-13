# SpeedtestService

[![Build Status](https://github.com/SkoltechSummerCamp/SpeedtestService/workflows/Build%20docker%20image/badge.svg)](https://github.com/SkoltechSummerCamp/SpeedtestService/actions)

### Building Iperf

Just start iPerf_build.sh

```bash
./scripts/build-iperf.sh
```

### Cloning repo

```bash
git clone --recursive --recurse-submodules $repo_link
```


## Usage

**iPerf binary is placed in the same directory as `iperf_wrapper.py` script.**



To start the server you need to run the command:

1. Build Iperf
2. Setup environment variables

```
ALLOWED_HOSTS=127.0.0.1; # Hosts on which you want to start service
BALANCER_ADDRESS=127.0.0.1:5555;
BALANCER_BASE_URL=127.0.0.1:5555;
CONNECTING_TIMEOUT=30;
DEBUG=True;
DJANGO_SETTINGS_MODULE=service.settings;
IPERF_PORT=5005;
SECRET_KEY=123;
SERVICE_IP_ADDRESS=127.0.0.1;
SERVICE_PORT=5004
```
3. Change host in swagger_client/configuration.py to balancer's hostname
4. Install everything from Pipfile
5. Run python

```
python3 manage.py runserver HERE_IS_YOUR_HOSTNAME 
```

The server listens port `5000` and can handle the following GET requests:

* start-iperf
* stop-iperf

> Set environment variables for IPERF_PORT and SERVICE_PORT, to allow multiple service on one server 

### start-iperf GET request
To start the iPerf with parameters, specified in `args`.

```
http://localhost:5000/start-iperf?args=-s%20-t%2010
```

If request has no `args`, iPerf will start with `-s -u` parameters.

If iPerf is already running, it will restart with new `args`. 

### stop-iperf GET request
Stop the iPerf process.

```
http://localhost:5000/stop-iperf
```