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
2. Run python

```
cd swagger_client
python3 setup.py install --user
cd ..
python3 server.py 
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