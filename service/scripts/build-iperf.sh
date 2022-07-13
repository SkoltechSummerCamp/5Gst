#/bin/sh
cd ../iPerf
bash configure
make
cd ../service
cp ../iPerf/src/iperf iperf.elf
