#/bin/sh
cd iPerf
head -1 src/Launch.cpp | grep vinogradov.alek@gmail.com
#If the patch has not been applied then the $? which is the exit status 
#for last command would have a success status code = 0
if [ $? -eq 1 ];
then
    #apply the patch
    echo "patching iPerf"
    patch src/Launch.cpp < ../scripts/iPerf_patch.diff
else
    echo "already pathed"
fi
bash configure
make
cd ..
cp iPerf/src/iperf iperf.elf

