package maestro.cli.runner.resultview

import maestro.cli.runner.CommandState
import maestro.cli.runner.CommandStatus

class PlainTextResultView: ResultView {

    private var currentStep = 0
    private var renderStepCount = 0

    override fun setState(state: UiState) {
        when (state) {
            is UiState.Running -> renderRunningState(state)
            is UiState.Error -> renderErrorState(state)
        }
    }

    private fun renderErrorState(state: UiState.Error) {
        println(state.message)
    }

    private fun renderRunningState(state: UiState.Running) {
        renderStepCount = 0
        renderRunningStatePlainText(state)
    }

    private fun shouldPrintStep(): Boolean {
        if (currentStep == renderStepCount) {
            renderStepCount++
            currentStep++
            return true
        }
        renderStepCount++
        return false
    }

    private fun registerStep(count: Int = 1) {
        renderStepCount += count
    }

    private fun renderRunningStatePlainText(state: UiState.Running) {
        if (shouldPrintStep()) {
            state.device?.let {
                println("Running on ${state.device.description}")
            }
        }

        if (state.initCommands.isNotEmpty()) {
            if (shouldPrintStep()) {
                println("  > Init Flow")
            }


            renderCommandsPlainText(state.initCommands)
        }

        if (shouldPrintStep()) {
            println(" > Flow")
        }

        renderCommandsPlainText(state.commands)
    }

    private fun renderCommandsPlainText(commands: List<CommandState>, indent: Int = 0) {
        for (command in commands) {
            renderCommandPlainText(command, indent)
        }
    }

    private fun renderCommandPlainText(command: CommandState, indent: Int) {
        val c = command.command.asCommand()
        if (c?.visible() == false) { return }

        if (command.subCommands != null) {

            if (shouldPrintStep()) {
                println("  ".repeat(indent) + "${c?.description()}...")
            }

            renderCommandsPlainText(command.subCommands, indent = indent + 1)

            if (shouldPrintStep()) {
                println("  ".repeat(indent) + "${c?.description()}... " + status(command.status))
            }
        } else {
            when (command.status) {
                CommandStatus.PENDING -> {
                    registerStep(2)
                }

                CommandStatus.RUNNING -> {
                    if (shouldPrintStep()) {
                        print("  ".repeat(indent) + "${c?.description()}...")
                    }
                    registerStep()
                }

                CommandStatus.COMPLETED, CommandStatus.FAILED, CommandStatus.SKIPPED -> {
                    registerStep()
                    if (shouldPrintStep()) {
                        println(" " + status(command.status))
                    }
                }
            }
        }
    }

    private fun status(status: CommandStatus): String {
        return when (status) {
            CommandStatus.COMPLETED -> "COMPLETED"
            CommandStatus.FAILED -> "FAILED"
            CommandStatus.RUNNING -> "RUNNING"
            CommandStatus.PENDING -> "PENDING"
            CommandStatus.SKIPPED -> "SKIPPED"
        }
    }
}
