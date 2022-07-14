#/bin/sh
cd $(dirname "${BASH_SOURCE[0]}")
cd ../../iPerf
bash configure
make
cd ../service
cp ../iPerf/src/iperf iperf.elf
