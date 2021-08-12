#include "jni.h"
#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <sys/stat.h>
#include <sys/wait.h>
#include <cstdlib>

int main(int argc, char **argv);

static pid_t iperfPid;

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_mkfifo(JNIEnv* env, jobject, jstring jPipePath)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    mkfifo(pipePath, 0777);
    env->ReleaseStringUTFChars(jPipePath, pipePath);
}

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_exitJni(JNIEnv* env, jobject)
{
    kill(iperfPid, SIGINT);
    waitpid(iperfPid, nullptr, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_sendForceExitJni(JNIEnv* env, jobject)
{
    kill(iperfPid, SIGINT);
}

int redirectFileToPipe(JNIEnv* env, jstring jPipePath, FILE* file)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    const int pipeFd = open(pipePath, O_WRONLY);
    env->ReleaseStringUTFChars(jPipePath, pipePath);

    dup2(pipeFd, fileno(file));
    setbuf(file, nullptr);
    fflush(file);
    return pipeFd;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_startJni(JNIEnv* env, jobject, jstring jStdoutPipePath, jstring jStderrPipePath, jobjectArray args)
{
    int stdoutPipeFd;
    int stderrPipeFd;
    int argc;
    char** argv;

    iperfPid = fork();
    if (iperfPid == -1) {
        return -1;
    } else if (iperfPid == 0) {
        stdoutPipeFd = redirectFileToPipe(env, jStdoutPipePath, stdout);
        stderrPipeFd = redirectFileToPipe(env, jStderrPipePath, stderr);

        argc = env->GetArrayLength(args) + 1;
        argv = new char *[argc];
        argv[0] = "iperf";
        for (int i = 0; i < argc - 1; i++) {
            auto jArg = (jstring) (env->GetObjectArrayElement(args, i));
            argv[i + 1] = (char*) env->GetStringUTFChars(jArg, nullptr);
        }

        main(argc, argv);

        for (int i = 0; i < argc - 1; i++) {
            auto jArg = (jstring) (env->GetObjectArrayElement(args, i));
            env->ReleaseStringUTFChars(jArg, argv[i + 1]);
        }

        close(stderrPipeFd);
        close(stdoutPipeFd);
        exit(0);
    }
    return 0;
}
