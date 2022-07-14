#/bin/sh
cd ./iPerf
bash configure
make
cd ..
cp ./iPerf/src/iperf iperf.elf
