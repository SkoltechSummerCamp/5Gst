#/bin/bash
cd "$(dirname -- "$(readlink -f "${BASH_SOURCE}")")"
cd ./iPerf
bash configure
make
cd ..
cp ./iPerf/src/iperf iperf.elf
