#/bin/sh
cd $(dirname "${BASH_SOURCE[0]}")
cd ..
cd ./iPerf
bash configure
make
cd ..
cp ./iPerf/src/iperf iperf.elf
