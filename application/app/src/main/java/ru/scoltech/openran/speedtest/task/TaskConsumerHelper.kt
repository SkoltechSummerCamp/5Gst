package ru.scoltech.openran.speedtest.task

import java.util.concurrent.atomic.AtomicReference

interface ExtractedArgumentHandler<T, A> {
    fun doTask(task: Task<A, *>): TaskConsumer<T>
}

private class ExtractedArgumentHandlerImpl<T, A>(
    private val taskConsumer: TaskConsumer<T>,
    private val extractArgument: (T) -> A,
) : ExtractedArgumentHandler<T, A> {
    override fun doTask(task: Task<A, *>): TaskConsumer<T> {
        val argumentHolder = AtomicReference<T>()

        return taskConsumer
            .andThenUnstoppable {
                argumentHolder.set(it)
                extractArgument(it)
            }
            .andThen(task)
            .andThenUnstoppable { argumentHolder.get() }
    }
}

fun <T, A> TaskConsumer<T>.withArgumentExtracted(
    extractArgument: (T) -> A,
): ExtractedArgumentHandler<T, A> {
    return ExtractedArgumentHandlerImpl(this, extractArgument)
}

fun <T, R> TaskConsumer<T>.withArgumentKeptDoUnstoppableTask(task: (T) -> R): TaskConsumer<T> {
    return andThenUnstoppable {
        task(it)
        it
    }
}
