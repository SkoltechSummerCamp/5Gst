package ru.scoltech.openran.speedtest.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

class TaskChainBuilder<S : Any?> : TaskConsumer.ChainInitializer<S> {
    private var onStop: () -> Unit = {}
    private var onFatalError: (String, Exception?) -> Unit = { _, _ -> }

    override fun initializeNewChain(): TaskConsumer<S> {
        return TasksCollector(mutableListOf())
    }

    @Suppress("UNCHECKED_CAST")
    fun <R : Any?> finishChainCreation(
        taskConsumer: TaskConsumer<R>,
        onSuccess: (R) -> Unit,
    ): TaskChain<S> {
        return TaskChain(
            taskConsumer.asTasksCollector().tasks as MutableList<Task<Any?, Any?>>,
            onSuccess as (Any?) -> Unit,
            onStop,
            onFatalError,
        )
    }

    fun onStop(onStop: () -> Unit): TaskChainBuilder<S> {
        this.onStop = onStop
        return this
    }

    fun onFatalError(onFatalError: (String, Exception?) -> Unit): TaskChainBuilder<S> {
        this.onFatalError = onFatalError
        return this
    }

    private fun <T : Any?> TaskConsumer<T>.asTasksCollector(): TasksCollector<T> {
        @Suppress("UNCHECKED_CAST")
        return this as? TasksCollector<T>
            ?: throw IllegalArgumentException(
                "Unexpected task consumer ${this::class}, expected ${TasksCollector::class}"
            )
    }

    private inner class TasksCollector<T : Any?>(
        val tasks: MutableList<Task<*, *>>
    ) : TaskConsumer<T> {
        override fun <R : Any?> andThen(task: Task<T, R>): TaskConsumer<R> {
            tasks.add(task)
            return TasksCollector(tasks)
        }

        override fun <R : Any?> andThenUnstoppable(task: (T) -> R): TaskConsumer<R> {
            return andThen(UnstoppableTask(task))
        }

        override fun <A : Any?, R : Any?> andThenTry(
            startTask: Task<T, A>,
            buildBlock: TaskConsumer.ChainInitializer<A>.() -> TaskConsumer<R>,
        ): TaskConsumer.FinallyTaskConsumer<A, R> {
            tasks.add(startTask)
            return FinallyTaskConsumerImpl(tasks, buildBlock)
        }

        override fun <R : Any?> andThenTry(
            buildBlock: TaskConsumer.ChainInitializer<T>.() -> TaskConsumer<R>,
        ): TaskConsumer.FinallyTaskConsumer<T, R> {
            return FinallyTaskConsumerImpl(tasks, buildBlock)
        }
    }

    private class UnstoppableTask<T : Any?, R : Any?>(private val task: (T) -> R) : Task<T, R> {
        override fun prepare(
            argument: T,
            killer: TaskKiller,
        ): Promise<(R) -> Unit, (String, Exception?) -> Unit> {
            return Promise { onSuccess, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    onSuccess?.invoke(task(argument))
                }
            }
        }
    }

    private inner class FinallyTaskConsumerImpl<T : Any?, R : Any?>(
        private val tasks: MutableList<Task<out Any?, out Any?>>,
        private val buildBlock: TaskConsumer.ChainInitializer<T>.() -> TaskConsumer<R>,
    ) : TaskConsumer.FinallyTaskConsumer<T, R> {
        override fun andThenFinally(task: Task<T, out Any?>): TaskConsumer<R> {
            @Suppress("UNCHECKED_CAST")
            tasks.add(TryTask(buildBlock, task as Task<T, Any?>))
            return TasksCollector(tasks)
        }
    }

    private inner class TryTask<T : Any?, R : Any?>(
        buildBlock: TaskConsumer.ChainInitializer<T>.() -> TaskConsumer<R>,
        private val finallyTask: Task<T, Any?>,
    ) : Task<T, R> {
        private val tasks: List<Task<Any?, Any?>>

        init {
            @Suppress("UNCHECKED_CAST")
            tasks = buildBlock(TaskChainBuilder()).asTasksCollector().tasks
                    as MutableList<Task<Any?, Any?>>
        }

        private fun buildTryChainOnSuccess(
            argument: T,
            onSuccess: ((R) -> Unit)?,
            onError: ((String, Exception?) -> Unit)?,
        ): (Any?) -> Unit = { chainResult ->
            finallyTask.prepare(argument, TaskKiller())
                .onSuccess {
                    @Suppress("UNCHECKED_CAST")
                    onSuccess?.invoke(chainResult as R)
                }
                .onError { message, exception ->
                    onError?.invoke(message, exception)
                }
                .start()
        }

        private fun buildTryChainOnStop(
            argument: T,
            onError: ((String, Exception?) -> Unit)?,
        ): () -> Unit = {
            finallyTask.prepare(argument, TaskKiller())
                .onSuccess {
                    onError?.invoke("Stopped", null)
                }
                .onError { message, exception ->
                    onError?.invoke("Caught {$message} during stop", exception)
                }
                .start()
        }

        private fun buildTryChainOnError(
            argument: T,
            onError: ((String, Exception?) -> Unit)?,
        ): (String, Exception?) -> Unit = { rootMessage, rootException ->
            finallyTask.prepare(argument, TaskKiller())
                .onSuccess {
                    onError?.invoke(rootMessage, rootException)
                }
                .onError { message, exception ->
                    val resultException = when {
                        rootException == null -> exception
                        exception == null -> rootException
                        else -> rootException.apply { addSuppressed(exception) }
                    }

                    onError?.invoke(
                        "During catching error {$rootMessage} caught {$message}",
                        resultException,
                    )
                }
                .start()
        }

        override fun prepare(
            argument: T,
            killer: TaskKiller,
        ): Promise<(R) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
            val tryChain = TaskChain<T>(
                tasks,
                buildTryChainOnSuccess(argument, onSuccess, onError),
                buildTryChainOnStop(argument, onError),
                buildTryChainOnError(argument, onError),
            )

            killer.register {
                tryChain.stop()
            }
            tryChain.start(argument)
        }
    }
}
