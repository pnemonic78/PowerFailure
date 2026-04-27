package net.sf.power.monitor.model

sealed class Command {
    object StartMonitor : Command()
    object StopMonitor : Command()
    object Settings : Command()
    object Test : Command()
}