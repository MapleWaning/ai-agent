from langchain.tools import tool

_TERMINATE_MESSAGE = "任务结束"


@tool
def terminate() -> str:
    """Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
    When you have finished all the tasks, call this tool to end the work."""
    return _TERMINATE_MESSAGE
