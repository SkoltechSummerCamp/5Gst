#/bin/sh
cd  "$(realpath "${0}" | xargs dirname)"
cd ../iPerf
bash configure
make
cd ..
cp ./iPerf/src/iperf iperf.elf
