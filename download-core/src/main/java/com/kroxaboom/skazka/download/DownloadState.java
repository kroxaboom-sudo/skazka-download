package com.kroxaboom.skazka.download;

public enum DownloadState {
    WAITING,
    RUNNING,
    VERIFYING,
    RETRY,
    NETWORK,
    PAUSED,
    ERROR,
    DONE,
    CANCELLED,
    SKIPPED
}
