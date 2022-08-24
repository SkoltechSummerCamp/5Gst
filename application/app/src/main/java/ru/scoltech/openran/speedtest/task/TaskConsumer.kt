package ru.scoltech.openran.speedtest.task

interface TaskConsumer<T> {
    fun <R> andThen(task: Task<T, R>): TaskConsumer<R>

    fun <R> andThenUnstoppable(task: (T) -> R): TaskConsumer<R>

    fun <R> andThenTry(
        buildBlock: ChainInitializer<T>.() -> TaskConsumer<R>,
    ): FinallyTaskConsumer<T, R>

    fun <A, R> andThenTry(
        startTask: Task<T, A>,
        buildBlock: ChainInitializer<A>.() -> TaskConsumer<R>
    ): FinallyTaskConsumer<A, R>

    interface ChainInitializer<T> {
        fun initializeNewChain(): TaskConsumer<T>
    }

    interface FinallyTaskConsumer<T, R> {
        fun andThenFinally(task: Task<T, out Any?>): TaskConsumer<R>
    }
}
