package com.jovakinn

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlin.system.measureTimeMillis

fun log (msg: String) = println("[${Thread.currentThread().name}] $msg")

suspend fun foo(): List<Int> {
    delay(1000)
    return listOf(1, 2, 3, 4, 5)
}

suspend fun performRequest(request: Int): String{
    delay(1000)
    return "response $request"
}

suspend fun sendString(channel: SendChannel<String>, s: String, time: Long){
    while (true) {
        delay(time)
        channel.send(s)
    }
}

fun main() = runBlocking<Unit>  {

    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "babax", 500L) }
    repeat(6){
        println(channel.receive())
    }
    coroutineContext.cancelChildren()


    val channel1 = Channel<Int>()
    launch {
        for (x in 1..5) channel1.send(x * x)
    }
    repeat(5){ println(channel1.receive())}
    println("Done!")

    foo().forEach {value -> println(value)}

    // parental responsibilities
    val request1 = launch {
        repeat(5) {i -> // launch children jobs
            launch {
                delay((i + 1) * 200L)
                log("Coroutine is done")
            }
        }
        log("request: I am done and I don't explicitly join" +
                " my children that are still active")
    }
    request1.join()
    log("Everything is complete.")

    val a = async {
        log("I am computing a piece of the answer")
        6
    }

    val b = async {
        log("I am computing another one")
        7
    }
    log("The answer is ${a.await() * b.await()}")



    val request = launch {
        GlobalScope.launch {
            log("job1: I run in GlobalScope and execute independently!")
            delay(1000)
            log("job1: I am not affected by cancellation of the request.")
        }
        launch {
            delay(100)
            log("job2: I am child of the request coroutine")
            delay(1800)
            log("I will not execute this line if my parent is cancelled")
        }
    }
    delay(500)
    request.cancel()
    delay(1000)
    log("main: Who has survived request cancellation?")

    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) {
            if (System.currentTimeMillis() >= nextPrintTime ) {
                println("job: I'm sleeping ${i++}")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L)
    println("main: I'm tired of waiting!")
    job.cancelAndJoin()
    println("Now I can quit")

    val time = measureTimeMillis {
        val one = sthUseful1Async()
        val two = sthUseful2Async()
        runBlocking { println("Answer is ${one.await() + two.await()}") }
    }
    println("Completed in $time ms")
}

suspend fun doSmtUseful1(): Int {
    delay(1000L)
    return 2
}
suspend fun doSmtUseful2 (): Int {
    delay(1000L)
    return 2
}

fun sthUseful1Async() = GlobalScope.async {
    doSmtUseful1()
}

fun sthUseful2Async() = GlobalScope.async {
    doSmtUseful2()
}

suspend fun doWorld(){
    delay(1000L)
    println("World!")
}
