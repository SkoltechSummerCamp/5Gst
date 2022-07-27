#/bin/bash
cd "$(dirname -- "$(readlink -f "${BASH_SOURCE}")")"
case $PWD in
  */scripts) cd ..;;
esac
cd ./iPerf
bash configure
make
cd ..
cp ./iPerf/src/iperf iperf.elf
