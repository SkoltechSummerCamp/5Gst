#include "jni.h"
#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <sys/stat.h>
#include <sys/wait.h>
#include <cstdlib>
#include <cerrno>
#include <cstring>

int main(int argc, char **argv);

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_mkfifo(JNIEnv* env, jobject, jstring jPipePath)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    int code = mkfifo(pipePath, 0777);
    env->ReleaseStringUTFChars(jPipePath, pipePath);
    return code == -1 ? errno : 0;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_waitForProcessNoDestroy(__unused JNIEnv* env, jobject, jlong pid)
{
    return waitid(P_PID, static_cast<id_t>(pid), nullptr, WEXITED | WNOWAIT) == -1 ? errno : 0; // NOLINT(hicpp-signed-bitwise)
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_waitForProcess(__unused JNIEnv* env, jobject, jlong pid)
{
    return waitpid(static_cast<pid_t>(pid), nullptr, 0) == -1 ? errno : 0;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_sendSigInt(__unused JNIEnv* env, jobject, jlong pid)
{
    return kill(static_cast<pid_t>(pid), SIGINT) == -1 ? errno : 0;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_sendSigKill(__unused JNIEnv* env, jobject, jlong pid)
{
    return kill(static_cast<pid_t>(pid), SIGKILL) == -1 ? errno : 0;
}

int redirectFileToPipe(JNIEnv* env, jstring jPipePath, FILE* file)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    const int pipeFd = open(pipePath, O_WRONLY);
    env->ReleaseStringUTFChars(jPipePath, pipePath);

    if (pipeFd == -1 || dup2(pipeFd, fileno(file)) == -1 || fflush(file) == EOF) {
        fprintf(stderr, "Could not open named pipe to redirect stream: %s", strerror(errno));
        exit(EXIT_FAILURE);
    }
    setbuf(file, nullptr);
    return pipeFd;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_iperf_IperfRunner_start(
        JNIEnv* env,
        jobject,
        jstring jStdoutPipePath,
        jstring jStderrPipePath,
        jobjectArray args,
        jlongArray pidHolder
)
{
    pid_t pid = fork();
    if (pid == -1) {
        return errno;
    } else if (pid == 0) {
        int stderrPipeFd = redirectFileToPipe(env, jStderrPipePath, stderr);
        int stdoutPipeFd = redirectFileToPipe(env, jStdoutPipePath, stdout);

        int argc = env->GetArrayLength(args) + 1;
        char** argv = new char *[argc];
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

        close(stdoutPipeFd);
        close(stderrPipeFd);
        exit(EXIT_SUCCESS);
    }

    auto* buffer = new jlong[1];
    buffer[0] = static_cast<jlong>(pid);
    env->SetLongArrayRegion(pidHolder, 0, 1, buffer);
    delete[] buffer;
    return 0;
}
