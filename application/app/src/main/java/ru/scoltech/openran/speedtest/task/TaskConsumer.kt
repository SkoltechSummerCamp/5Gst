package ru.scoltech.openran.speedtest.task

interface TaskConsumer<T> {
    fun <R> andThen(task: Task<T, R>): TaskConsumer<R>
    fun <R> andThenUnstoppable(task: (T) -> R): TaskConsumer<R>
    fun <A, R> andThenTry(
        startTask: Task<T, A>,
        buildBlock: TaskConsumer<A>.() -> TaskConsumer<R>
    ): FinallyTaskConsumer<A, R>

    interface FinallyTaskConsumer<T, R> {
        fun andThenFinally(task: (T) -> Unit): TaskConsumer<R>
    }
}
