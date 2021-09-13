package ru.scoltech.openran.speedtest.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

class TaskChainBuilder<S : Any?> {
    private var tasks = mutableListOf<Task<out Any?, out Any?>>()
    private var onStop: () -> Unit = {}
    private var onFatalError: (String, Exception?) -> Unit = { _, _ -> }

    fun initializeNewChain(): TaskConsumer<S> {
        tasks = mutableListOf()
        return TasksCollector(tasks)
    }

    @Suppress("UNCHECKED_CAST")
    fun finishChainCreation(): TaskChain<S> {
        return TaskChain(tasks as MutableList<Task<Any?, Any?>>, onStop, onFatalError)
    }

    fun onStop(onStop: () -> Unit): TaskChainBuilder<S> {
        this.onStop = onStop
        return this
    }

    fun onFatalError(onFatalError: (String, Exception?) -> Unit): TaskChainBuilder<S> {
        this.onFatalError = onFatalError
        return this
    }

    private class TasksCollector<T : Any?>(
        private val tasks: MutableList<Task<*, *>>
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
            buildBlock: TaskConsumer<A>.() -> TaskConsumer<R>,
        ): TaskConsumer.FinallyTaskConsumer<A, R> {
            tasks.add(startTask)
            return object : TaskConsumer.FinallyTaskConsumer<A, R> {
                override fun andThenFinally(task: (A) -> Unit): TaskConsumer<R> {
                    tasks.add(TryTask(buildBlock, task))
                    return TasksCollector(tasks)
                }
            }
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

    private class TryTask<T : Any?, R : Any?>(
        buildBlock: TaskConsumer<T>.() -> TaskConsumer<R>,
        private val finallyTask: (T) -> Unit,
    ) : Task<T, R> {
        private val tasks: List<Task<Any?, Any?>>

        init {
            val mutableTasks = mutableListOf<Task<*, *>>()
            buildBlock(TasksCollector(mutableTasks))
            @Suppress("UNCHECKED_CAST")
            tasks = mutableTasks as MutableList<Task<Any?, Any?>>
        }

        private fun ((String, Exception?) -> Unit)?.withEndTask(
            finallyArgument: T
        ): ((String, Exception?) -> Unit) {
            return { message, exception ->
                finallyTask(finallyArgument)
                this?.invoke(message, exception)
            }
        }

        override fun prepare(
            argument: T,
            killer: TaskKiller,
        ): Promise<(R) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
            @Suppress("UNCHECKED_CAST")
            val onSuccessTask = UnstoppableTask<R, Unit> {
                finallyTask(argument)
                onSuccess?.invoke(it)
            } as Task<Any?, Any?>

            val tryChain = TaskChain<T>(
                tasks + listOf(onSuccessTask),
                { onError.withEndTask(argument).invoke("Stopped", null) },
                onError.withEndTask(argument)
            )

            killer.register { tryChain.stop() }
            tryChain.start(argument)
        }
    }
}
